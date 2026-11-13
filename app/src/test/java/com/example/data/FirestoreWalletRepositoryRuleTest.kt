package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.example.model.WithdrawalTransaction
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class FirestoreWalletRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun getOrCreateAndSyncProfile_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = FirestoreWalletRepository(firestore, auth)

        val created = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getOrCreateUserProfile("Alice Mint", ALICE_EMAIL, null)
        }
        assertTrue(created.isSuccess)
        assertEquals(uid, created.getOrThrow().userId)

        val updatedSnapshot = PersistedGameSnapshot(
            coins = 1500L,
            freeSpinsLeft = 3,
            lastSpinResetEpochDay = 20400L,
            nextAdAvailableEpochMs = 0L,
            energy = 88,
            lastEnergyUpdateMs = System.currentTimeMillis(),
            streakDay = 2,
            lastCheckinEpochDay = 20400L,
            totalTaps = 45L,
            totalSpins = 2
        )
        val synced = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncWalletSnapshot(updatedSnapshot, "Alice Mint", ALICE_EMAIL, null)
        }
        assertTrue(synced.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserProfile().first { it != null && it.coins == 1500L }
        }
        assertNotNull(observed)
        assertEquals(1500L, observed?.coins)
        assertEquals(3L, observed?.freeSpinsLeft)
    }

    @Test
    fun recordTransaction_authenticatedOwner_appendsAndObserves() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = FirestoreWalletRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getOrCreateUserProfile("Alice Mint", ALICE_EMAIL, null).getOrThrow()
        }

        val txId = UUID.randomUUID().toString()
        val tx = WithdrawalTransaction(
            id = txId,
            referenceCode = "DEP-772211",
            methodName = "UPI",
            accountIdentifier = "alice@okaxis",
            coinsSpent = 5000L,
            inrEquivalent = 50.0,
            usdEquivalent = 0.5,
            timestampMillis = System.currentTimeMillis(),
            status = "Credited",
            txType = "DEPOSIT"
        )

        val writeRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.recordTransaction(tx, "Alice Mint", ALICE_EMAIL, null)
        }
        assertTrue(writeRes.isSuccess)

        val list = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTransactions().first { items -> items.any { it.id == txId } }
        }
        assertTrue(list.any { it.referenceCode == "DEP-772211" && it.coinsSpent == 5000L })
    }

    @Test
    fun crossUserReadAndWrite_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = FirestoreWalletRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.getOrCreateUserProfile("Alice Mint", ALICE_EMAIL, null).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = FirestoreWalletRepository(firestore, auth)

        val readResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getProfileForUserId(aliceUid)
        }
        assertTrue(readResult.isFailure)
        val readException = readResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(readException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, readException?.code)

        val spoofTx = WithdrawalTransaction(
            id = UUID.randomUUID().toString(),
            referenceCode = "SM-999999",
            methodName = "UPI",
            accountIdentifier = "bob@okaxis",
            coinsSpent = 10000L,
            inrEquivalent = 100.0,
            usdEquivalent = 1.0,
            timestampMillis = System.currentTimeMillis(),
            status = "Submitted",
            txType = "WITHDRAWAL"
        )
        val writeResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.recordTransactionForUserId(aliceUid, spoofTx)
        }
        assertTrue(writeResult.isFailure)
        val writeException = writeResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(writeException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, writeException?.code)
    }

    @Test
    fun unauthenticatedAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repo = FirestoreWalletRepository(firestore, auth)

        val readResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.getProfileForUserId(aliceUid)
        }
        assertTrue(readResult.isFailure)
        val firestoreException = readResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun recordAdminCommission_playerCanLogAndNonAdminCannotRead() = runBlocking {
        signInTestUser(BOB_EMAIL)
        val bobRepo = FirestoreWalletRepository(firestore, auth)

        val logRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.recordAdminCommission(
                sourceGame = "Ludo",
                grossWinCoins = 500L,
                commissionCoins = 5L,
                netPlayerCoins = 495L,
                playerName = "Bob Player"
            )
        }
        org.junit.Assert.assertNull(
            "Expected recordAdminCommission to succeed, got: ${logRes.exceptionOrNull()?.stackTraceToString()}",
            logRes.exceptionOrNull()
        )
        assertTrue(logRes.isSuccess)

        var listException: FirebaseFirestoreException? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeAdminCommissions().first()
            }
        } catch (e: Exception) {
            listException = e.unwrapFirestoreException()
        }
        assertNotNull(listException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, listException?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_spinmint@test.com"
        const val BOB_EMAIL = "bob_spinmint@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
