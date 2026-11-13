package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CardSlateElevated
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonMagenta
import com.example.data.AdminCommissionRecord
import com.example.ui.theme.RoseAlert
import com.squareup.moshi.JsonClass

enum class EarningMode(val title: String) {
    SPIN("Spin"),
    TAP("Tap"),
    GAMES("Games"),
    SCRATCH("Scratch"),
    DAILY("Streak")
}

enum class ArcadeGameType(val title: String, val badge: String) {
    LUDO("Ludo", "BOARD"),
    FRUIT("Fruit Slots", "SLOTS"),
    TEEN_PATTI("Teen Patti", "CARDS"),
    ANDAR_BAHAR("Andar Bahar", "CARDS"),
    BLACKJACK("Blackjack 21", "CARDS"),
    MINES("Gem Mines", "VAULT"),
    PLINKO("Plinko Drop", "DROP"),
    DICE("Lucky Dice 7", "DICE")
}

// --- Card Games Models ---

enum class CardSuit(val symbol: String, val isRed: Boolean) {
    SPADES("♠", false),
    HEARTS("♥", true),
    DIAMONDS("♦", true),
    CLUBS("♣", false)
}

enum class CardRank(val label: String, val rankValue: Int, val bjValue: Int) {
    TWO("2", 2, 2),
    THREE("3", 3, 3),
    FOUR("4", 4, 4),
    FIVE("5", 5, 5),
    SIX("6", 6, 6),
    SEVEN("7", 7, 7),
    EIGHT("8", 8, 8),
    NINE("9", 9, 9),
    TEN("10", 10, 10),
    JACK("J", 11, 10),
    QUEEN("Q", 12, 10),
    KING("K", 13, 10),
    ACE("A", 14, 11)
}

data class PlayingCard(
    val suit: CardSuit,
    val rank: CardRank
)

enum class TeenPattiHandRank(val displayName: String, val rankWeight: Int) {
    HIGH_CARD("High Card", 1),
    PAIR("Pair", 2),
    COLOR("Color (Flush)", 3),
    SEQUENCE("Sequence", 4),
    PURE_SEQUENCE("Pure Sequence", 5),
    TRAIL("Trail (Trio)", 6)
}

data class TeenPattiState(
    val isDealing: Boolean = false,
    val playerCards: List<PlayingCard> = emptyList(),
    val dealerCards: List<PlayingCard> = emptyList(),
    val playerHandName: String = "",
    val dealerHandName: String = "",
    val outcomeText: String = "Deal 3 cards to challenge the Neon Dealer!",
    val lastWinCoins: Long? = null
)

enum class AndarBaharSide(val label: String) {
    ANDAR("ANDAR"),
    BAHAR("BAHAR")
}

data class AndarBaharState(
    val isPlaying: Boolean = false,
    val jokerCard: PlayingCard? = null,
    val andarCards: List<PlayingCard> = emptyList(),
    val baharCards: List<PlayingCard> = emptyList(),
    val chosenSide: AndarBaharSide = AndarBaharSide.ANDAR,
    val winningSide: AndarBaharSide? = null,
    val statusText: String = "Pick Andar or Bahar, then deal until Joker rank matches!",
    val lastWinCoins: Long? = null
)

data class BlackjackState(
    val isActive: Boolean = false,
    val isFinished: Boolean = false,
    val stakeCoins: Long = 0L,
    val isFreePlay: Boolean = true,
    val deck: List<PlayingCard> = emptyList(),
    val playerCards: List<PlayingCard> = emptyList(),
    val dealerCards: List<PlayingCard> = emptyList(),
    val hideDealerHoleCard: Boolean = true,
    val statusText: String = "Hit 21 or beat the Dealer's hand!",
    val lastWinCoins: Long? = null
)

// --- Ludo Game Models ---
// Token progress: -1 = in Home Base, 0..50 = steps along 52-cell main track, 51..55 = home stretch, 56 = Finished Home!
data class LudoGameState(
    val isActive: Boolean = false,
    val isPlayerTurn: Boolean = true,
    val isBotThinking: Boolean = false,
    val diceValue: Int = 6,
    val hasRolled: Boolean = false,
    val playerTokens: List<Int> = listOf(-1, -1, -1, -1),
    val botTokens: List<Int> = listOf(-1, -1, -1, -1),
    val movableTokenIndices: List<Int> = emptyList(),
    val stakeCoins: Long = 0L,
    val isFreePlay: Boolean = true,
    val winnerLabel: String? = null,
    val statusText: String = "Start a quick Ludo match (First 2 tokens Home wins!)"
)

// --- Fruit Slots Models ---
enum class FruitSymbol(val emoji: String, val label: String, val payoutMultiplier: Double) {
    CHERRY("🍒", "Cherry", 2.0),
    LEMON("🍋", "Lemon", 2.5),
    GRAPES("🍇", "Grapes", 3.5),
    WATERMELON("🍉", "Melon", 5.0),
    DIAMOND("💎", "Diamond", 8.0),
    SEVEN("7️⃣", "Lucky 7", 15.0)
}

data class FruitSlotsState(
    val reels: List<FruitSymbol> = listOf(FruitSymbol.SEVEN, FruitSymbol.DIAMOND, FruitSymbol.SEVEN),
    val isSpinning: Boolean = false,
    val statusText: String = "Match 2 or 3 fruits to win Mint Coins!",
    val lastWinCoins: Long? = null
)

// --- Existing Arcade Games Models ---

enum class DiceChoice(val label: String, val multiplier: Double) {
    UNDER_7("UNDER 7", 2.2),
    EXACT_7("EXACT 7", 4.5),
    OVER_7("OVER 7", 2.2)
}

enum class MineTileState {
    HIDDEN,
    GEM,
    MINE
}

data class MinesGameState(
    val isActive: Boolean = false,
    val isRoundOver: Boolean = false,
    val hitMine: Boolean = false,
    val stakeCoins: Long = 0L,
    val isFreePlay: Boolean = false,
    val tiles: List<MineTileState> = List(25) { MineTileState.HIDDEN },
    val mineIndices: Set<Int> = emptySet(),
    val gemsFound: Int = 0,
    val currentMultiplier: Double = 1.0,
    val potentialWinCoins: Long = 0L
)

data class DiceGameState(
    val dieOne: Int = 4,
    val dieTwo: Int = 3,
    val isRolling: Boolean = false,
    val lastWinCoins: Long? = null,
    val lastResultSummary: String = "Pick Under 7, Exact 7, or Over 7 to roll!"
)

data class PlinkoDropPlan(
    val stakeCoins: Long,
    val isFreePlay: Boolean,
    val directions: List<Int>,
    val bucketIndex: Int,
    val multiplier: Double,
    val rewardCoins: Long
)

data class PlinkoGameState(
    val isDropping: Boolean = false,
    val lastBucketIndex: Int? = null,
    val lastWinCoins: Long? = null
)

// --- Core Wallet, Spin, Deposit, Withdraw, Security Models ---

data class WheelSegment(
    val id: Int,
    val label: String,
    val shortLabel: String,
    val coins: Long,
    val color: Color,
    val weight: Int,
    val isJackpot: Boolean = false,
    val isBetterLuck: Boolean = false
)

val DEFAULT_WHEEL_SEGMENTS = listOf(
    WheelSegment(0, "50 Coins", "50", 50, ElectricViolet, weight = 26),
    WheelSegment(1, "100 Coins", "100", 100, NeonCyan, weight = 22),
    WheelSegment(2, "Better Luck", "RETRY", 0, CardSlateElevated, weight = 14, isBetterLuck = true),
    WheelSegment(3, "500 Coins", "500", 500, EmeraldMint, weight = 10),
    WheelSegment(4, "150 Coins", "150", 150, NeonMagenta, weight = 16),
    WheelSegment(5, "Jackpot 1000", "1000★", 1000, NeonGold, weight = 4, isJackpot = true),
    WheelSegment(6, "200 Coins", "200", 200, AmberGold, weight = 12),
    WheelSegment(7, "75 Coins", "75", 75, RoseAlert, weight = 20)
)

data class DailyRewardDay(
    val dayNumber: Int,
    val coins: Long,
    val title: String,
    val isJackpotDay: Boolean = false
)

val DAILY_REWARD_DAYS = listOf(
    DailyRewardDay(1, 100, "Starter Boost"),
    DailyRewardDay(2, 200, "Momentum"),
    DailyRewardDay(3, 350, "Hot Streak"),
    DailyRewardDay(4, 500, "Silver Vault"),
    DailyRewardDay(5, 750, "Neon Surge"),
    DailyRewardDay(6, 1000, "Emerald Drop"),
    DailyRewardDay(7, 2500, "Mega Jackpot", isJackpotDay = true)
)

enum class PaymentMethod(
    val displayName: String,
    val accountLabel: String,
    val placeholder: String,
    val accentColor: Color
) {
    UPI("UPI", "UPI ID", "yourname@okaxis", EmeraldMint),
    PAYTM("Paytm", "Paytm Wallet Number", "9876543210", NeonCyan),
    PAYPAL("PayPal", "PayPal Email", "player@email.com", ElectricViolet),
    USDT("Crypto (USDT)", "USDT TRC20 / BEP20 Address", "TQn9Y2khEsLJW1ChVWFMSMeRDow5KcbLSE", NeonGold)
}

data class DepositPackage(
    val id: String,
    val inrAmount: Int,
    val baseCoins: Long,
    val bonusCoins: Long,
    val tag: String? = null
) {
    val totalCoins: Long get() = baseCoins + bonusCoins
}

val DEPOSIT_PACKAGES = listOf(
    DepositPackage("pkg_50", 50, 5_000L, 0L, null),
    DepositPackage("pkg_100", 100, 10_000L, 500L, "+5% BONUS"),
    DepositPackage("pkg_250", 250, 25_000L, 2_500L, "+10% POPULAR"),
    DepositPackage("pkg_500", 500, 50_000L, 10_000L, "+20% VIP")
)

@JsonClass(generateAdapter = true)
data class WithdrawalTransaction(
    val id: String,
    val referenceCode: String,
    val methodName: String,
    val accountIdentifier: String,
    val coinsSpent: Long,
    val inrEquivalent: Double,
    val usdEquivalent: Double,
    val timestampMillis: Long,
    val status: String = "Submitted",
    val txType: String = "WITHDRAWAL"
)

data class SecurityState(
    val hasPin: Boolean = false,
    val savedPin: String = "",
    val lockAppOnLaunch: Boolean = false,
    val requirePinForWallet: Boolean = true,
    val isAppLocked: Boolean = false
)

enum class ToastType {
    SUCCESS,
    INFO,
    ERROR,
    JACKPOT
}

data class ToastMessage(
    val id: Long,
    val title: String,
    val subtitle: String,
    val type: ToastType
)

data class GameUiState(
    val coins: Long = 0L,
    val freeSpinsLeft: Int = 5,
    val maxDailySpins: Int = 5,
    val adCooldownSeconds: Int = 0,
    val spinResetCountdownText: String = "23:59:59",
    val energy: Int = 100,
    val maxEnergy: Int = 100,
    val tapPower: Long = 10L,
    val streakDay: Int = 0,
    val canClaimDailyToday: Boolean = true,
    val isMuted: Boolean = false,
    val scratchCardPrize: Long = 150L,
    val scratchCardRevealed: Boolean = false,
    val scratchCardVersion: Int = 1,
    val isSpinning: Boolean = false,
    val isWatchingAd: Boolean = false,
    val adProgress: Float = 0f,
    val activeMode: EarningMode = EarningMode.SPIN,
    val transactions: List<WithdrawalTransaction> = emptyList(),
    val latestReceipt: WithdrawalTransaction? = null,
    val userId: String = "",
    val userDisplayName: String = "SpinMint Player",
    val userEmail: String? = null,
    val isAdmin: Boolean = false,
    val showWithdrawModal: Boolean = false,
    val showDepositModal: Boolean = false,
    val showSecurityModal: Boolean = false,
    val showHistorySheet: Boolean = false,
    val showAccountModal: Boolean = false,
    val showAdminModal: Boolean = false,
    val adminCommissions: List<AdminCommissionRecord> = emptyList(),
    val claimedAdminCoins: Long = 0L,
    val activeToast: ToastMessage? = null,
    val confettiTriggerId: Long = 0L,
    val totalTaps: Long = 0L,
    val totalSpins: Int = 0,
    val activeArcadeGame: ArcadeGameType = ArcadeGameType.LUDO,
    val ludoState: LudoGameState = LudoGameState(),
    val fruitState: FruitSlotsState = FruitSlotsState(),
    val teenPattiState: TeenPattiState = TeenPattiState(),
    val andarBaharState: AndarBaharState = AndarBaharState(),
    val blackjackState: BlackjackState = BlackjackState(),
    val minesState: MinesGameState = MinesGameState(),
    val diceState: DiceGameState = DiceGameState(),
    val plinkoState: PlinkoGameState = PlinkoGameState(),
    val securityState: SecurityState = SecurityState()
) {
    val inrBalance: Double get() = coins * 0.01
    val usdBalance: Double get() = coins * 0.0001
    val totalAdminCommissionCoins: Long get() = adminCommissions.sumOf { it.commissionCoins }
    val unclaimedAdminCoins: Long get() = (totalAdminCommissionCoins - claimedAdminCoins).coerceAtLeast(0L)

    companion object {
        const val MIN_WITHDRAW_COINS = 10_000L
    }
}
