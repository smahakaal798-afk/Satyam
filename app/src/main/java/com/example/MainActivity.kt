package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.auth.signOutFromGoogle
import com.example.data.FirestoreWalletRepository
import com.example.data.GameRepository
import com.example.data.SPINMINT_ADMIN_EMAIL
import com.example.model.EarningMode
import com.example.model.GameUiState
import com.example.ui.components.AccountLogoutModal
import com.example.ui.components.AdminDashboardModal
import com.example.ui.components.ArcadeHubScreen
import com.example.ui.components.ConfettiOverlay
import com.example.ui.components.CustomToastBanner
import com.example.ui.components.DailyStreakScreen
import com.example.ui.components.DepositModal
import com.example.ui.components.LuckySpinScreen
import com.example.ui.components.PinLockOverlay
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.ScratchCardScreen
import com.example.ui.components.SecurityVaultModal
import com.example.ui.components.SignInScreen
import com.example.ui.components.TapToEarnScreen
import com.example.ui.components.TransactionHistoryDialog
import com.example.ui.components.WalletTopBar
import com.example.ui.components.WithdrawModal
import com.example.ui.theme.CardSlate
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.NeonGold
import com.example.ui.theme.SpinMintTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VoidObsidian
import com.example.viewmodel.GameViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            SpinMintTheme {
                SpinMintAuthRoot()
            }
        }
    }
}

@Composable
fun SpinMintAuthRoot(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        SignInScreen(
            onAuthSuccess = {
                // Auth state listener updates currentUser automatically
            }
        )
    } else {
        val uid = user.uid
        val isAdmin = user.email?.equals(SPINMINT_ADMIN_EMAIL, ignoreCase = true) == true &&
            user.isEmailVerified
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val credentialManager = remember(context) { CredentialManager.create(context) }

        key(uid) {
            val viewModel: GameViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        GameViewModel(
                            repository = GameRepository(app, uid),
                            firestoreRepo = FirestoreWalletRepository(db, auth),
                            currentUserId = uid,
                            userDisplayName = user.displayName ?: "SpinMint Player",
                            userEmail = user.email,
                            userPhotoUrl = user.photoUrl?.toString(),
                            isAdmin = isAdmin
                        )
                    }
                }
            )
            SpinMintApp(
                viewModel = viewModel,
                onLogout = {
                    viewModel.closeAccountModal()
                    signOutFromGoogle(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                }
            )
        }
    }
}

private data class NavTabMeta(
    val mode: EarningMode,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val NAV_TABS = listOf(
    NavTabMeta(EarningMode.SPIN, Icons.Rounded.Casino, Icons.Outlined.Casino),
    NavTabMeta(EarningMode.TAP, Icons.Rounded.TouchApp, Icons.Outlined.TouchApp),
    NavTabMeta(EarningMode.GAMES, Icons.Rounded.SportsEsports, Icons.Outlined.SportsEsports),
    NavTabMeta(EarningMode.SCRATCH, Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome),
    NavTabMeta(EarningMode.DAILY, Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth)
)

@Composable
fun SpinMintApp(
    viewModel: GameViewModel,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = uiState.activeMode != EarningMode.SPIN) {
        viewModel.selectMode(EarningMode.SPIN)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(VoidObsidian, DeepIndigo, VoidObsidian)
                )
            )
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 600.dp

            if (isWideScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    NavigationRail(
                        containerColor = DeepIndigo,
                        contentColor = TextPrimary,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        NAV_TABS.forEach { tab ->
                            val selected = uiState.activeMode == tab.mode
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectMode(tab.mode) },
                                modifier = Modifier.testTag("nav_tab_${tab.mode.name.lowercase()}"),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.mode.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.mode.title,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = VoidObsidian,
                                    selectedTextColor = NeonGold,
                                    indicatorColor = NeonGold,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 680.dp)
                                .fillMaxSize()
                        ) {
                            WalletTopBar(
                                coins = uiState.coins,
                                inrBalance = uiState.inrBalance,
                                usdBalance = uiState.usdBalance,
                                isMuted = uiState.isMuted,
                                isSecurityPinActive = uiState.securityState.hasPin,
                                isAdmin = uiState.isAdmin,
                                userDisplayName = uiState.userDisplayName,
                                onToggleMute = viewModel::toggleMute,
                                onOpenSecurity = viewModel::openSecurityModal,
                                onOpenAdmin = viewModel::openAdminModal,
                                onOpenAccount = viewModel::openAccountModal,
                                onOpenDeposit = viewModel::openDepositModal,
                                onOpenWithdraw = viewModel::openWithdrawModal,
                                onOpenHistory = viewModel::openHistorySheet,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            EarningModeContent(
                                uiState = uiState,
                                viewModel = viewModel,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                Scaffold(
                    containerColor = VoidObsidian.copy(alpha = 0f),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        NavigationBar(
                            containerColor = CardSlate,
                            windowInsets = NavigationBarDefaults.windowInsets
                        ) {
                            NAV_TABS.forEach { tab ->
                                val selected = uiState.activeMode == tab.mode
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectMode(tab.mode) },
                                    modifier = Modifier.testTag("nav_tab_${tab.mode.name.lowercase()}"),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.mode.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.mode.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = VoidObsidian,
                                        selectedTextColor = NeonGold,
                                        indicatorColor = NeonGold,
                                        unselectedIconColor = TextMuted,
                                        unselectedTextColor = TextMuted
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .statusBarsPadding()
                    ) {
                        WalletTopBar(
                            coins = uiState.coins,
                            inrBalance = uiState.inrBalance,
                            usdBalance = uiState.usdBalance,
                            isMuted = uiState.isMuted,
                            isSecurityPinActive = uiState.securityState.hasPin,
                            isAdmin = uiState.isAdmin,
                            userDisplayName = uiState.userDisplayName,
                            onToggleMute = viewModel::toggleMute,
                            onOpenSecurity = viewModel::openSecurityModal,
                            onOpenAdmin = viewModel::openAdminModal,
                            onOpenAccount = viewModel::openAccountModal,
                            onOpenDeposit = viewModel::openDepositModal,
                            onOpenWithdraw = viewModel::openWithdrawModal,
                            onOpenHistory = viewModel::openHistorySheet,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        EarningModeContent(
                            uiState = uiState,
                            viewModel = viewModel,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Full-screen celebratory confetti overlay
        ConfettiOverlay(triggerId = uiState.confettiTriggerId)

        // Custom Floating Toast Notifications (replaces browser alert())
        CustomToastBanner(
            toast = uiState.activeToast,
            onDismiss = viewModel::dismissToast,
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 6.dp)
        )

        // Withdraw / Redeem Modal
        if (uiState.showWithdrawModal) {
            WithdrawModal(
                coins = uiState.coins,
                inrBalance = uiState.inrBalance,
                usdBalance = uiState.usdBalance,
                securityState = uiState.securityState,
                onDismiss = viewModel::closeWithdrawModal,
                onSubmit = viewModel::submitWithdrawal
            )
        }

        // Deposit / Add Coins Modal
        if (uiState.showDepositModal) {
            DepositModal(
                securityState = uiState.securityState,
                onDismiss = viewModel::closeDepositModal,
                onSubmitDeposit = viewModel::submitDeposit
            )
        }

        // Security Vault Modal
        if (uiState.showSecurityModal) {
            SecurityVaultModal(
                securityState = uiState.securityState,
                onDismiss = viewModel::closeSecurityModal,
                onSetPin = viewModel::setVaultPin,
                onDisablePin = viewModel::disableVaultPin,
                onToggleLaunchLock = viewModel::toggleLaunchLock,
                onToggleWalletGuard = viewModel::toggleWalletPinGuard,
                onLockNow = viewModel::lockVaultNow
            )
        }

        // Withdrawal / Deposit Receipt Dialog
        uiState.latestReceipt?.let { receipt ->
            ReceiptDialog(
                transaction = receipt,
                onDismiss = viewModel::dismissReceipt
            )
        }

        // Transaction History Modal
        if (uiState.showHistorySheet) {
            TransactionHistoryDialog(
                transactions = uiState.transactions,
                onDismiss = viewModel::closeHistorySheet
            )
        }

        // Account & Logout Modal
        if (uiState.showAccountModal) {
            AccountLogoutModal(
                displayName = uiState.userDisplayName,
                email = uiState.userEmail,
                coins = uiState.coins,
                isSecurityPinActive = uiState.securityState.hasPin,
                isAdmin = uiState.isAdmin,
                onDismiss = viewModel::closeAccountModal,
                onOpenAdmin = viewModel::openAdminModal,
                onOpenSecurity = viewModel::openSecurityModal,
                onLogoutConfirmed = onLogout
            )
        }

        // Admin 1% Revenue Vault Modal
        if (uiState.showAdminModal && uiState.isAdmin) {
            AdminDashboardModal(
                adminEmail = uiState.userEmail ?: SPINMINT_ADMIN_EMAIL,
                totalCommissionCoins = uiState.totalAdminCommissionCoins,
                unclaimedCoins = uiState.unclaimedAdminCoins,
                commissions = uiState.adminCommissions,
                onDismiss = viewModel::closeAdminModal,
                onClaimCommissions = viewModel::claimAdminCommissionsToWallet
            )
        }

        // Full-Screen App PIN Lock Gate
        if (uiState.securityState.isAppLocked) {
            PinLockOverlay(onUnlockAttempt = viewModel::unlockAppWithPin)
        }
    }
}

@Composable
private fun EarningModeContent(
    uiState: GameUiState,
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = uiState.activeMode,
        animationSpec = tween(220),
        modifier = modifier.fillMaxWidth(),
        label = "earning_mode_crossfade"
    ) { mode ->
        when (mode) {
            EarningMode.SPIN -> LuckySpinScreen(
                freeSpinsLeft = uiState.freeSpinsLeft,
                maxDailySpins = uiState.maxDailySpins,
                resetCountdown = uiState.spinResetCountdownText,
                adCooldownSeconds = uiState.adCooldownSeconds,
                isSpinning = uiState.isSpinning,
                isWatchingAd = uiState.isWatchingAd,
                adProgress = uiState.adProgress,
                onRequestSpin = viewModel::requestWheelSpin,
                onWheelTick = viewModel::onWheelTick,
                onSpinComplete = viewModel::onWheelSpinFinished,
                onWatchAd = viewModel::watchSimulatedAdForSpin
            )

            EarningMode.TAP -> TapToEarnScreen(
                energy = uiState.energy,
                maxEnergy = uiState.maxEnergy,
                tapPower = uiState.tapPower,
                totalTaps = uiState.totalTaps,
                onCoinTap = viewModel::onCoinTapped
            )

            EarningMode.GAMES -> ArcadeHubScreen(
                activeGame = uiState.activeArcadeGame,
                coins = uiState.coins,
                ludoState = uiState.ludoState,
                fruitState = uiState.fruitState,
                teenPattiState = uiState.teenPattiState,
                andarBaharState = uiState.andarBaharState,
                blackjackState = uiState.blackjackState,
                minesState = uiState.minesState,
                plinkoState = uiState.plinkoState,
                diceState = uiState.diceState,
                onSelectGame = viewModel::selectArcadeGame,
                onStartLudo = viewModel::startLudoMatch,
                onRollLudoDice = viewModel::rollLudoDice,
                onMoveLudoToken = viewModel::movePlayerLudoToken,
                onSpinFruits = viewModel::spinFruitSlots,
                onDealTeenPatti = viewModel::playTeenPattiRound,
                onPlayAndarBahar = viewModel::playAndarBaharRound,
                onStartBlackjack = viewModel::startBlackjack,
                onHitBlackjack = viewModel::hitBlackjack,
                onStandBlackjack = viewModel::standBlackjack,
                onStartMines = viewModel::startMinesGame,
                onRevealMineTile = viewModel::revealMineTile,
                onCashOutMines = viewModel::cashOutMines,
                onRequestPlinkoDrop = viewModel::requestPlinkoDrop,
                onPlinkoPegHit = viewModel::onPlinkoPegHit,
                onCompletePlinkoDrop = viewModel::completePlinkoDrop,
                onRollDice = viewModel::rollDice
            )

            EarningMode.SCRATCH -> ScratchCardScreen(
                prizeCoins = uiState.scratchCardPrize,
                isRevealed = uiState.scratchCardRevealed,
                cardVersion = uiState.scratchCardVersion,
                onScratchStroke = viewModel::onScratchStroke,
                onRevealReward = viewModel::revealScratchCardReward,
                onNewCard = viewModel::generateNewScratchCard
            )

            EarningMode.DAILY -> DailyStreakScreen(
                streakDay = uiState.streakDay,
                canClaimToday = uiState.canClaimDailyToday,
                onClaimBonus = viewModel::claimDailyCheckin
            )
        }
    }
}
