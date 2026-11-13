package com.example.data

import android.util.Log
import com.example.model.WithdrawalTransaction
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?,
    auth: FirebaseAuth = Firebase.auth
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val currentUser = runCatching { auth.currentUser }.getOrNull()

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

@IgnoreExtraProperties
data class UserWalletProfile(
    val userId: String = "",
    val displayName: String = "SpinMint Player",
    val email: String? = null,
    val photoUrl: String? = null,
    val coins: Long = 0L,
    val freeSpinsLeft: Long = 5L,
    val lastSpinResetEpochDay: Long = 0L,
    val nextAdAvailableEpochMs: Long = 0L,
    val energy: Long = 100L,
    val lastEnergyUpdateMs: Long = 0L,
    val streakDay: Long = 0L,
    val lastCheckinEpochDay: Long = 0L,
    val totalTaps: Long = 0L,
    val totalSpins: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "displayName" to displayName.ifBlank { "SpinMint Player" }.take(80),
            "email" to email?.takeIf { it.isNotBlank() }?.take(160),
            "photoUrl" to photoUrl?.takeIf { it.isNotBlank() }?.take(500),
            "coins" to coins.coerceAtLeast(0L),
            "freeSpinsLeft" to freeSpinsLeft.coerceIn(0L, 500L),
            "lastSpinResetEpochDay" to lastSpinResetEpochDay.coerceAtLeast(0L),
            "nextAdAvailableEpochMs" to nextAdAvailableEpochMs.coerceAtLeast(0L),
            "energy" to energy.coerceIn(0L, 100L),
            "lastEnergyUpdateMs" to lastEnergyUpdateMs.coerceAtLeast(0L),
            "streakDay" to streakDay.coerceIn(0L, 7L),
            "lastCheckinEpochDay" to lastCheckinEpochDay.coerceAtLeast(0L),
            "totalTaps" to totalTaps.coerceAtLeast(0L),
            "totalSpins" to totalSpins.coerceAtLeast(0L),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }

    fun toUpdateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "displayName" to displayName.ifBlank { "SpinMint Player" }.take(80),
            "email" to email?.takeIf { it.isNotBlank() }?.take(160),
            "photoUrl" to photoUrl?.takeIf { it.isNotBlank() }?.take(500),
            "coins" to coins.coerceAtLeast(0L),
            "freeSpinsLeft" to freeSpinsLeft.coerceIn(0L, 500L),
            "lastSpinResetEpochDay" to lastSpinResetEpochDay.coerceAtLeast(0L),
            "nextAdAvailableEpochMs" to nextAdAvailableEpochMs.coerceAtLeast(0L),
            "energy" to energy.coerceIn(0L, 100L),
            "lastEnergyUpdateMs" to lastEnergyUpdateMs.coerceAtLeast(0L),
            "streakDay" to streakDay.coerceIn(0L, 7L),
            "lastCheckinEpochDay" to lastCheckinEpochDay.coerceAtLeast(0L),
            "totalTaps" to totalTaps.coerceAtLeast(0L),
            "totalSpins" to totalSpins.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@IgnoreExtraProperties
data class WalletTransactionRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val referenceCode: String = "",
    val methodName: String = "",
    val accountIdentifier: String = "",
    val coinsSpent: Long = 0L,
    val inrEquivalent: Double = 0.0,
    val usdEquivalent: Double = 0.0,
    val status: String = "Submitted",
    val txType: String = "WITHDRAWAL",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toUiTransaction(): WithdrawalTransaction {
        val tsMillis = createdAt?.toDate()?.time ?: System.currentTimeMillis()
        return WithdrawalTransaction(
            id = id.ifBlank { referenceCode },
            referenceCode = referenceCode,
            methodName = methodName,
            accountIdentifier = accountIdentifier,
            coinsSpent = coinsSpent,
            inrEquivalent = inrEquivalent,
            usdEquivalent = usdEquivalent,
            timestampMillis = tsMillis,
            status = status,
            txType = txType
        )
    }

    companion object {
        fun toCreateMap(userId: String, tx: WithdrawalTransaction): Map<String, Any> {
            return mapOf(
                "userId" to userId,
                "referenceCode" to tx.referenceCode.ifBlank { "SM-000000" }.take(32),
                "methodName" to tx.methodName.ifBlank { "UPI" }.take(32),
                "accountIdentifier" to tx.accountIdentifier.ifBlank { "Wallet" }.take(160),
                "coinsSpent" to tx.coinsSpent.coerceAtLeast(1L),
                "inrEquivalent" to tx.inrEquivalent.coerceAtLeast(0.0),
                "usdEquivalent" to tx.usdEquivalent.coerceAtLeast(0.0),
                "status" to if (tx.status == "Credited") "Credited" else "Submitted",
                "txType" to if (tx.txType == "DEPOSIT") "DEPOSIT" else "WITHDRAWAL",
                "createdAt" to FieldValue.serverTimestamp()
            )
        }
    }
}

const val SPINMINT_ADMIN_EMAIL = "smahakaal798@gmail.com"

@IgnoreExtraProperties
data class AdminCommissionRecord(
    @DocumentId val id: String = "",
    val playerUid: String = "",
    val playerName: String = "Player",
    val sourceGame: String = "SpinMint",
    val grossWinCoins: Long = 0L,
    val commissionCoins: Long = 0L,
    val netPlayerCoins: Long = 0L,
    val inrValue: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    companion object {
        fun toCreateMap(
            playerUid: String,
            playerName: String,
            sourceGame: String,
            grossWinCoins: Long,
            commissionCoins: Long,
            netPlayerCoins: Long
        ): Map<String, Any> {
            return mapOf(
                "playerUid" to playerUid,
                "playerName" to playerName.ifBlank { "SpinMint Player" }.take(80),
                "sourceGame" to sourceGame.ifBlank { "SpinMint" }.take(40),
                "grossWinCoins" to grossWinCoins.coerceIn(1L, 10_000_000L),
                "commissionCoins" to commissionCoins.coerceIn(1L, grossWinCoins.coerceAtLeast(1L)),
                "netPlayerCoins" to netPlayerCoins.coerceAtLeast(0L),
                "inrValue" to (commissionCoins * 0.01),
                "createdAt" to FieldValue.serverTimestamp()
            )
        }
    }
}

@IgnoreExtraProperties
data class AdminTreasurySummary(
    val adminEmail: String = SPINMINT_ADMIN_EMAIL,
    val claimedCoins: Long = 0L,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    companion object {
        fun toWriteMap(adminEmail: String, claimedCoins: Long): Map<String, Any> {
            return mapOf(
                "adminEmail" to adminEmail.ifBlank { SPINMINT_ADMIN_EMAIL }.take(160),
                "claimedCoins" to claimedCoins.coerceAtLeast(0L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        }
    }
}

