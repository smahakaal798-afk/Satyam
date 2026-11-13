package com.example.model

/**
 * Pure, deterministic game-rule engines for Card Games (Teen Patti, Blackjack)
 * and Ludo Board rules so they remain cleanly separated and unit-testable.
 */
object CardAndBoardEngine {

    fun createShuffledDeck(): List<PlayingCard> {
        val deck = mutableListOf<PlayingCard>()
        for (suit in CardSuit.entries) {
            for (rank in CardRank.entries) {
                deck.add(PlayingCard(suit, rank))
            }
        }
        return deck.shuffled()
    }

    // --- Blackjack 21 ---
    fun calculateBlackjackScore(cards: List<PlayingCard>): Int {
        var total = cards.sumOf { it.rank.bjValue }
        var aceCount = cards.count { it.rank == CardRank.ACE }
        while (total > 21 && aceCount > 0) {
            total -= 10
            aceCount--
        }
        return total
    }

    // --- Teen Patti 3-Card Hand Evaluator ---
    data class EvaluatedTeenPattiHand(
        val handRank: TeenPattiHandRank,
        val tieBreakers: List<Int>
    ) : Comparable<EvaluatedTeenPattiHand> {
        override fun compareTo(other: EvaluatedTeenPattiHand): Int {
            if (this.handRank.rankWeight != other.handRank.rankWeight) {
                return this.handRank.rankWeight.compareTo(other.handRank.rankWeight)
            }
            for (i in tieBreakers.indices) {
                val b = other.tieBreakers.getOrElse(i) { 0 }
                if (tieBreakers[i] != b) return tieBreakers[i].compareTo(b)
            }
            return 0
        }
    }

    fun evaluateTeenPattiHand(cards: List<PlayingCard>): EvaluatedTeenPattiHand {
        require(cards.size == 3)
        val sortedDesc = cards.map { it.rank.rankValue }.sortedDescending()
        val isSameSuit = cards.all { it.suit == cards.first().suit }
        val isTrail = sortedDesc[0] == sortedDesc[1] && sortedDesc[1] == sortedDesc[2]

        // Standard Teen Patti sequence check (A-K-Q highest, A-2-3 second highest, down to 4-3-2)
        val isNormalSeq = (sortedDesc[0] - 1 == sortedDesc[1]) && (sortedDesc[1] - 1 == sortedDesc[2])
        val isAceLowSeq = sortedDesc == listOf(14, 3, 2)
        val isSequence = isNormalSeq || isAceLowSeq
        val seqHighValue = if (isAceLowSeq) 13 else sortedDesc[0]

        return when {
            isTrail -> EvaluatedTeenPattiHand(TeenPattiHandRank.TRAIL, sortedDesc)
            isSameSuit && isSequence -> EvaluatedTeenPattiHand(TeenPattiHandRank.PURE_SEQUENCE, listOf(seqHighValue))
            isSequence -> EvaluatedTeenPattiHand(TeenPattiHandRank.SEQUENCE, listOf(seqHighValue))
            isSameSuit -> EvaluatedTeenPattiHand(TeenPattiHandRank.COLOR, sortedDesc)
            sortedDesc[0] == sortedDesc[1] -> EvaluatedTeenPattiHand(
                TeenPattiHandRank.PAIR,
                listOf(sortedDesc[0], sortedDesc[2])
            )
            sortedDesc[1] == sortedDesc[2] -> EvaluatedTeenPattiHand(
                TeenPattiHandRank.PAIR,
                listOf(sortedDesc[1], sortedDesc[0])
            )
            else -> EvaluatedTeenPattiHand(TeenPattiHandRank.HIGH_CARD, sortedDesc)
        }
    }

    // --- Ludo Track & Legal Move Helpers ---
    // 52-cell outer loop:
    // Player (Emerald, Bottom-Left) starts at track index 0 -> 50, then home stretch 51..55, Home = 56.
    // Bot (Gold, Top-Right) starts opposite at track index 26, so Bot's relative progress p (0..50)
    // maps to global track cell `(26 + p) % 52`.
    // Safe star cells on global 52-cell track:
    val SAFE_GLOBAL_CELLS = setOf(0, 8, 13, 21, 26, 34, 39, 47)

    fun playerGlobalCell(progress: Int): Int? {
        return if (progress in 0..50) progress else null
    }

    fun botGlobalCell(progress: Int): Int? {
        return if (progress in 0..50) (26 + progress) % 52 else null
    }

    fun getMovableTokens(tokens: List<Int>, dice: Int): List<Int> {
        return tokens.mapIndexedNotNull { idx, progress ->
            when {
                progress == 56 -> null // already home
                progress == -1 -> if (dice == 6) idx else null
                progress + dice <= 56 -> idx
                else -> null
            }
        }
    }
}
