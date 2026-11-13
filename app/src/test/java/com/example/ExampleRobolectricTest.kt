package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GameRepository
import com.example.model.CardAndBoardEngine
import com.example.model.CardRank
import com.example.model.CardSuit
import com.example.model.GameUiState
import com.example.model.PlayingCard
import com.example.model.TeenPattiHandRank
import com.example.viewmodel.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SpinMint", appName)
    }

    @Test
    fun `wallet coin to cash conversion matches specification`() {
        val state = GameUiState(coins = 10_000L)
        assertEquals(100.0, state.inrBalance, 0.001)
        assertEquals(1.0, state.usdBalance, 0.001)
    }

    @Test
    fun `energy recovery caps at max energy`() {
        val recovered = GameRepository.computeRecoveredEnergy(
            savedEnergy = 85,
            lastUpdateMs = 0L,
            nowMs = 60_000L,
            maxEnergy = 100
        )
        assertEquals(100, recovered)
    }

    @Test
    fun `mines multiplier increases monotonically with gems found`() {
        val m1 = GameViewModel.computeMinesMultiplier(1)
        val m3 = GameViewModel.computeMinesMultiplier(3)
        val m5 = GameViewModel.computeMinesMultiplier(5)
        assertTrue(m1 > 1.0)
        assertTrue(m3 > m1)
        assertTrue(m5 > m3)
    }

    @Test
    fun `plinko buckets are symmetric with 7 slots`() {
        assertEquals(7, GameViewModel.PLINKO_MULTIPLIERS.size)
        assertEquals(7, GameViewModel.PLINKO_FREE_REWARDS.size)
        assertEquals(5.0, GameViewModel.PLINKO_MULTIPLIERS.first(), 0.001)
        assertEquals(5.0, GameViewModel.PLINKO_MULTIPLIERS.last(), 0.001)
    }

    @Test
    fun `teen patti evaluates trail higher than pure sequence`() {
        val trail = CardAndBoardEngine.evaluateTeenPattiHand(
            listOf(
                PlayingCard(CardSuit.SPADES, CardRank.KING),
                PlayingCard(CardSuit.HEARTS, CardRank.KING),
                PlayingCard(CardSuit.DIAMONDS, CardRank.KING)
            )
        )
        val pureSeq = CardAndBoardEngine.evaluateTeenPattiHand(
            listOf(
                PlayingCard(CardSuit.HEARTS, CardRank.ACE),
                PlayingCard(CardSuit.HEARTS, CardRank.KING),
                PlayingCard(CardSuit.HEARTS, CardRank.QUEEN)
            )
        )
        assertEquals(TeenPattiHandRank.TRAIL, trail.handRank)
        assertEquals(TeenPattiHandRank.PURE_SEQUENCE, pureSeq.handRank)
        assertTrue(trail > pureSeq)
    }

    @Test
    fun `blackjack soft ace adjusts from 11 to 1 to avoid bust`() {
        val score = CardAndBoardEngine.calculateBlackjackScore(
            listOf(
                PlayingCard(CardSuit.SPADES, CardRank.ACE),
                PlayingCard(CardSuit.HEARTS, CardRank.NINE),
                PlayingCard(CardSuit.CLUBS, CardRank.FIVE)
            )
        )
        assertEquals(15, score)
    }

    @Test
    fun `ludo allows deploying base token only on rolling 6`() {
        val tokens = listOf(-1, -1, 10, 56)
        assertEquals(listOf(2), CardAndBoardEngine.getMovableTokens(tokens, 4))
        assertEquals(listOf(0, 1, 2), CardAndBoardEngine.getMovableTokens(tokens, 6))
    }

    @Test
    fun `one percent admin commission deducts exact 1 percent on wins and accumulates on taps`() {
        val largeWin = GameViewModel.splitWinWithOnePercentAdminFee(1_000L, 0L)
        assertEquals(990L, largeWin.netPlayerCoins)
        assertEquals(10L, largeWin.adminFeeCoins)
        assertEquals(0L, largeWin.nextPendingPool)

        // 10 taps of 10 coins = 100 coins total -> 1 coin goes to admin on the 10th tap
        var pool = 0L
        var totalNet = 0L
        var totalAdmin = 0L
        repeat(10) {
            val split = GameViewModel.splitWinWithOnePercentAdminFee(10L, pool)
            totalNet += split.netPlayerCoins
            totalAdmin += split.adminFeeCoins
            pool = split.nextPendingPool
        }
        assertEquals(99L, totalNet)
        assertEquals(1L, totalAdmin)
        assertEquals(0L, pool)
    }
}
