package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEffectsEngine
import com.example.data.FirestoreWalletRepository
import com.example.data.GameRepository
import com.example.data.PersistedGameSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import com.example.model.AndarBaharSide
import com.example.model.AndarBaharState
import com.example.model.ArcadeGameType
import com.example.model.BlackjackState
import com.example.model.CardAndBoardEngine
import com.example.model.DAILY_REWARD_DAYS
import com.example.model.DEFAULT_WHEEL_SEGMENTS
import com.example.model.DepositPackage
import com.example.model.DiceChoice
import com.example.model.DiceGameState
import com.example.model.EarningMode
import com.example.model.FruitSlotsState
import com.example.model.FruitSymbol
import com.example.model.GameUiState
import com.example.model.LudoGameState
import com.example.model.MineTileState
import com.example.model.MinesGameState
import com.example.model.PaymentMethod
import com.example.model.PlinkoDropPlan
import com.example.model.PlinkoGameState
import com.example.model.PlayingCard
import com.example.model.SecurityState
import com.example.model.TeenPattiState
import com.example.model.ToastMessage
import com.example.model.ToastType
import com.example.model.WheelSegment
import com.example.model.WithdrawalTransaction
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToLong
import kotlin.random.Random

class GameViewModel(
    private val repository: GameRepository,
    private val firestoreRepo: FirestoreWalletRepository? = null,
    private val currentUserId: String = "guest",
    private val userDisplayName: String = "SpinMint Player",
    private val userEmail: String? = null,
    private val userPhotoUrl: String? = null,
    private val isAdmin: Boolean = false
) : ViewModel() {

    val soundEngine = SoundEffectsEngine()

    private val _uiState = MutableStateFlow(
        GameUiState(
            scratchCardPrize = randomScratchPrize(),
            userId = currentUserId,
            userDisplayName = userDisplayName,
            userEmail = userEmail,
            isAdmin = isAdmin
        )
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var lastSnapshot = PersistedGameSnapshot()
    private var cloudTransactions: List<WithdrawalTransaction> = emptyList()
    private var toastCounter = 0L
    private var hasEvaluatedLaunchLock = false
    private var tapSyncJob: Job? = null
    private var pendingCommissionPool = 0L

    init {
        viewModelScope.launch {
            repository.observeSnapshot().collect { snapshot ->
                lastSnapshot = snapshot
                soundEngine.isMuted = snapshot.isMuted
                val today = GameRepository.currentEpochDay()
                val effectiveStreak = computeEffectiveStreak(
                    savedStreak = snapshot.streakDay,
                    lastCheckinDay = snapshot.lastCheckinEpochDay,
                    today = today
                )
                val canClaimToday = snapshot.lastCheckinEpochDay != today

                val hasPin = snapshot.securityPin.length == 4
                val shouldLockOnFirstRead = !hasEvaluatedLaunchLock && hasPin && snapshot.lockAppOnLaunch
                hasEvaluatedLaunchLock = true

                _uiState.update { state ->
                    val mergedTx = mergeTransactions(cloudTransactions, snapshot.transactions)
                    state.copy(
                        coins = snapshot.coins,
                        freeSpinsLeft = snapshot.freeSpinsLeft,
                        energy = snapshot.energy,
                        streakDay = effectiveStreak,
                        canClaimDailyToday = canClaimToday,
                        isMuted = snapshot.isMuted,
                        transactions = mergedTx,
                        totalTaps = snapshot.totalTaps,
                        totalSpins = snapshot.totalSpins,
                        securityState = SecurityState(
                            hasPin = hasPin,
                            savedPin = snapshot.securityPin,
                            lockAppOnLaunch = snapshot.lockAppOnLaunch,
                            requirePinForWallet = snapshot.requirePinForWallet,
                            isAppLocked = if (shouldLockOnFirstRead) true else state.securityState.isAppLocked
                        )
                    )
                }
            }
        }

        // Cloud profile hydration and realtime transaction ledger observation
        if (firestoreRepo != null && currentUserId.isNotBlank() && currentUserId != "guest") {
            viewModelScope.launch {
                val localSnap = repository.currentSnapshot()
                val profileResult = firestoreRepo.getOrCreateUserProfile(
                    displayName = userDisplayName,
                    email = userEmail,
                    photoUrl = userPhotoUrl
                )
                profileResult.getOrNull()?.let { cloudProfile ->
                    if (localSnap.coins == 0L && localSnap.totalTaps == 0L && localSnap.totalSpins == 0) {
                        repository.hydrateFromCloud(cloudProfile)
                    } else if (cloudProfile.totalTaps > localSnap.totalTaps || cloudProfile.coins > localSnap.coins) {
                        repository.hydrateFromCloud(cloudProfile)
                    } else {
                        firestoreRepo.syncWalletSnapshot(localSnap, userDisplayName, userEmail, userPhotoUrl)
                    }
                }
            }

            viewModelScope.launch {
                firestoreRepo.observeTransactions()
                    .catch { error ->
                        val firestoreError = generateSequence(error) { it.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                        if (firestoreError == null && error is CancellationException) throw error
                        val rootError = firestoreError ?: error
                        Log.w("GameViewModel", "Error observing cloud transactions", rootError)
                    }
                    .collect { cloudRecords ->
                        cloudTransactions = cloudRecords.map { it.toUiTransaction() }
                        _uiState.update { state ->
                            state.copy(transactions = mergeTransactions(cloudTransactions, lastSnapshot.transactions))
                        }
                    }
            }

            if (isAdmin) {
                viewModelScope.launch {
                    firestoreRepo.observeAdminCommissions()
                        .catch { error ->
                            val firestoreError = generateSequence(error) { it.cause }
                                .filterIsInstance<FirebaseFirestoreException>()
                                .firstOrNull()
                            if (firestoreError == null && error is CancellationException) throw error
                            Log.w("GameViewModel", "Error observing admin commissions", firestoreError ?: error)
                        }
                        .collect { records ->
                            _uiState.update { it.copy(adminCommissions = records) }
                        }
                }

                viewModelScope.launch {
                    firestoreRepo.observeAdminTreasury()
                        .catch { error ->
                            val firestoreError = generateSequence(error) { it.cause }
                                .filterIsInstance<FirebaseFirestoreException>()
                                .firstOrNull()
                            if (firestoreError == null && error is CancellationException) throw error
                            Log.w("GameViewModel", "Error observing admin treasury", firestoreError ?: error)
                        }
                        .collect { claimedCoins ->
                            _uiState.update { it.copy(claimedAdminCoins = claimedCoins) }
                        }
                }
            }
        }

        // Live 1-second loop for energy regeneration, ad cooldown, and daily spin reset timer
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                val now = System.currentTimeMillis()
                val recoveredEnergy = GameRepository.computeRecoveredEnergy(
                    savedEnergy = lastSnapshot.energy,
                    lastUpdateMs = lastSnapshot.lastEnergyUpdateMs,
                    nowMs = now
                )
                if (recoveredEnergy > _uiState.value.energy) {
                    repository.syncRecoveredEnergy()
                }
                val adRemainingSec = ((lastSnapshot.nextAdAvailableEpochMs - now) / 1000L)
                    .coerceAtLeast(0L)
                    .toInt()
                val resetCountdown = computeMidnightCountdown()

                _uiState.update {
                    it.copy(
                        energy = recoveredEnergy,
                        adCooldownSeconds = adRemainingSec,
                        spinResetCountdownText = resetCountdown
                    )
                }
            }
        }
    }

    fun selectMode(mode: EarningMode) {
        _uiState.update { it.copy(activeMode = mode) }
    }

    fun toggleMute() {
        viewModelScope.launch {
            val newMuted = repository.toggleMute()
            soundEngine.isMuted = newMuted
            showToast(
                title = if (newMuted) "Sound Muted" else "Sound Enabled",
                subtitle = if (newMuted) "Audio effects paused" else "Arcade sound effects active",
                type = ToastType.INFO
            )
        }
    }

    fun onCoinTapped() {
        val currentState = _uiState.value
        if (currentState.energy <= 0) {
            soundEngine.playErrorTone()
            showToast(
                title = "Energy Depleted!",
                subtitle = "Recharging +1 energy per second. Try Spin or Scratch while recharging!",
                type = ToastType.ERROR
            )
            return
        }
        soundEngine.playCoinTap()
        viewModelScope.launch {
            val split = splitWinWithOnePercentAdminFee(currentState.tapPower, pendingCommissionPool)
            val tapped = repository.recordTap(coinDelta = split.netPlayerCoins)
            if (tapped) {
                pendingCommissionPool = split.nextPendingPool
                if (split.adminFeeCoins > 0L) {
                    firestoreRepo?.recordAdminCommission(
                        sourceGame = "Tap-to-Earn",
                        grossWinCoins = currentState.tapPower,
                        commissionCoins = split.adminFeeCoins,
                        netPlayerCoins = split.netPlayerCoins,
                        playerName = userDisplayName
                    )
                }
                scheduleTapCloudSync()
            }
        }
    }

    /**
     * Consumes a free spin and returns the chosen winning WheelSegment, or null if no spins remain.
     */
    suspend fun requestWheelSpin(): WheelSegment? {
        val state = _uiState.value
        if (state.isSpinning) return null
        if (state.freeSpinsLeft <= 0) {
            soundEngine.playErrorTone()
            showToast(
                title = "No Spins Remaining",
                subtitle = "Tap 'Watch Ad (+1 Spin)' below or wait for daily reset!",
                type = ToastType.ERROR
            )
            return null
        }

        val consumed = repository.consumeFreeSpin()
        if (!consumed) return null

        _uiState.update { it.copy(isSpinning = true) }
        return pickWeightedSegment()
    }

    fun onWheelTick() {
        soundEngine.playWheelTick()
    }

    fun onWheelSpinFinished(segment: WheelSegment) {
        viewModelScope.launch {
            if (segment.coins > 0) {
                val split = allocateWinWithAdminFee(segment.coins, "Lucky Spin")
                repository.addCoins(split.netPlayerCoins)
                soundEngine.playWinFanfare(isJackpot = segment.isJackpot || segment.coins >= 500)
                val triggerConfetti = segment.coins >= 500
                _uiState.update {
                    it.copy(
                        isSpinning = false,
                        confettiTriggerId = if (triggerConfetti) System.currentTimeMillis() else it.confettiTriggerId
                    )
                }
                showToast(
                    title = if (segment.isJackpot) "JACKPOT! +${split.netPlayerCoins} Coins!" else "+${split.netPlayerCoins} Coins Won!",
                    subtitle = if (split.adminFeeCoins > 0L) {
                        "1% Admin fee (${split.adminFeeCoins}) • Added to Mint Wallet"
                    } else {
                        "Added straight to your Mint Wallet"
                    },
                    type = if (segment.isJackpot || segment.coins >= 500) ToastType.JACKPOT else ToastType.SUCCESS
                )
            } else {
                soundEngine.playErrorTone()
                _uiState.update { it.copy(isSpinning = false) }
                showToast(
                    title = "Better Luck Next Spin!",
                    subtitle = "Spin again or tap the Gold Coin for guaranteed coins",
                    type = ToastType.INFO
                )
            }
            syncCloudSnapshotImmediate()
        }
    }

    fun watchSimulatedAdForSpin() {
        val state = _uiState.value
        if (state.isWatchingAd || state.isSpinning) return
        if (state.adCooldownSeconds > 0) {
            soundEngine.playErrorTone()
            showToast(
                title = "Ad Cooling Down",
                subtitle = "Next bonus spin ad unlocks in ${state.adCooldownSeconds}s",
                type = ToastType.INFO
            )
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isWatchingAd = true, adProgress = 0f) }
            val steps = 30
            for (step in 1..steps) {
                delay(100L)
                _uiState.update { it.copy(adProgress = step / steps.toFloat()) }
            }
            repository.grantAdBonusSpin(cooldownMs = 25_000L)
            syncCloudSnapshotImmediate()
            soundEngine.playWinFanfare(isJackpot = false)
            _uiState.update { it.copy(isWatchingAd = false, adProgress = 0f) }
            showToast(
                title = "+1 Bonus Spin Unlocked!",
                subtitle = "Rewarded video complete — spin the wheel now",
                type = ToastType.SUCCESS
            )
        }
    }

    fun claimDailyCheckin() {
        val state = _uiState.value
        if (!state.canClaimDailyToday) {
            soundEngine.playErrorTone()
            showToast(
                title = "Already Claimed Today",
                subtitle = "Come back tomorrow to continue your 7-day streak!",
                type = ToastType.INFO
            )
            return
        }
        val targetDayIndex = state.streakDay.coerceIn(0, 6)
        val reward = DAILY_REWARD_DAYS[targetDayIndex]
        val nextStreak = if (targetDayIndex == 6) 7 else targetDayIndex + 1

        viewModelScope.launch {
            val split = allocateWinWithAdminFee(reward.coins, "Daily Streak")
            repository.claimDailyCheckin(rewardCoins = split.netPlayerCoins, nextStreakDay = nextStreak)
            syncCloudSnapshotImmediate()
            soundEngine.playWinFanfare(isJackpot = reward.isJackpotDay || reward.coins >= 500)
            _uiState.update {
                it.copy(
                    confettiTriggerId = if (reward.coins >= 500) System.currentTimeMillis() else it.confettiTriggerId
                )
            }
            showToast(
                title = "Day ${reward.dayNumber} Claimed! +${split.netPlayerCoins} Coins",
                subtitle = if (reward.isJackpotDay) "7-Day Mega Streak Completed! (1% Admin fee: ${split.adminFeeCoins})" else "1% Admin fee (${split.adminFeeCoins}) • Streak active",
                type = if (reward.isJackpotDay) ToastType.JACKPOT else ToastType.SUCCESS
            )
        }
    }

    fun onScratchStroke() {
        soundEngine.playScratchTick()
    }

    fun revealScratchCardReward() {
        val state = _uiState.value
        if (state.scratchCardRevealed) return
        val prize = state.scratchCardPrize
        viewModelScope.launch {
            val split = allocateWinWithAdminFee(prize, "Scratch Card")
            repository.addCoins(split.netPlayerCoins)
            syncCloudSnapshotImmediate()
            soundEngine.playWinFanfare(isJackpot = prize >= 500)
            _uiState.update {
                it.copy(
                    scratchCardRevealed = true,
                    confettiTriggerId = if (prize >= 500) System.currentTimeMillis() else it.confettiTriggerId
                )
            }
            showToast(
                title = "Scratch Win! +${split.netPlayerCoins} Coins",
                subtitle = if (split.adminFeeCoins > 0L) "1% Admin fee (${split.adminFeeCoins}) • Credited to wallet" else "Mint Coins credited to your wallet",
                type = if (prize >= 500) ToastType.JACKPOT else ToastType.SUCCESS
            )
        }
    }

    fun generateNewScratchCard() {
        _uiState.update {
            it.copy(
                scratchCardPrize = randomScratchPrize(),
                scratchCardRevealed = false,
                scratchCardVersion = it.scratchCardVersion + 1
            )
        }
    }

    fun openWithdrawModal() {
        _uiState.update { it.copy(showWithdrawModal = true) }
    }

    fun closeWithdrawModal() {
        _uiState.update { it.copy(showWithdrawModal = false) }
    }

    fun openHistorySheet() {
        _uiState.update { it.copy(showHistorySheet = true) }
    }

    fun closeHistorySheet() {
        _uiState.update { it.copy(showHistorySheet = false) }
    }

    fun dismissReceipt() {
        _uiState.update { it.copy(latestReceipt = null) }
    }

    fun dismissToast() {
        _uiState.update { it.copy(activeToast = null) }
    }

    fun openDepositModal() {
        _uiState.update { it.copy(showDepositModal = true) }
    }

    fun closeDepositModal() {
        _uiState.update { it.copy(showDepositModal = false) }
    }

    fun openSecurityModal() {
        _uiState.update { it.copy(showSecurityModal = true) }
    }

    fun closeSecurityModal() {
        _uiState.update { it.copy(showSecurityModal = false) }
    }

    fun openAccountModal() {
        _uiState.update { it.copy(showAccountModal = true) }
    }

    fun closeAccountModal() {
        _uiState.update { it.copy(showAccountModal = false) }
    }

    fun openAdminModal() {
        if (!isAdmin) return
        _uiState.update { it.copy(showAdminModal = true) }
    }

    fun closeAdminModal() {
        _uiState.update { it.copy(showAdminModal = false) }
    }

    fun claimAdminCommissionsToWallet() {
        if (!isAdmin) return
        val state = _uiState.value
        val unclaimed = state.unclaimedAdminCoins
        if (unclaimed <= 0L) {
            showToast(
                title = "All Caught Up",
                subtitle = "All 1% commissions have already been claimed to your wallet.",
                type = ToastType.INFO
            )
            return
        }
        val newTotalClaimed = state.totalAdminCommissionCoins
        viewModelScope.launch {
            repository.addCoins(unclaimed)
            firestoreRepo?.claimAdminCommissionCoins(
                newTotalClaimedCoins = newTotalClaimed,
                adminEmail = userEmail ?: com.example.data.SPINMINT_ADMIN_EMAIL
            )
            syncCloudSnapshotImmediate()
            soundEngine.playWinFanfare(isJackpot = true)
            _uiState.update {
                it.copy(
                    claimedAdminCoins = newTotalClaimed,
                    confettiTriggerId = System.currentTimeMillis()
                )
            }
            showToast(
                title = "Admin Commission Claimed! +$unclaimed Coins",
                subtitle = "1% player winnings credited to your Mint Wallet",
                type = ToastType.JACKPOT
            )
        }
    }

    fun submitWithdrawal(
        method: PaymentMethod,
        accountInput: String,
        requestedCoinsText: String,
        pinInput: String = ""
    ) {
        val security = _uiState.value.securityState
        if (security.hasPin && security.requirePinForWallet && pinInput != security.savedPin) {
            soundEngine.playErrorTone()
            showToast(
                title = "Security PIN Required",
                subtitle = "Enter your 4-digit Vault PIN to authorize this withdrawal.",
                type = ToastType.ERROR
            )
            return
        }

        val trimmedAccount = accountInput.trim()
        val requestedCoins = requestedCoinsText.trim().toLongOrNull() ?: 0L
        val currentCoins = _uiState.value.coins

        if (currentCoins < GameUiState.MIN_WITHDRAW_COINS) {
            soundEngine.playErrorTone()
            showToast(
                title = "Minimum Limit Not Reached",
                subtitle = "You need at least 10,000 Coins (₹100) to withdraw. Need ${10_000L - currentCoins} more!",
                type = ToastType.ERROR
            )
            return
        }

        if (requestedCoins < GameUiState.MIN_WITHDRAW_COINS) {
            soundEngine.playErrorTone()
            showToast(
                title = "Invalid Redemption Amount",
                subtitle = "Minimum withdrawal is 10,000 Coins (₹100 / $1.00).",
                type = ToastType.ERROR
            )
            return
        }

        if (requestedCoins > currentCoins) {
            soundEngine.playErrorTone()
            showToast(
                title = "Insufficient Balance",
                subtitle = "You entered $requestedCoins Coins, but your balance is $currentCoins Coins.",
                type = ToastType.ERROR
            )
            return
        }

        val validationError = validatePaymentIdentifier(method, trimmedAccount)
        if (validationError != null) {
            soundEngine.playErrorTone()
            showToast(
                title = "Check ${method.displayName} Details",
                subtitle = validationError,
                type = ToastType.ERROR
            )
            return
        }

        val tx = WithdrawalTransaction(
            id = UUID.randomUUID().toString(),
            referenceCode = "SM-${(100000..999999).random()}",
            methodName = method.displayName,
            accountIdentifier = trimmedAccount,
            coinsSpent = requestedCoins,
            inrEquivalent = requestedCoins * 0.01,
            usdEquivalent = requestedCoins * 0.0001,
            timestampMillis = System.currentTimeMillis(),
            status = "Submitted",
            txType = "WITHDRAWAL"
        )

        viewModelScope.launch {
            val ok = repository.submitWithdrawal(tx)
            if (ok) {
                firestoreRepo?.recordTransaction(tx, userDisplayName, userEmail, userPhotoUrl)
                syncCloudSnapshotImmediate()
                soundEngine.playCashOut()
                _uiState.update {
                    it.copy(
                        showWithdrawModal = false,
                        latestReceipt = tx,
                        confettiTriggerId = System.currentTimeMillis()
                    )
                }
                showToast(
                    title = "Withdrawal Submitted!",
                    subtitle = "${tx.referenceCode} • ₹${String.format(Locale.US, "%.2f", tx.inrEquivalent)} via ${tx.methodName}",
                    type = ToastType.SUCCESS
                )
            } else {
                soundEngine.playErrorTone()
                showToast(
                    title = "Redemption Failed",
                    subtitle = "Insufficient coins available.",
                    type = ToastType.ERROR
                )
            }
        }
    }

    fun submitDeposit(
        method: PaymentMethod,
        pkg: DepositPackage?,
        customInrText: String,
        accountInput: String,
        pinInput: String = ""
    ) {
        val security = _uiState.value.securityState
        if (security.hasPin && security.requirePinForWallet && pinInput != security.savedPin) {
            soundEngine.playErrorTone()
            showToast(
                title = "Security PIN Required",
                subtitle = "Enter your 4-digit Vault PIN to authorize deposit.",
                type = ToastType.ERROR
            )
            return
        }

        val inrAmount = pkg?.inrAmount ?: (customInrText.trim().toIntOrNull() ?: 0)
        if (inrAmount < 10) {
            soundEngine.playErrorTone()
            showToast(
                title = "Minimum Deposit ₹10",
                subtitle = "Choose a package or enter ₹10 or higher.",
                type = ToastType.ERROR
            )
            return
        }

        val coinsToCredit = pkg?.totalCoins ?: (inrAmount * 100L)
        val trimmedAccount = accountInput.trim().ifBlank { "${method.displayName} Wallet" }

        val tx = WithdrawalTransaction(
            id = UUID.randomUUID().toString(),
            referenceCode = "DEP-${(100000..999999).random()}",
            methodName = method.displayName,
            accountIdentifier = trimmedAccount,
            coinsSpent = coinsToCredit,
            inrEquivalent = inrAmount.toDouble(),
            usdEquivalent = inrAmount * 0.01,
            timestampMillis = System.currentTimeMillis(),
            status = "Credited",
            txType = "DEPOSIT"
        )

        viewModelScope.launch {
            repository.recordDeposit(tx)
            firestoreRepo?.recordTransaction(tx, userDisplayName, userEmail, userPhotoUrl)
            syncCloudSnapshotImmediate()
            soundEngine.playCashOut()
            _uiState.update {
                it.copy(
                    showDepositModal = false,
                    latestReceipt = tx,
                    confettiTriggerId = System.currentTimeMillis()
                )
            }
            showToast(
                title = "Deposit Credited! +$coinsToCredit Coins",
                subtitle = "Simulated ₹$inrAmount top-up via ${method.displayName}",
                type = ToastType.JACKPOT
            )
        }
    }

    // --- Security Vault Controls ---

    fun setVaultPin(pin: String) {
        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            soundEngine.playErrorTone()
            showToast(
                title = "Invalid PIN",
                subtitle = "Please enter exactly 4 digits.",
                type = ToastType.ERROR
            )
            return
        }
        viewModelScope.launch {
            repository.setSecurityPin(pin)
            soundEngine.playCashOut()
            showToast(
                title = "Vault PIN Enabled",
                subtitle = "Your wallet & deposits are now PIN protected.",
                type = ToastType.SUCCESS
            )
        }
    }

    fun disableVaultPin(currentPin: String) {
        val saved = _uiState.value.securityState.savedPin
        if (saved.isNotEmpty() && currentPin != saved) {
            soundEngine.playErrorTone()
            showToast(
                title = "Incorrect PIN",
                subtitle = "Enter your current 4-digit PIN to disable Vault Lock.",
                type = ToastType.ERROR
            )
            return
        }
        viewModelScope.launch {
            repository.setSecurityPin("")
            repository.setLockAppOnLaunch(false)
            _uiState.update {
                it.copy(securityState = it.securityState.copy(isAppLocked = false))
            }
            showToast(
                title = "Vault PIN Removed",
                subtitle = "App & wallet PIN protection turned off.",
                type = ToastType.INFO
            )
        }
    }

    fun toggleLaunchLock(enabled: Boolean) {
        if (enabled && !_uiState.value.securityState.hasPin) {
            soundEngine.playErrorTone()
            showToast(
                title = "Set a 4-Digit PIN First",
                subtitle = "Create your Vault PIN before enabling App Launch Lock.",
                type = ToastType.ERROR
            )
            return
        }
        viewModelScope.launch {
            repository.setLockAppOnLaunch(enabled)
        }
    }

    fun toggleWalletPinGuard(enabled: Boolean) {
        viewModelScope.launch {
            repository.setRequirePinForWallet(enabled)
        }
    }

    fun lockVaultNow() {
        if (!_uiState.value.securityState.hasPin) {
            soundEngine.playErrorTone()
            showToast(
                title = "No PIN Set",
                subtitle = "Set a 4-digit Vault PIN first to lock the app.",
                type = ToastType.INFO
            )
            return
        }
        _uiState.update {
            it.copy(
                showSecurityModal = false,
                securityState = it.securityState.copy(isAppLocked = true)
            )
        }
    }

    fun unlockAppWithPin(pin: String): Boolean {
        val saved = _uiState.value.securityState.savedPin
        return if (pin == saved) {
            soundEngine.playCoinTap()
            _uiState.update {
                it.copy(securityState = it.securityState.copy(isAppLocked = false))
            }
            true
        } else {
            soundEngine.playErrorTone()
            false
        }
    }

    // --- Arcade Mini-Games ---

    fun selectArcadeGame(type: ArcadeGameType) {
        _uiState.update { it.copy(activeArcadeGame = type) }
    }

    // 1. Gem Mines (5x5 grid with 3 hidden laser mines)
    fun startMinesGame(stakeCoins: Long, isFreePlay: Boolean) {
        val currentMines = _uiState.value.minesState
        if (currentMines.isActive) return

        viewModelScope.launch {
            if (!isFreePlay) {
                val ok = repository.tryDeductCoins(stakeCoins)
                if (!ok) {
                    soundEngine.playErrorTone()
                    showToast(
                        title = "Insufficient Coins",
                        subtitle = "Choose Free Play or a smaller stake to start Gem Mines!",
                        type = ToastType.ERROR
                    )
                    return@launch
                }
                syncCloudSnapshotImmediate()
            }

            val minePositions = (0 until 25).shuffled().take(3).toSet()
            _uiState.update {
                it.copy(
                    minesState = MinesGameState(
                        isActive = true,
                        isRoundOver = false,
                        hitMine = false,
                        stakeCoins = if (isFreePlay) 0L else stakeCoins,
                        isFreePlay = isFreePlay,
                        tiles = List(25) { MineTileState.HIDDEN },
                        mineIndices = minePositions,
                        gemsFound = 0,
                        currentMultiplier = 1.0,
                        potentialWinCoins = 0L
                    )
                )
            }
            soundEngine.playCoinTap()
        }
    }

    fun revealMineTile(index: Int) {
        val mines = _uiState.value.minesState
        if (!mines.isActive || mines.isRoundOver || index !in 0 until 25) return
        if (mines.tiles[index] != MineTileState.HIDDEN) return

        if (index in mines.mineIndices) {
            // Boom! Reveal all mines and remaining gems
            soundEngine.playErrorTone()
            val revealedTiles = List(25) { idx ->
                when {
                    idx in mines.mineIndices -> MineTileState.MINE
                    mines.tiles[idx] == MineTileState.GEM -> MineTileState.GEM
                    else -> MineTileState.HIDDEN
                }
            }
            _uiState.update {
                it.copy(
                    minesState = mines.copy(
                        isActive = false,
                        isRoundOver = true,
                        hitMine = true,
                        tiles = revealedTiles,
                        potentialWinCoins = 0L
                    )
                )
            }
            showToast(
                title = "Laser Mine Hit!",
                subtitle = "Round ended — start a new vault run anytime!",
                type = ToastType.ERROR
            )
        } else {
            val newGems = mines.gemsFound + 1
            val mult = computeMinesMultiplier(newGems)
            val baseStake = if (mines.isFreePlay) 25.0 else mines.stakeCoins.toDouble()
            val potential = (baseStake * mult).roundToLong()
            val updatedTiles = mines.tiles.toMutableList().apply { this[index] = MineTileState.GEM }

            soundEngine.playCoinTap()
            _uiState.update {
                it.copy(
                    minesState = mines.copy(
                        tiles = updatedTiles,
                        gemsFound = newGems,
                        currentMultiplier = mult,
                        potentialWinCoins = potential
                    )
                )
            }

            // If all 22 safe gems found, auto cash out with jackpot!
            if (newGems >= 22) {
                cashOutMines()
            }
        }
    }

    fun cashOutMines() {
        val mines = _uiState.value.minesState
        if (!mines.isActive || mines.gemsFound <= 0) return
        val payout = mines.potentialWinCoins

        val revealedAll = List(25) { idx ->
            when {
                idx in mines.mineIndices -> MineTileState.MINE
                mines.tiles[idx] == MineTileState.GEM -> MineTileState.GEM
                else -> MineTileState.HIDDEN
            }
        }

        viewModelScope.launch {
            val split = allocateWinWithAdminFee(payout, "Gem Mines")
            repository.addCoins(split.netPlayerCoins)
            syncCloudSnapshotImmediate()
            val bigWin = payout >= 500L
            soundEngine.playWinFanfare(isJackpot = bigWin)
            _uiState.update {
                it.copy(
                    minesState = mines.copy(
                        isActive = false,
                        isRoundOver = true,
                        hitMine = false,
                        tiles = revealedAll,
                        potentialWinCoins = split.netPlayerCoins
                    ),
                    confettiTriggerId = if (bigWin) System.currentTimeMillis() else it.confettiTriggerId
                )
            }
            showToast(
                title = "Vault Cashed Out! +${split.netPlayerCoins} Coins",
                subtitle = "${mines.gemsFound} Gems cleared • 1% Admin fee: ${split.adminFeeCoins}",
                type = if (bigWin) ToastType.JACKPOT else ToastType.SUCCESS
            )
        }
    }

    // 2. Plinko Coin Drop (6 peg rows -> 7 buckets)
    suspend fun requestPlinkoDrop(stakeCoins: Long, isFreePlay: Boolean): PlinkoDropPlan? {
        if (_uiState.value.plinkoState.isDropping) return null
        if (!isFreePlay) {
            val ok = repository.tryDeductCoins(stakeCoins)
            if (!ok) {
                soundEngine.playErrorTone()
                showToast(
                    title = "Insufficient Coins",
                    subtitle = "Switch to Free Drop or pick a smaller coin drop!",
                    type = ToastType.ERROR
                )
                return null
            }
        }

        val directions = List(6) { if (Random.nextBoolean()) 1 else -1 }
        val bucketIndex = directions.count { it == 1 }.coerceIn(0, 6)
        val mult = PLINKO_MULTIPLIERS[bucketIndex]
        val reward = if (isFreePlay) {
            PLINKO_FREE_REWARDS[bucketIndex]
        } else {
            (stakeCoins * mult).roundToLong()
        }

        _uiState.update {
            it.copy(plinkoState = it.plinkoState.copy(isDropping = true))
        }
        return PlinkoDropPlan(
            stakeCoins = if (isFreePlay) 0L else stakeCoins,
            isFreePlay = isFreePlay,
            directions = directions,
            bucketIndex = bucketIndex,
            multiplier = mult,
            rewardCoins = reward
        )
    }

    fun onPlinkoPegHit() {
        soundEngine.playWheelTick()
    }

    fun completePlinkoDrop(plan: PlinkoDropPlan) {
        viewModelScope.launch {
            val split = allocateWinWithAdminFee(plan.rewardCoins, "Plinko Drop")
            repository.addCoins(split.netPlayerCoins)
            syncCloudSnapshotImmediate()
            val isBig = plan.multiplier >= 2.0 || plan.rewardCoins >= 250L
            soundEngine.playWinFanfare(isJackpot = plan.multiplier >= 5.0)
            _uiState.update {
                it.copy(
                    plinkoState = PlinkoGameState(
                        isDropping = false,
                        lastBucketIndex = plan.bucketIndex,
                        lastWinCoins = split.netPlayerCoins
                    ),
                    confettiTriggerId = if (isBig) System.currentTimeMillis() else it.confettiTriggerId
                )
            }
            showToast(
                title = if (plan.multiplier >= 5.0) "5.0x PLINKO JACKPOT! +${split.netPlayerCoins}" else "+${split.netPlayerCoins} Coins Landed!",
                subtitle = if (split.adminFeeCoins > 0L) "Landed in ${plan.multiplier}x • 1% Admin fee (${split.adminFeeCoins})" else "Landed in ${plan.multiplier}x bucket",
                type = if (plan.multiplier >= 5.0) ToastType.JACKPOT else ToastType.SUCCESS
            )
        }
    }

    // 3. Lucky Dice 7
    fun rollDice(choice: DiceChoice, stakeCoins: Long, isFreePlay: Boolean) {
        if (_uiState.value.diceState.isRolling) return

        viewModelScope.launch {
            if (!isFreePlay) {
                val ok = repository.tryDeductCoins(stakeCoins)
                if (!ok) {
                    soundEngine.playErrorTone()
                    showToast(
                        title = "Insufficient Coins",
                        subtitle = "Select Free Roll or lower your dice stake.",
                        type = ToastType.ERROR
                    )
                    return@launch
                }
            }

            _uiState.update {
                it.copy(
                    diceState = it.diceState.copy(
                        isRolling = true,
                        lastWinCoins = null,
                        lastResultSummary = "Rolling the Neon Dice…"
                    )
                )
            }

            repeat(7) {
                delay(85L)
                soundEngine.playWheelTick()
                _uiState.update { state ->
                    state.copy(
                        diceState = state.diceState.copy(
                            dieOne = Random.nextInt(1, 7),
                            dieTwo = Random.nextInt(1, 7)
                        )
                    )
                }
            }

            val d1 = Random.nextInt(1, 7)
            val d2 = Random.nextInt(1, 7)
            val total = d1 + d2
            val won = when (choice) {
                DiceChoice.UNDER_7 -> total < 7
                DiceChoice.EXACT_7 -> total == 7
                DiceChoice.OVER_7 -> total > 7
            }

            val payout = if (!won) {
                0L
            } else if (isFreePlay) {
                if (choice == DiceChoice.EXACT_7) 200L else 80L
            } else {
                (stakeCoins * choice.multiplier).roundToLong()
            }

            if (payout > 0L) {
                val split = allocateWinWithAdminFee(payout, "Lucky Dice 7")
                repository.addCoins(split.netPlayerCoins)
                syncCloudSnapshotImmediate()
                val bigWin = choice == DiceChoice.EXACT_7 || payout >= 500L
                soundEngine.playWinFanfare(isJackpot = bigWin)
                _uiState.update {
                    it.copy(
                        diceState = DiceGameState(
                            dieOne = d1,
                            dieTwo = d2,
                            isRolling = false,
                            lastWinCoins = split.netPlayerCoins,
                            lastResultSummary = "Rolled $total ($d1 + $d2) • Won +${split.netPlayerCoins} Coins!"
                        ),
                        confettiTriggerId = if (bigWin) System.currentTimeMillis() else it.confettiTriggerId
                    )
                }
                showToast(
                    title = "Dice Win! +${split.netPlayerCoins} Coins",
                    subtitle = if (split.adminFeeCoins > 0L) "Total $total • 1% Admin fee (${split.adminFeeCoins})" else "Total $total matched ${choice.label}",
                    type = if (bigWin) ToastType.JACKPOT else ToastType.SUCCESS
                )
            } else {
                soundEngine.playErrorTone()
                _uiState.update {
                    it.copy(
                        diceState = DiceGameState(
                            dieOne = d1,
                            dieTwo = d2,
                            isRolling = false,
                            lastWinCoins = 0L,
                            lastResultSummary = "Rolled $total ($d1 + $d2) • Needed ${choice.label}"
                        )
                    )
                }
                showToast(
                    title = "Rolled $total ($d1 + $d2)",
                    subtitle = "Missed ${choice.label} — try another roll!",
                    type = ToastType.INFO
                )
            }
        }
    }

    // 4. Ludo Quick Match (Player Emerald vs Bot Gold, first 2 tokens Home wins!)
    fun startLudoMatch(stakeCoins: Long, isFreePlay: Boolean) {
        viewModelScope.launch {
            if (!isFreePlay) {
                val ok = repository.tryDeductCoins(stakeCoins)
                if (!ok) {
                    soundEngine.playErrorTone()
                    showToast(
                        title = "Insufficient Coins",
                        subtitle = "Choose Free Play or a smaller Ludo stake.",
                        type = ToastType.ERROR
                    )
                    return@launch
                }
            }
            _uiState.update {
                it.copy(
                    ludoState = LudoGameState(
                        isActive = true,
                        isPlayerTurn = true,
                        isBotThinking = false,
                        diceValue = 6,
                        hasRolled = false,
                        playerTokens = listOf(0, -1, -1, -1), // 1 token deployed on start for fast fun!
                        botTokens = listOf(0, -1, -1, -1),
                        movableTokenIndices = emptyList(),
                        stakeCoins = if (isFreePlay) 0L else stakeCoins,
                        isFreePlay = isFreePlay,
                        winnerLabel = null,
                        statusText = "Your turn! Roll the Ludo dice."
                    )
                )
            }
            soundEngine.playCoinTap()
        }
    }

    fun rollLudoDice() {
        val ludo = _uiState.value.ludoState
        if (!ludo.isActive || !ludo.isPlayerTurn || ludo.hasRolled || ludo.isBotThinking) return

        viewModelScope.launch {
            repeat(5) {
                soundEngine.playWheelTick()
                _uiState.update { state ->
                    state.copy(ludoState = state.ludoState.copy(diceValue = Random.nextInt(1, 7)))
                }
                delay(70L)
            }
            val rolled = Random.nextInt(1, 7)
            val movable = CardAndBoardEngine.getMovableTokens(ludo.playerTokens, rolled)

            if (movable.isEmpty()) {
                soundEngine.playErrorTone()
                _uiState.update { state ->
                    state.copy(
                        ludoState = state.ludoState.copy(
                            diceValue = rolled,
                            hasRolled = true,
                            movableTokenIndices = emptyList(),
                            statusText = "Rolled $rolled — No legal move. Bot's turn!"
                        )
                    )
                }
                delay(750L)
                passTurnToLudoBot()
            } else if (movable.size == 1) {
                _uiState.update { state ->
                    state.copy(
                        ludoState = state.ludoState.copy(
                            diceValue = rolled,
                            hasRolled = true,
                            movableTokenIndices = movable,
                            statusText = "Rolled $rolled — Moving token..."
                        )
                    )
                }
                delay(280L)
                movePlayerLudoToken(movable.first())
            } else {
                soundEngine.playCoinTap()
                _uiState.update { state ->
                    state.copy(
                        ludoState = state.ludoState.copy(
                            diceValue = rolled,
                            hasRolled = true,
                            movableTokenIndices = movable,
                            statusText = "Rolled $rolled! Tap a glowing Emerald token."
                        )
                    )
                }
            }
        }
    }

    fun movePlayerLudoToken(tokenIndex: Int) {
        val ludo = _uiState.value.ludoState
        if (!ludo.isActive || !ludo.isPlayerTurn || !ludo.hasRolled) return
        if (tokenIndex !in ludo.movableTokenIndices) return

        val currentPos = ludo.playerTokens[tokenIndex]
        val nextPos = if (currentPos == -1) 0 else currentPos + ludo.diceValue
        val updatedPlayer = ludo.playerTokens.toMutableList().apply { this[tokenIndex] = nextPos }
        val updatedBot = ludo.botTokens.toMutableList()

        var capturedBot = false
        val landedGlobal = CardAndBoardEngine.playerGlobalCell(nextPos)
        if (landedGlobal != null && landedGlobal !in CardAndBoardEngine.SAFE_GLOBAL_CELLS) {
            for (bIdx in updatedBot.indices) {
                val botGlobal = CardAndBoardEngine.botGlobalCell(updatedBot[bIdx])
                if (botGlobal == landedGlobal) {
                    updatedBot[bIdx] = -1
                    capturedBot = true
                }
            }
        }

        val playerHomeCount = updatedPlayer.count { it == 56 }
        if (playerHomeCount >= 2) {
            val winCoins = if (ludo.isFreePlay) 250L else ludo.stakeCoins * 2L
            viewModelScope.launch {
                val split = allocateWinWithAdminFee(winCoins, "Ludo")
                repository.addCoins(split.netPlayerCoins)
                syncCloudSnapshotImmediate()
                soundEngine.playWinFanfare(isJackpot = true)
                _uiState.update { state ->
                    state.copy(
                        ludoState = ludo.copy(
                            isActive = false,
                            playerTokens = updatedPlayer,
                            botTokens = updatedBot,
                            movableTokenIndices = emptyList(),
                            winnerLabel = "YOU WON +${split.netPlayerCoins} COINS!",
                            statusText = "Victory! 2 Emerald tokens Home (1% Admin fee: ${split.adminFeeCoins})."
                        ),
                        confettiTriggerId = System.currentTimeMillis()
                    )
                }
                showToast(
                    title = "Ludo Champion! +${split.netPlayerCoins} Coins",
                    subtitle = "1% Admin fee (${split.adminFeeCoins}) • You beat the Neon Bot!",
                    type = ToastType.JACKPOT
                )
            }
            return
        }

        val extraTurn = ludo.diceValue == 6 || capturedBot || nextPos == 56
        if (capturedBot || nextPos == 56) {
            soundEngine.playWinFanfare(isJackpot = false)
        } else {
            soundEngine.playCoinTap()
        }

        if (extraTurn) {
            _uiState.update { state ->
                state.copy(
                    ludoState = ludo.copy(
                        playerTokens = updatedPlayer,
                        botTokens = updatedBot,
                        hasRolled = false,
                        movableTokenIndices = emptyList(),
                        statusText = when {
                            capturedBot -> "Captured Gold token! Bonus roll!"
                            nextPos == 56 -> "Token reached Home! Bonus roll!"
                            else -> "Rolled 6! Roll again."
                        }
                    )
                )
            }
        } else {
            _uiState.update { state ->
                state.copy(
                    ludoState = ludo.copy(
                        playerTokens = updatedPlayer,
                        botTokens = updatedBot,
                        isPlayerTurn = false,
                        hasRolled = false,
                        movableTokenIndices = emptyList(),
                        statusText = "Bot's turn..."
                    )
                )
            }
            passTurnToLudoBot()
        }
    }

    private fun passTurnToLudoBot() {
        viewModelScope.launch {
            var keepGoing = true
            while (keepGoing) {
                val current = _uiState.value.ludoState
                if (!current.isActive) return@launch
                _uiState.update { it.copy(ludoState = current.copy(isPlayerTurn = false, isBotThinking = true, hasRolled = false)) }
                delay(550L)
                val botDice = Random.nextInt(1, 7)
                soundEngine.playWheelTick()
                val latest = _uiState.value.ludoState
                val movable = CardAndBoardEngine.getMovableTokens(latest.botTokens, botDice)
                _uiState.update { it.copy(ludoState = latest.copy(diceValue = botDice, statusText = "Bot rolled $botDice")) }
                delay(450L)

                if (movable.isEmpty()) {
                    keepGoing = false
                } else {
                    // Smart Bot: prioritize capture, then reaching Home, then advancing furthest token
                    val chosenIdx = movable.maxByOrNull { idx ->
                        val cur = latest.botTokens[idx]
                        val next = if (cur == -1) 0 else cur + botDice
                        val botGlob = CardAndBoardEngine.botGlobalCell(next)
                        val captures = botGlob != null && botGlob !in CardAndBoardEngine.SAFE_GLOBAL_CELLS &&
                            latest.playerTokens.any { CardAndBoardEngine.playerGlobalCell(it) == botGlob }
                        when {
                            captures -> 1000
                            next == 56 -> 900
                            cur == -1 -> 500
                            else -> next
                        }
                    } ?: movable.first()

                    val curPos = latest.botTokens[chosenIdx]
                    val nextPos = if (curPos == -1) 0 else curPos + botDice
                    val updatedBot = latest.botTokens.toMutableList().apply { this[chosenIdx] = nextPos }
                    val updatedPlayer = latest.playerTokens.toMutableList()
                    var capturedPlayer = false
                    val botGlobal = CardAndBoardEngine.botGlobalCell(nextPos)
                    if (botGlobal != null && botGlobal !in CardAndBoardEngine.SAFE_GLOBAL_CELLS) {
                        for (pIdx in updatedPlayer.indices) {
                            if (CardAndBoardEngine.playerGlobalCell(updatedPlayer[pIdx]) == botGlobal) {
                                updatedPlayer[pIdx] = -1
                                capturedPlayer = true
                            }
                        }
                    }

                    val botHomeCount = updatedBot.count { it == 56 }
                    if (botHomeCount >= 2) {
                        soundEngine.playErrorTone()
                        _uiState.update { state ->
                            state.copy(
                                ludoState = latest.copy(
                                    isActive = false,
                                    isBotThinking = false,
                                    playerTokens = updatedPlayer,
                                    botTokens = updatedBot,
                                    winnerLabel = "BOT WON THIS MATCH",
                                    statusText = "Gold Bot got 2 tokens Home. Rematch anytime!"
                                )
                            )
                        }
                        return@launch
                    }

                    _uiState.update { state ->
                        state.copy(
                            ludoState = latest.copy(
                                playerTokens = updatedPlayer,
                                botTokens = updatedBot,
                                statusText = if (capturedPlayer) "Bot captured your token!" else "Bot moved token"
                            )
                        )
                    }
                    keepGoing = (botDice == 6 || capturedPlayer || nextPos == 56)
                }
            }

            val endLudo = _uiState.value.ludoState
            if (endLudo.isActive) {
                _uiState.update {
                    it.copy(
                        ludoState = endLudo.copy(
                            isPlayerTurn = true,
                            isBotThinking = false,
                            hasRolled = false,
                            movableTokenIndices = emptyList(),
                            statusText = "Your turn! Tap Roll Dice."
                        )
                    )
                }
            }
        }
    }

    // 5. Fruit Slots
    fun spinFruitSlots(stakeCoins: Long, isFreePlay: Boolean) {
        if (_uiState.value.fruitState.isSpinning) return

        viewModelScope.launch {
            if (!isFreePlay) {
                val ok = repository.tryDeductCoins(stakeCoins)
                if (!ok) {
                    soundEngine.playErrorTone()
                    showToast(
                        title = "Insufficient Coins",
                        subtitle = "Switch to Free Spin or lower your Fruit Slot stake.",
                        type = ToastType.ERROR
                    )
                    return@launch
                }
            }

            _uiState.update {
                it.copy(fruitState = it.fruitState.copy(isSpinning = true, lastWinCoins = null, statusText = "Spinning Fruit Reels…"))
            }

            val symbols = FruitSymbol.entries
            repeat(9) {
                delay(85L)
                soundEngine.playWheelTick()
                _uiState.update { state ->
                    state.copy(
                        fruitState = state.fruitState.copy(
                            reels = List(3) { symbols.random() }
                        )
                    )
                }
            }

            val finalReels = List(3) { symbols.random() }
            val payout = when {
                finalReels[0] == finalReels[1] && finalReels[1] == finalReels[2] -> {
                    val mult = finalReels[0].payoutMultiplier
                    if (isFreePlay) (25.0 * mult).roundToLong() else (stakeCoins * mult).roundToLong()
                }
                finalReels[0] == finalReels[1] || finalReels[1] == finalReels[2] || finalReels[0] == finalReels[2] -> {
                    if (isFreePlay) 40L else (stakeCoins * 1.4).roundToLong()
                }
                else -> 0L
            }

            if (payout > 0L) {
                val split = allocateWinWithAdminFee(payout, "Fruit Slots")
                repository.addCoins(split.netPlayerCoins)
                syncCloudSnapshotImmediate()
                val isJackpot = finalReels.distinct().size == 1
                soundEngine.playWinFanfare(isJackpot = isJackpot)
                _uiState.update {
                    it.copy(
                        fruitState = FruitSlotsState(
                            reels = finalReels,
                            isSpinning = false,
                            statusText = if (isJackpot) "TRIPLE ${finalReels[0].emoji} JACKPOT! +${split.netPlayerCoins}" else "Pair Match! +${split.netPlayerCoins} Coins",
                            lastWinCoins = split.netPlayerCoins
                        ),
                        confettiTriggerId = if (isJackpot || payout >= 500L) System.currentTimeMillis() else it.confettiTriggerId
                    )
                }
                showToast(
                    title = if (isJackpot) "Fruit Jackpot! +${split.netPlayerCoins} Coins" else "Fruit Match! +${split.netPlayerCoins} Coins",
                    subtitle = "${finalReels[0].emoji} ${finalReels[1].emoji} ${finalReels[2].emoji} • 1% Admin fee: ${split.adminFeeCoins}",
                    type = if (isJackpot) ToastType.JACKPOT else ToastType.SUCCESS
                )
            } else {
                soundEngine.playErrorTone()
                _uiState.update {
                    it.copy(
                        fruitState = FruitSlotsState(
                            reels = finalReels,
                            isSpinning = false,
                            statusText = "No fruit match — spin again!",
                            lastWinCoins = 0L
                        )
                    )
                }
            }
        }
    }

    // 6. Teen Patti (3-Card Showdown)
    fun playTeenPattiRound(stakeCoins: Long, isFreePlay: Boolean) {
        if (_uiState.value.teenPattiState.isDealing) return

        viewModelScope.launch {
            if (!isFreePlay) {
                val ok = repository.tryDeductCoins(stakeCoins)
                if (!ok) {
                    soundEngine.playErrorTone()
                    showToast("Insufficient Coins", "Try Free Deal or pick a lower stake.", ToastType.ERROR)
                    return@launch
                }
            }

            _uiState.update {
                it.copy(teenPattiState = it.teenPattiState.copy(isDealing = true, outcomeText = "Shuffling & Dealing 3 Cards…"))
            }
            repeat(3) {
                soundEngine.playWheelTick()
                delay(120L)
            }

            val deck = CardAndBoardEngine.createShuffledDeck()
            val player = deck.subList(0, 3)
            val dealer = deck.subList(3, 6)
            val evalPlayer = CardAndBoardEngine.evaluateTeenPattiHand(player)
            val evalDealer = CardAndBoardEngine.evaluateTeenPattiHand(dealer)
            val cmp = evalPlayer.compareTo(evalDealer)

            val payout = when {
                cmp > 0 -> if (isFreePlay) 120L else (stakeCoins * 2.0).roundToLong()
                cmp == 0 -> if (isFreePlay) 40L else stakeCoins
                else -> 0L
            }

            if (payout > 0L) {
                val netCredited = if (cmp > 0) {
                    allocateWinWithAdminFee(payout, "Teen Patti").netPlayerCoins
                } else {
                    payout
                }
                repository.addCoins(netCredited)
                syncCloudSnapshotImmediate()
                val big = evalPlayer.handRank.rankWeight >= 4 || payout >= 500L
                soundEngine.playWinFanfare(isJackpot = big)
                _uiState.update {
                    it.copy(
                        teenPattiState = TeenPattiState(
                            isDealing = false,
                            playerCards = player,
                            dealerCards = dealer,
                            playerHandName = evalPlayer.handRank.displayName,
                            dealerHandName = evalDealer.handRank.displayName,
                            outcomeText = if (cmp > 0) "You Won +$netCredited Coins with ${evalPlayer.handRank.displayName}!" else "Tie! Stake returned (+$netCredited)",
                            lastWinCoins = netCredited
                        ),
                        confettiTriggerId = if (big) System.currentTimeMillis() else it.confettiTriggerId
                    )
                }
            } else {
                soundEngine.playErrorTone()
                _uiState.update {
                    it.copy(
                        teenPattiState = TeenPattiState(
                            isDealing = false,
                            playerCards = player,
                            dealerCards = dealer,
                            playerHandName = evalPlayer.handRank.displayName,
                            dealerHandName = evalDealer.handRank.displayName,
                            outcomeText = "Dealer won with ${evalDealer.handRank.displayName}",
                            lastWinCoins = 0L
                        )
                    )
                }
            }
        }
    }

    // 7. Andar Bahar
    fun playAndarBaharRound(side: AndarBaharSide, stakeCoins: Long, isFreePlay: Boolean) {
        if (_uiState.value.andarBaharState.isPlaying) return

        viewModelScope.launch {
            if (!isFreePlay) {
                val ok = repository.tryDeductCoins(stakeCoins)
                if (!ok) {
                    soundEngine.playErrorTone()
                    showToast("Insufficient Coins", "Try Free Play or lower your stake.", ToastType.ERROR)
                    return@launch
                }
            }

            val deck = CardAndBoardEngine.createShuffledDeck()
            val joker = deck.first()
            val andar = mutableListOf<PlayingCard>()
            val bahar = mutableListOf<PlayingCard>()

            _uiState.update {
                it.copy(
                    andarBaharState = AndarBaharState(
                        isPlaying = true,
                        jokerCard = joker,
                        andarCards = emptyList(),
                        baharCards = emptyList(),
                        chosenSide = side,
                        winningSide = null,
                        statusText = "Joker is ${joker.rank.label}${joker.suit.symbol}! Dealing…"
                    )
                )
            }

            var winner: AndarBaharSide = AndarBaharSide.ANDAR
            for (i in 1 until deck.size) {
                delay(140L)
                soundEngine.playWheelTick()
                val card = deck[i]
                val dealToAndar = (i % 2 == 1)
                if (dealToAndar) andar.add(card) else bahar.add(card)

                _uiState.update { state ->
                    state.copy(
                        andarBaharState = state.andarBaharState.copy(
                            andarCards = andar.toList(),
                            baharCards = bahar.toList()
                        )
                    )
                }
                if (card.rank == joker.rank) {
                    winner = if (dealToAndar) AndarBaharSide.ANDAR else AndarBaharSide.BAHAR
                    break
                }
            }

            val won = winner == side
            val payout = if (!won) 0L else if (isFreePlay) 110L else (stakeCoins * 1.95).roundToLong()

            if (payout > 0L) {
                val split = allocateWinWithAdminFee(payout, "Andar Bahar")
                repository.addCoins(split.netPlayerCoins)
                syncCloudSnapshotImmediate()
                soundEngine.playWinFanfare(isJackpot = payout >= 500L)
                _uiState.update {
                    it.copy(
                        andarBaharState = it.andarBaharState.copy(
                            isPlaying = false,
                            winningSide = winner,
                            statusText = "${winner.label} matched ${joker.rank.label}! Won +${split.netPlayerCoins} Coins",
                            lastWinCoins = split.netPlayerCoins
                        ),
                        confettiTriggerId = if (payout >= 500L) System.currentTimeMillis() else it.confettiTriggerId
                    )
                }
            } else {
                soundEngine.playErrorTone()
                _uiState.update {
                    it.copy(
                        andarBaharState = it.andarBaharState.copy(
                            isPlaying = false,
                            winningSide = winner,
                            statusText = "Matched on ${winner.label}. Better luck next deal!",
                            lastWinCoins = 0L
                        )
                    )
                }
            }
        }
    }

    // 8. Blackjack 21
    fun startBlackjack(stakeCoins: Long, isFreePlay: Boolean) {
        if (_uiState.value.blackjackState.isActive) return

        viewModelScope.launch {
            if (!isFreePlay) {
                val ok = repository.tryDeductCoins(stakeCoins)
                if (!ok) {
                    soundEngine.playErrorTone()
                    showToast("Insufficient Coins", "Use Free Deal or pick a lower stake.", ToastType.ERROR)
                    return@launch
                }
            }

            val deck = CardAndBoardEngine.createShuffledDeck()
            val player = listOf(deck[0], deck[2])
            val dealer = listOf(deck[1], deck[3])
            val remaining = deck.drop(4)
            val playerScore = CardAndBoardEngine.calculateBlackjackScore(player)
            soundEngine.playCoinTap()

            if (playerScore == 21) {
                val bjWin = if (isFreePlay) 150L else (stakeCoins * 2.5).roundToLong()
                val split = allocateWinWithAdminFee(bjWin, "Blackjack 21")
                repository.addCoins(split.netPlayerCoins)
                syncCloudSnapshotImmediate()
                soundEngine.playWinFanfare(isJackpot = true)
                _uiState.update {
                    it.copy(
                        blackjackState = BlackjackState(
                            isActive = false,
                            isFinished = true,
                            stakeCoins = if (isFreePlay) 0L else stakeCoins,
                            isFreePlay = isFreePlay,
                            deck = remaining,
                            playerCards = player,
                            dealerCards = dealer,
                            hideDealerHoleCard = false,
                            statusText = "NATURAL BLACKJACK 21! +${split.netPlayerCoins} Coins!",
                            lastWinCoins = split.netPlayerCoins
                        ),
                        confettiTriggerId = System.currentTimeMillis()
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        blackjackState = BlackjackState(
                            isActive = true,
                            isFinished = false,
                            stakeCoins = if (isFreePlay) 0L else stakeCoins,
                            isFreePlay = isFreePlay,
                            deck = remaining,
                            playerCards = player,
                            dealerCards = dealer,
                            hideDealerHoleCard = true,
                            statusText = "Your Score: $playerScore — Hit or Stand?",
                            lastWinCoins = null
                        )
                    )
                }
            }
        }
    }

    fun hitBlackjack() {
        val bj = _uiState.value.blackjackState
        if (!bj.isActive || bj.deck.isEmpty()) return
        val nextCard = bj.deck.first()
        val updatedPlayer = bj.playerCards + nextCard
        val updatedDeck = bj.deck.drop(1)
        val score = CardAndBoardEngine.calculateBlackjackScore(updatedPlayer)

        if (score > 21) {
            soundEngine.playErrorTone()
            _uiState.update {
                it.copy(
                    blackjackState = bj.copy(
                        isActive = false,
                        isFinished = true,
                        deck = updatedDeck,
                        playerCards = updatedPlayer,
                        hideDealerHoleCard = false,
                        statusText = "Bust ($score)! Dealer wins.",
                        lastWinCoins = 0L
                    )
                )
            }
        } else {
            soundEngine.playCoinTap()
            _uiState.update {
                it.copy(
                    blackjackState = bj.copy(
                        deck = updatedDeck,
                        playerCards = updatedPlayer,
                        statusText = "Your Score: $score — Hit or Stand?"
                    )
                )
            }
            if (score == 21) {
                standBlackjack()
            }
        }
    }

    fun standBlackjack() {
        val bj = _uiState.value.blackjackState
        if (!bj.isActive) return

        viewModelScope.launch {
            var dealerHand = bj.dealerCards
            var deck = bj.deck
            while (CardAndBoardEngine.calculateBlackjackScore(dealerHand) < 17 && deck.isNotEmpty()) {
                dealerHand = dealerHand + deck.first()
                deck = deck.drop(1)
            }
            val pScore = CardAndBoardEngine.calculateBlackjackScore(bj.playerCards)
            val dScore = CardAndBoardEngine.calculateBlackjackScore(dealerHand)

            val payout = when {
                dScore > 21 || pScore > dScore -> if (bj.isFreePlay) 100L else (bj.stakeCoins * 2.0).roundToLong()
                pScore == dScore -> if (bj.isFreePlay) 30L else bj.stakeCoins
                else -> 0L
            }

            if (payout > 0L) {
                val isPush = pScore == dScore && dScore <= 21
                val netCredited = if (!isPush) {
                    allocateWinWithAdminFee(payout, "Blackjack 21").netPlayerCoins
                } else {
                    payout
                }
                repository.addCoins(netCredited)
                syncCloudSnapshotImmediate()
                soundEngine.playWinFanfare(isJackpot = payout >= 500L)
                _uiState.update {
                    it.copy(
                        blackjackState = bj.copy(
                            isActive = false,
                            isFinished = true,
                            deck = deck,
                            dealerCards = dealerHand,
                            hideDealerHoleCard = false,
                            statusText = if (isPush) "Push ($pScore vs $dScore) • +$netCredited" else "You Beat Dealer ($pScore vs $dScore)! +$netCredited",
                            lastWinCoins = netCredited
                        ),
                        confettiTriggerId = if (payout >= 500L) System.currentTimeMillis() else it.confettiTriggerId
                    )
                }
            } else {
                soundEngine.playErrorTone()
                _uiState.update {
                    it.copy(
                        blackjackState = bj.copy(
                            isActive = false,
                            isFinished = true,
                            deck = deck,
                            dealerCards = dealerHand,
                            hideDealerHoleCard = false,
                            statusText = "Dealer Won ($dScore vs $pScore).",
                            lastWinCoins = 0L
                        )
                    )
                }
            }
        }
    }

    private fun allocateWinWithAdminFee(
        grossWinCoins: Long,
        sourceGame: String
    ): WinCommissionSplit {
        val split = splitWinWithOnePercentAdminFee(grossWinCoins, pendingCommissionPool)
        pendingCommissionPool = split.nextPendingPool
        if (split.adminFeeCoins > 0L) {
            firestoreRepo?.let { repo ->
                viewModelScope.launch {
                    repo.recordAdminCommission(
                        sourceGame = sourceGame,
                        grossWinCoins = grossWinCoins,
                        commissionCoins = split.adminFeeCoins,
                        netPlayerCoins = split.netPlayerCoins,
                        playerName = userDisplayName
                    )
                }
            }
        }
        return split
    }

    data class WinCommissionSplit(
        val netPlayerCoins: Long,
        val adminFeeCoins: Long,
        val nextPendingPool: Long
    )

    companion object {
        val PLINKO_MULTIPLIERS = listOf(5.0, 2.2, 1.3, 0.6, 1.3, 2.2, 5.0)
        val PLINKO_FREE_REWARDS = listOf(250L, 120L, 60L, 25L, 60L, 120L, 250L)

        fun computeMinesMultiplier(gemsFound: Int): Double {
            if (gemsFound <= 0) return 1.0
            return 1.0 + (gemsFound * 0.22) + (gemsFound * gemsFound * 0.04)
        }

        fun splitWinWithOnePercentAdminFee(
            grossWinCoins: Long,
            pendingPool: Long = 0L
        ): WinCommissionSplit {
            if (grossWinCoins <= 0L) {
                return WinCommissionSplit(
                    netPlayerCoins = 0L,
                    adminFeeCoins = 0L,
                    nextPendingPool = pendingPool.coerceAtLeast(0L)
                )
            }
            val combined = pendingPool.coerceAtLeast(0L) + grossWinCoins
            val fee = (combined / 100L).coerceAtMost(grossWinCoins)
            val nextPool = combined % 100L
            val net = (grossWinCoins - fee).coerceAtLeast(0L)
            return WinCommissionSplit(
                netPlayerCoins = net,
                adminFeeCoins = fee,
                nextPendingPool = nextPool
            )
        }
    }

    private fun validatePaymentIdentifier(method: PaymentMethod, input: String): String? {
        if (input.isBlank()) return "Please enter your ${method.accountLabel}."
        return when (method) {
            PaymentMethod.UPI -> if (!input.contains("@") || input.length < 5) {
                "Enter a valid UPI ID (e.g. player@okaxis)."
            } else null
            PaymentMethod.PAYTM -> if (input.filter { it.isDigit() }.length < 10) {
                "Enter a valid 10-digit Paytm mobile number."
            } else null
            PaymentMethod.PAYPAL -> if (!input.contains("@") || !input.contains(".")) {
                "Enter a valid PayPal email address."
            } else null
            PaymentMethod.USDT -> if (input.length < 16) {
                "Enter a valid USDT TRC20/BEP20 wallet address."
            } else null
        }
    }

    private fun showToast(title: String, subtitle: String, type: ToastType) {
        val nextId = ++toastCounter
        _uiState.update {
            it.copy(activeToast = ToastMessage(nextId, title, subtitle, type))
        }
        viewModelScope.launch {
            delay(3200L)
            _uiState.update { state ->
                if (state.activeToast?.id == nextId) state.copy(activeToast = null) else state
            }
        }
    }

    private fun pickWeightedSegment(): WheelSegment {
        val totalWeight = DEFAULT_WHEEL_SEGMENTS.sumOf { it.weight }
        var r = Random.nextInt(totalWeight)
        for (segment in DEFAULT_WHEEL_SEGMENTS) {
            r -= segment.weight
            if (r < 0) return segment
        }
        return DEFAULT_WHEEL_SEGMENTS.first()
    }

    private fun randomScratchPrize(): Long {
        val roll = Random.nextInt(100)
        return when {
            roll < 35 -> 75L
            roll < 65 -> 150L
            roll < 85 -> 250L
            roll < 95 -> 500L
            else -> 1000L
        }
    }

    private fun computeEffectiveStreak(
        savedStreak: Int,
        lastCheckinDay: Long,
        today: Long
    ): Int {
        if (lastCheckinDay == 0L) return 0
        val diff = today - lastCheckinDay
        return when {
            diff <= 1L -> if (savedStreak >= 7 && diff == 1L) 0 else savedStreak
            else -> 0 // streak expired
        }
    }

    private fun computeMidnightCountdown(): String {
        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffSec = ((midnight.timeInMillis - now.timeInMillis) / 1000L).coerceAtLeast(0L)
        val hours = diffSec / 3600L
        val mins = (diffSec % 3600L) / 60L
        val secs = diffSec % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
    }

    private fun syncCloudSnapshotImmediate() {
        val repo = firestoreRepo ?: return
        viewModelScope.launch {
            val snap = repository.currentSnapshot()
            repo.syncWalletSnapshot(snap, userDisplayName, userEmail, userPhotoUrl)
        }
    }

    private fun scheduleTapCloudSync() {
        val repo = firestoreRepo ?: return
        tapSyncJob?.cancel()
        tapSyncJob = viewModelScope.launch {
            delay(1200L)
            val snap = repository.currentSnapshot()
            repo.syncWalletSnapshot(snap, userDisplayName, userEmail, userPhotoUrl)
        }
    }

    private fun mergeTransactions(
        cloud: List<WithdrawalTransaction>,
        local: List<WithdrawalTransaction>
    ): List<WithdrawalTransaction> {
        val byRef = LinkedHashMap<String, WithdrawalTransaction>()
        cloud.forEach { tx -> byRef[tx.referenceCode.ifBlank { tx.id }] = tx }
        local.forEach { tx ->
            val key = tx.referenceCode.ifBlank { tx.id }
            if (!byRef.containsKey(key)) {
                byRef[key] = tx
            }
        }
        return byRef.values.sortedByDescending { it.timestampMillis }
    }

    override fun onCleared() {
        super.onCleared()
        soundEngine.release()
    }
}
