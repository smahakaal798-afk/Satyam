package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.WithdrawalTransaction
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

private val Context.spinMintDataStore: DataStore<Preferences> by preferencesDataStore(name = "spinmint_wallet")

data class PersistedGameSnapshot(
    val coins: Long = 0L,
    val freeSpinsLeft: Int = 5,
    val lastSpinResetEpochDay: Long = 0L,
    val nextAdAvailableEpochMs: Long = 0L,
    val energy: Int = 100,
    val lastEnergyUpdateMs: Long = 0L,
    val streakDay: Int = 0,
    val lastCheckinEpochDay: Long = 0L,
    val isMuted: Boolean = false,
    val transactions: List<WithdrawalTransaction> = emptyList(),
    val totalTaps: Long = 0L,
    val totalSpins: Int = 0,
    val securityPin: String = "",
    val lockAppOnLaunch: Boolean = false,
    val requirePinForWallet: Boolean = true
)

class GameRepository(
    private val context: Context,
    userId: String = "guest"
) {

    private val moshi = Moshi.Builder().build()
    private val txListType = Types.newParameterizedType(List::class.java, WithdrawalTransaction::class.java)
    private val txAdapter = moshi.adapter<List<WithdrawalTransaction>>(txListType)

    private val prefix = userId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").ifBlank { "guest" }

    private val coinsKey = longPreferencesKey("${prefix}_coins")
    private val freeSpinsLeftKey = intPreferencesKey("${prefix}_free_spins_left")
    private val lastSpinResetDayKey = longPreferencesKey("${prefix}_last_spin_reset_day")
    private val nextAdAvailableMsKey = longPreferencesKey("${prefix}_next_ad_available_ms")
    private val energyKey = intPreferencesKey("${prefix}_energy")
    private val lastEnergyUpdateMsKey = longPreferencesKey("${prefix}_last_energy_update_ms")
    private val streakDayKey = intPreferencesKey("${prefix}_streak_day")
    private val lastCheckinDayKey = longPreferencesKey("${prefix}_last_checkin_day")
    private val isMutedKey = booleanPreferencesKey("is_muted")
    private val transactionsJsonKey = stringPreferencesKey("${prefix}_transactions_json")
    private val totalTapsKey = longPreferencesKey("${prefix}_total_taps")
    private val totalSpinsKey = intPreferencesKey("${prefix}_total_spins")
    private val securityPinKey = stringPreferencesKey("${prefix}_security_pin")
    private val lockAppOnLaunchKey = booleanPreferencesKey("${prefix}_lock_app_on_launch")
    private val requirePinForWalletKey = booleanPreferencesKey("${prefix}_require_pin_for_wallet")

    fun observeSnapshot(): Flow<PersistedGameSnapshot> {
        return context.spinMintDataStore.data.map { prefs -> mapPrefsToSnapshot(prefs) }
    }

    suspend fun currentSnapshot(): PersistedGameSnapshot {
        val prefs = context.spinMintDataStore.data.first()
        return mapPrefsToSnapshot(prefs)
    }

    private fun mapPrefsToSnapshot(prefs: Preferences): PersistedGameSnapshot {
        val currentDay = currentEpochDay()
        val savedResetDay = prefs[lastSpinResetDayKey] ?: 0L
        val spinsLeft = if (savedResetDay != currentDay) {
            5
        } else {
            prefs[freeSpinsLeftKey] ?: 5
        }

        val savedEnergy = prefs[energyKey] ?: 100
        val lastEnergyMs = prefs[lastEnergyUpdateMsKey] ?: System.currentTimeMillis()
        val recoveredEnergy = computeRecoveredEnergy(savedEnergy, lastEnergyMs, System.currentTimeMillis())

        val txJson = prefs[transactionsJsonKey]
        val parsedTransactions = if (txJson.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching { txAdapter.fromJson(txJson) ?: emptyList() }.getOrDefault(emptyList())
        }

        return PersistedGameSnapshot(
            coins = prefs[coinsKey] ?: 0L,
            freeSpinsLeft = spinsLeft,
            lastSpinResetEpochDay = if (savedResetDay == 0L) currentDay else savedResetDay,
            nextAdAvailableEpochMs = prefs[nextAdAvailableMsKey] ?: 0L,
            energy = recoveredEnergy,
            lastEnergyUpdateMs = lastEnergyMs,
            streakDay = prefs[streakDayKey] ?: 0,
            lastCheckinEpochDay = prefs[lastCheckinDayKey] ?: 0L,
            isMuted = prefs[isMutedKey] ?: false,
            transactions = parsedTransactions,
            totalTaps = prefs[totalTapsKey] ?: 0L,
            totalSpins = prefs[totalSpinsKey] ?: 0,
            securityPin = prefs[securityPinKey] ?: "",
            lockAppOnLaunch = prefs[lockAppOnLaunchKey] ?: false,
            requirePinForWallet = prefs[requirePinForWalletKey] ?: true
        )
    }

    suspend fun hydrateFromCloud(profile: UserWalletProfile) {
        context.spinMintDataStore.edit { prefs ->
            prefs[coinsKey] = profile.coins.coerceAtLeast(0L)
            prefs[freeSpinsLeftKey] = profile.freeSpinsLeft.toInt().coerceIn(0, 500)
            prefs[lastSpinResetDayKey] = profile.lastSpinResetEpochDay
            prefs[nextAdAvailableMsKey] = profile.nextAdAvailableEpochMs
            prefs[energyKey] = profile.energy.toInt().coerceIn(0, 100)
            prefs[lastEnergyUpdateMsKey] = profile.lastEnergyUpdateMs
            prefs[streakDayKey] = profile.streakDay.toInt().coerceIn(0, 7)
            prefs[lastCheckinDayKey] = profile.lastCheckinEpochDay
            prefs[totalTapsKey] = profile.totalTaps.coerceAtLeast(0L)
            prefs[totalSpinsKey] = profile.totalSpins.toInt().coerceAtLeast(0)
        }
    }

    suspend fun addCoins(delta: Long) {
        context.spinMintDataStore.edit { prefs ->
            val current = prefs[coinsKey] ?: 0L
            prefs[coinsKey] = (current + delta).coerceAtLeast(0L)
        }
    }

    suspend fun tryDeductCoins(amount: Long): Boolean {
        if (amount <= 0L) return true
        var deducted = false
        context.spinMintDataStore.edit { prefs ->
            val current = prefs[coinsKey] ?: 0L
            if (current >= amount) {
                prefs[coinsKey] = current - amount
                deducted = true
            }
        }
        return deducted
    }

    suspend fun recordTap(coinDelta: Long, maxEnergy: Int = 100): Boolean {
        var success = false
        val now = System.currentTimeMillis()
        context.spinMintDataStore.edit { prefs ->
            val savedEnergy = prefs[energyKey] ?: maxEnergy
            val lastUpdate = prefs[lastEnergyUpdateMsKey] ?: now
            val currentEnergy = computeRecoveredEnergy(savedEnergy, lastUpdate, now, maxEnergy)
            if (currentEnergy > 0) {
                prefs[energyKey] = currentEnergy - 1
                prefs[lastEnergyUpdateMsKey] = now
                prefs[coinsKey] = (prefs[coinsKey] ?: 0L) + coinDelta
                prefs[totalTapsKey] = (prefs[totalTapsKey] ?: 0L) + 1L
                success = true
            }
        }
        return success
    }

    suspend fun syncRecoveredEnergy(maxEnergy: Int = 100) {
        val now = System.currentTimeMillis()
        context.spinMintDataStore.edit { prefs ->
            val savedEnergy = prefs[energyKey] ?: maxEnergy
            val lastUpdate = prefs[lastEnergyUpdateMsKey] ?: now
            val recovered = computeRecoveredEnergy(savedEnergy, lastUpdate, now, maxEnergy)
            if (recovered != savedEnergy) {
                prefs[energyKey] = recovered
                prefs[lastEnergyUpdateMsKey] = now
            }
        }
    }

    suspend fun consumeFreeSpin(): Boolean {
        val today = currentEpochDay()
        var consumed = false
        context.spinMintDataStore.edit { prefs ->
            val resetDay = prefs[lastSpinResetDayKey] ?: 0L
            val spins = if (resetDay != today) 5 else (prefs[freeSpinsLeftKey] ?: 5)
            if (spins > 0) {
                prefs[freeSpinsLeftKey] = spins - 1
                prefs[lastSpinResetDayKey] = today
                prefs[totalSpinsKey] = (prefs[totalSpinsKey] ?: 0) + 1
                consumed = true
            }
        }
        return consumed
    }

    suspend fun grantAdBonusSpin(cooldownMs: Long = 30_000L) {
        val today = currentEpochDay()
        val now = System.currentTimeMillis()
        context.spinMintDataStore.edit { prefs ->
            val resetDay = prefs[lastSpinResetDayKey] ?: 0L
            val spins = if (resetDay != today) 5 else (prefs[freeSpinsLeftKey] ?: 5)
            prefs[freeSpinsLeftKey] = spins + 1
            prefs[lastSpinResetDayKey] = today
            prefs[nextAdAvailableMsKey] = now + cooldownMs
        }
    }

    suspend fun claimDailyCheckin(rewardCoins: Long, nextStreakDay: Int) {
        val today = currentEpochDay()
        context.spinMintDataStore.edit { prefs ->
            prefs[coinsKey] = (prefs[coinsKey] ?: 0L) + rewardCoins
            prefs[streakDayKey] = nextStreakDay
            prefs[lastCheckinDayKey] = today
        }
    }

    suspend fun toggleMute(): Boolean {
        var newMuted = false
        context.spinMintDataStore.edit { prefs ->
            val current = prefs[isMutedKey] ?: false
            newMuted = !current
            prefs[isMutedKey] = newMuted
        }
        return newMuted
    }

    suspend fun submitWithdrawal(transaction: WithdrawalTransaction): Boolean {
        var submitted = false
        context.spinMintDataStore.edit { prefs ->
            val currentCoins = prefs[coinsKey] ?: 0L
            if (currentCoins >= transaction.coinsSpent) {
                prefs[coinsKey] = currentCoins - transaction.coinsSpent
                val existingJson = prefs[transactionsJsonKey]
                val existing = if (existingJson.isNullOrBlank()) {
                    emptyList()
                } else {
                    runCatching { txAdapter.fromJson(existingJson) ?: emptyList() }.getOrDefault(emptyList())
                }
                val updated = listOf(transaction) + existing
                prefs[transactionsJsonKey] = txAdapter.toJson(updated)
                submitted = true
            }
        }
        return submitted
    }

    suspend fun recordDeposit(transaction: WithdrawalTransaction) {
        context.spinMintDataStore.edit { prefs ->
            val currentCoins = prefs[coinsKey] ?: 0L
            prefs[coinsKey] = currentCoins + transaction.coinsSpent
            val existingJson = prefs[transactionsJsonKey]
            val existing = if (existingJson.isNullOrBlank()) {
                emptyList()
            } else {
                runCatching { txAdapter.fromJson(existingJson) ?: emptyList() }.getOrDefault(emptyList())
            }
            val updated = listOf(transaction) + existing
            prefs[transactionsJsonKey] = txAdapter.toJson(updated)
        }
    }

    suspend fun setSecurityPin(pin: String) {
        context.spinMintDataStore.edit { prefs ->
            prefs[securityPinKey] = pin
        }
    }

    suspend fun setLockAppOnLaunch(enabled: Boolean) {
        context.spinMintDataStore.edit { prefs ->
            prefs[lockAppOnLaunchKey] = enabled
        }
    }

    suspend fun setRequirePinForWallet(enabled: Boolean) {
        context.spinMintDataStore.edit { prefs ->
            prefs[requirePinForWalletKey] = enabled
        }
    }

    companion object {
        fun currentEpochDay(): Long {
            return TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
        }

        fun computeRecoveredEnergy(
            savedEnergy: Int,
            lastUpdateMs: Long,
            nowMs: Long,
            maxEnergy: Int = 100
        ): Int {
            if (savedEnergy >= maxEnergy) return maxEnergy
            val elapsedMs = (nowMs - lastUpdateMs).coerceAtLeast(0L)
            val gained = (elapsedMs / 1000L).toInt()
            return (savedEnergy + gained).coerceIn(0, maxEnergy)
        }
    }
}
