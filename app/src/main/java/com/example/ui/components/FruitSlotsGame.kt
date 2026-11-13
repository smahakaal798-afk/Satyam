package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FruitSlotsState
import com.example.model.FruitSymbol
import com.example.ui.theme.BorderViolet
import com.example.ui.theme.BrightEmerald
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CardSlateElevated
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidObsidian

@Composable
fun FruitSlotsGame(
    coins: Long,
    state: FruitSlotsState,
    onSpinReels: (Long, Boolean) -> Unit,
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
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "NEON FRUIT SLOTS",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonMagenta,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = state.statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if ((state.lastWinCoins ?: 0L) > 0L) BrightEmerald else TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3-Reel Slot Cabinet
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp),
            shape = RoundedCornerShape(24.dp),
            color = DeepIndigo,
            border = BorderStroke(2.dp, NeonGold.copy(alpha = 0.75f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.reels.forEach { symbol ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(108.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = CardSlateElevated,
                            border = BorderStroke(1.2.dp, BorderViolet)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = symbol.emoji,
                                    fontSize = 46.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Compact Payout Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FruitSymbol.entries.take(3).forEach { sym ->
                        Text(
                            text = "${sym.emoji}x3=${sym.payoutMultiplier}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FruitSymbol.entries.drop(3).forEach { sym ->
                        Text(
                            text = "${sym.emoji}x3=${sym.payoutMultiplier}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonGold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        StakeSelectorRow(
            coins = coins,
            selectedStake = selectedStake,
            isFreePlay = isFreePlay,
            onSelectFree = { if (!state.isSpinning) isFreePlay = true },
            onSelectStake = { stake ->
                if (!state.isSpinning) {
                    isFreePlay = false
                    selectedStake = stake
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = { onSpinReels(selectedStake, isFreePlay) },
            enabled = !state.isSpinning,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .height(52.dp)
                .testTag("spin_fruit_slots_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonGold,
                contentColor = VoidObsidian,
                disabledContainerColor = CardSlateElevated,
                disabledContentColor = TextPrimary
            )
        ) {
            Text(
                text = when {
                    state.isSpinning -> "SPINNING FRUITS…"
                    isFreePlay -> "FREE FRUIT SPIN"
                    else -> "SPIN FOR $selectedStake COINS"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
