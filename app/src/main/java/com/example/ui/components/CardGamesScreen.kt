package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AndarBaharSide
import com.example.model.AndarBaharState
import com.example.model.BlackjackState
import com.example.model.CardAndBoardEngine
import com.example.model.PlayingCard
import com.example.model.TeenPattiState
import com.example.ui.theme.BorderViolet
import com.example.ui.theme.BrightEmerald
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CardSlateElevated
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.RoseAlert
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidObsidian

@Composable
fun PlayingCardView(
    card: PlayingCard?,
    faceDown: Boolean = false,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val width = if (compact) 54.dp else 72.dp
    val height = if (compact) 76.dp else 102.dp

    Surface(
        modifier = modifier.size(width = width, height = height),
        shape = RoundedCornerShape(12.dp),
        color = if (faceDown || card == null) ElectricViolet.copy(alpha = 0.28f) else Color(0xFFFDFBF7),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (faceDown || card == null) NeonGold.copy(alpha = 0.7f) else BorderViolet
        ),
        shadowElevation = 4.dp
    ) {
        if (faceDown || card == null) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "★",
                    fontSize = if (compact) 20.sp else 26.sp,
                    color = NeonGold
                )
            }
        } else {
            val inkColor = if (card.suit.isRed) Color(0xFFE11D48) else Color(0xFF0F172A)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${card.rank.label}${card.suit.symbol}",
                    fontSize = if (compact) 12.sp else 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = inkColor
                )
                Text(
                    text = card.suit.symbol,
                    fontSize = if (compact) 20.sp else 26.sp,
                    color = inkColor,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text(
                    text = card.rank.label,
                    fontSize = if (compact) 11.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = inkColor,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun TeenPattiGame(
    coins: Long,
    state: TeenPattiState,
    onDealRound: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStake by remember { mutableLongStateOf(50L) }
    var isFreePlay by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(16.dp),
            color = CardSlate,
            border = BorderStroke(1.dp, BorderViolet)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TEEN PATTI • 3-CARD SHOWDOWN",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = state.outcomeText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if ((state.lastWinCoins ?: 0L) > 0L) BrightEmerald else TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dealer Hand
        Text(
            text = if (state.dealerHandName.isNotEmpty()) "DEALER (${state.dealerHandName})" else "DEALER HAND",
            style = MaterialTheme.typography.labelSmall,
            color = RoseAlert
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (i in 0 until 3) {
                PlayingCardView(
                    card = state.dealerCards.getOrNull(i),
                    faceDown = state.dealerCards.isEmpty() || state.isDealing
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Player Hand
        Text(
            text = if (state.playerHandName.isNotEmpty()) "YOUR HAND (${state.playerHandName})" else "YOUR HAND",
            style = MaterialTheme.typography.labelSmall,
            color = BrightEmerald
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (i in 0 until 3) {
                PlayingCardView(
                    card = state.playerCards.getOrNull(i),
                    faceDown = state.playerCards.isEmpty() || state.isDealing
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        StakeSelectorRow(
            coins = coins,
            selectedStake = selectedStake,
            isFreePlay = isFreePlay,
            onSelectFree = { if (!state.isDealing) isFreePlay = true },
            onSelectStake = { stake ->
                if (!state.isDealing) {
                    isFreePlay = false
                    selectedStake = stake
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onDealRound(selectedStake, isFreePlay) },
            enabled = !state.isDealing,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .height(52.dp)
                .testTag("deal_teen_patti_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonGold,
                contentColor = VoidObsidian
            )
        ) {
            Text(
                text = when {
                    state.isDealing -> "DEALING CARDS…"
                    isFreePlay -> "FREE TEEN PATTI DEAL"
                    else -> "SHOWDOWN FOR $selectedStake COINS"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun AndarBaharGame(
    coins: Long,
    state: AndarBaharState,
    onPlayRound: (AndarBaharSide, Long, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSide by remember { mutableStateOf(AndarBaharSide.ANDAR) }
    var selectedStake by remember { mutableLongStateOf(50L) }
    var isFreePlay by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(16.dp),
            color = CardSlate,
            border = BorderStroke(1.dp, BorderViolet)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ANDAR BAHAR (1.95x)",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = state.statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if ((state.lastWinCoins ?: 0L) > 0L) BrightEmerald else TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Joker Card Centerpiece
        Text("JOKER CARD", style = MaterialTheme.typography.labelSmall, color = NeonGold)
        Spacer(modifier = Modifier.height(6.dp))
        PlayingCardView(card = state.jokerCard, faceDown = state.jokerCard == null)

        Spacer(modifier = Modifier.height(12.dp))

        // Andar & Bahar Dealt Rows
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            shape = RoundedCornerShape(16.dp),
            color = DeepIndigo,
            border = BorderStroke(1.dp, BorderViolet)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "ANDAR (${state.andarCards.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.winningSide == AndarBaharSide.ANDAR) BrightEmerald else NeonCyan
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.andarCards) { card ->
                        PlayingCardView(card = card, compact = true)
                    }
                }
                Text(
                    text = "BAHAR (${state.baharCards.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.winningSide == AndarBaharSide.BAHAR) BrightEmerald else NeonGold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.baharCards) { card ->
                        PlayingCardView(card = card, compact = true)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Side Choice
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AndarBaharSide.entries.forEach { side ->
                val selected = selectedSide == side
                val accent = if (side == AndarBaharSide.ANDAR) NeonCyan else NeonGold
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable(enabled = !state.isPlaying) { selectedSide = side },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) accent.copy(alpha = 0.22f) else CardSlate,
                    border = BorderStroke(if (selected) 1.6.dp else 1.dp, if (selected) accent else BorderViolet)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = side.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) accent else TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        StakeSelectorRow(
            coins = coins,
            selectedStake = selectedStake,
            isFreePlay = isFreePlay,
            onSelectFree = { if (!state.isPlaying) isFreePlay = true },
            onSelectStake = { stake ->
                if (!state.isPlaying) {
                    isFreePlay = false
                    selectedStake = stake
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onPlayRound(selectedSide, selectedStake, isFreePlay) },
            enabled = !state.isPlaying,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .height(52.dp)
                .testTag("deal_andar_bahar_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldMint,
                contentColor = VoidObsidian
            )
        ) {
            Text(
                text = if (state.isPlaying) "DEALING…" else "BET ON ${selectedSide.label}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun BlackjackGame(
    coins: Long,
    state: BlackjackState,
    onStartRound: (Long, Boolean) -> Unit,
    onHit: () -> Unit,
    onStand: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStake by remember { mutableLongStateOf(50L) }
    var isFreePlay by remember { mutableStateOf(true) }

    val playerScore = CardAndBoardEngine.calculateBlackjackScore(state.playerCards)
    val dealerScoreText = if (state.dealerCards.isEmpty()) {
        "—"
    } else if (state.hideDealerHoleCard) {
        "${state.dealerCards.first().rank.bjValue} + ?"
    } else {
        "${CardAndBoardEngine.calculateBlackjackScore(state.dealerCards)}"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(16.dp),
            color = CardSlate,
            border = BorderStroke(1.dp, BorderViolet)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "BLACKJACK 21 (NATURAL 2.5x)",
                    style = MaterialTheme.typography.titleMedium,
                    color = BrightEmerald,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = state.statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if ((state.lastWinCoins ?: 0L) > 0L) NeonGold else TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "DEALER ($dealerScoreText)",
            style = MaterialTheme.typography.labelSmall,
            color = RoseAlert,
            fontFamily = JetBrainsMonoFontFamily
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.dealerCards.isEmpty()) {
                PlayingCardView(card = null, faceDown = true)
                PlayingCardView(card = null, faceDown = true)
            } else {
                state.dealerCards.forEachIndexed { idx, card ->
                    PlayingCardView(
                        card = card,
                        faceDown = idx == 1 && state.hideDealerHoleCard
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = if (state.playerCards.isEmpty()) "PLAYER" else "PLAYER ($playerScore)",
            style = MaterialTheme.typography.labelSmall,
            color = BrightEmerald,
            fontFamily = JetBrainsMonoFontFamily
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.playerCards.isEmpty()) {
                PlayingCardView(card = null, faceDown = true)
                PlayingCardView(card = null, faceDown = true)
            } else {
                state.playerCards.forEach { card ->
                    PlayingCardView(card = card)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!state.isActive) {
            StakeSelectorRow(
                coins = coins,
                selectedStake = selectedStake,
                isFreePlay = isFreePlay,
                onSelectFree = { isFreePlay = true },
                onSelectStake = { stake ->
                    isFreePlay = false
                    selectedStake = stake
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onStartRound(selectedStake, isFreePlay) },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .height(52.dp)
                    .testTag("deal_blackjack_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGold,
                    contentColor = VoidObsidian
                )
            ) {
                Text(
                    text = if (isFreePlay) "FREE BLACKJACK DEAL" else "DEAL FOR $selectedStake COINS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onHit,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("blackjack_hit_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldMint,
                        contentColor = VoidObsidian
                    )
                ) {
                    Text("HIT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                }
                Button(
                    onClick = onStand,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("blackjack_stand_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGold,
                        contentColor = VoidObsidian
                    )
                ) {
                    Text("STAND", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
