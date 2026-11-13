package com.example.data

import android.content.Context
import com.example.R
import com.example.model.WithdrawalTransaction
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreWalletRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserProfile(): Flow<UserWalletProfile?> = flow {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserWalletProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeTransactions(): Flow<List<WalletTransactionRecord>> = flow {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("transactions")
        emitAll(
            colRef.snapshots()
                .map { querySnapshot ->
                    querySnapshot.toObjects(
                        WalletTransactionRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun getOrCreateUserProfile(
        displayName: String,
        email: String?,
        photoUrl: String?
    ): Result<UserWalletProfile> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                val existing = snap.toObject(
                    UserWalletProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existing != null) {
                    return Result.success(existing)
                }
            }
            val nowMs = System.currentTimeMillis()
            val today = GameRepository.currentEpochDay()
            val initial = UserWalletProfile(
                userId = uid,
                displayName = displayName.ifBlank { "SpinMint Player" },
                email = email,
                photoUrl = photoUrl,
                coins = 0L,
                freeSpinsLeft = 5L,
                lastSpinResetEpochDay = today,
                nextAdAvailableEpochMs = 0L,
                energy = 100L,
                lastEnergyUpdateMs = nowMs,
                streakDay = 0L,
                lastCheckinEpochDay = 0L,
                totalTaps = 0L,
                totalSpins = 0L
            )
            docRef.set(initial.toCreateMap()).await()
            Result.success(initial)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun syncWalletSnapshot(
        snapshot: PersistedGameSnapshot,
        displayName: String,
        email: String?,
        photoUrl: String?
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("users").document(uid)
        val profile = UserWalletProfile(
            userId = uid,
            displayName = displayName.ifBlank { "SpinMint Player" },
            email = email,
            photoUrl = photoUrl,
            coins = snapshot.coins,
            freeSpinsLeft = snapshot.freeSpinsLeft.toLong(),
            lastSpinResetEpochDay = snapshot.lastSpinResetEpochDay,
            nextAdAvailableEpochMs = snapshot.nextAdAvailableEpochMs,
            energy = snapshot.energy.toLong(),
            lastEnergyUpdateMs = snapshot.lastEnergyUpdateMs,
            streakDay = snapshot.streakDay.toLong(),
            lastCheckinEpochDay = snapshot.lastCheckinEpochDay,
            totalTaps = snapshot.totalTaps,
            totalSpins = snapshot.totalSpins.toLong()
        )
        return try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(profile.toUpdateMap()).await()
            } else {
                docRef.set(profile.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun recordTransaction(
        tx: WithdrawalTransaction,
        displayName: String = "SpinMint Player",
        email: String? = null,
        photoUrl: String? = null
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val userDocRef = db.collection("users").document(uid)
        val sanitizedTxId = tx.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { null }
        val txDocRef = if (sanitizedTxId != null) {
            userDocRef.collection("transactions").document(sanitizedTxId)
        } else {
            userDocRef.collection("transactions").document()
        }
        return try {
            val userSnap = userDocRef.get().await()
            if (!userSnap.exists()) {
                val initial = UserWalletProfile(
                    userId = uid,
                    displayName = displayName.ifBlank { "SpinMint Player" },
                    email = email,
                    photoUrl = photoUrl,
                    lastSpinResetEpochDay = GameRepository.currentEpochDay(),
                    lastEnergyUpdateMs = System.currentTimeMillis()
                )
                userDocRef.set(initial.toCreateMap()).await()
            }
            val payload = WalletTransactionRecord.toCreateMap(uid, tx)
            txDocRef.set(payload).await()
            Result.success(txDocRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, txDocRef.path, auth)
            Result.failure(e)
        }
    }

    // Read by explicit UID for cross-user negative testing in RuleTest
    suspend fun getProfileForUserId(targetUserId: String): Result<UserWalletProfile?> {
        val docRef = db.collection("users").document(targetUserId)
        return try {
            val snap = docRef.get().await()
            Result.success(
                snap.toObject(
                    UserWalletProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path, auth)
            Result.failure(e)
        }
    }

    // Append transaction by explicit UID for cross-user negative testing in RuleTest
    suspend fun recordTransactionForUserId(
        targetUserId: String,
        tx: WithdrawalTransaction
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: targetUserId
        val txDocRef = db.collection("users")
            .document(targetUserId)
            .collection("transactions")
            .document()
        return try {
            val payload = WalletTransactionRecord.toCreateMap(uid, tx)
            txDocRef.set(payload).await()
            Result.success(txDocRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, txDocRef.path, auth)
            Result.failure(e)
        }
    }

    // --- 1% Admin Commission & Treasury ---

    suspend fun recordAdminCommission(
        sourceGame: String,
        grossWinCoins: Long,
        commissionCoins: Long,
        netPlayerCoins: Long,
        playerName: String
    ): Result<String> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("admin_commissions").document()
        val payload = AdminCommissionRecord.toCreateMap(
            playerUid = uid,
            playerName = playerName,
            sourceGame = sourceGame,
            grossWinCoins = grossWinCoins,
            commissionCoins = commissionCoins,
            netPlayerCoins = netPlayerCoins
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    fun observeAdminCommissions(): Flow<List<AdminCommissionRecord>> = flow {
        requireUserId()
        val colRef = db.collection("admin_commissions")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AdminCommissionRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path, auth)
                    }
                    throw error
                }
        )
    }

    fun observeAdminTreasury(): Flow<Long> = flow {
        requireUserId()
        val docRef = db.collection("admin_treasury").document("summary")
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.getLong("claimedCoins") ?: 0L
                    } else {
                        0L
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun claimAdminCommissionCoins(newTotalClaimedCoins: Long, adminEmail: String): Result<Unit> {
        try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docRef = db.collection("admin_treasury").document("summary")
        val payload = AdminTreasurySummary.toWriteMap(adminEmail, newTotalClaimedCoins)
        return try {
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            Result.failure(e)
        }
    }
}

