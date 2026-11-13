package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.example.model.MineTileState
import com.example.model.MinesGameState
import com.example.ui.theme.BorderViolet
import com.example.ui.theme.BrightEmerald
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CardSlateElevated
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonGold
import com.example.ui.theme.RoseAlert
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidObsidian
import java.util.Locale

@Composable
fun GemMinesGame(
    coins: Long,
    state: MinesGameState,
    onStartGame: (Long, Boolean) -> Unit,
    onRevealTile: (Int) -> Unit,
    onCashOut: () -> Unit,
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
        // HUD Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(16.dp),
            color = CardSlate,
            border = BorderStroke(1.dp, BorderViolet)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GEM MINES (22 GEMS • 3 MINES)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = if (state.isActive) {
                            "${String.format(Locale.US, "%.2fx", state.currentMultiplier)} Multiplier"
                        } else if (state.hitMine) {
                            "Laser Mine Triggered!"
                        } else {
                            "Tap tiles to uncover Emerald Gems"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (state.hitMine) RoseAlert else NeonGold,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldMint.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, EmeraldMint.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "+${state.potentialWinCoins}",
                        style = MaterialTheme.typography.labelLarge,
                        color = BrightEmerald,
                        fontFamily = JetBrainsMonoFontFamily,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5x5 Vault Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (row in 0 until 5) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (col in 0 until 5) {
                        val idx = row * 5 + col
                        val tile = state.tiles[idx]
                        val bgColor = when (tile) {
                            MineTileState.HIDDEN -> CardSlateElevated
                            MineTileState.GEM -> EmeraldMint.copy(alpha = 0.24f)
                            MineTileState.MINE -> RoseAlert.copy(alpha = 0.24f)
                        }
                        val borderColor = when (tile) {
                            MineTileState.HIDDEN -> if (state.isActive) BorderViolet else BorderViolet.copy(alpha = 0.5f)
                            MineTileState.GEM -> BrightEmerald
                            MineTileState.MINE -> RoseAlert
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .testTag("mine_tile_$idx")
                                .clickable(enabled = state.isActive && tile == MineTileState.HIDDEN) {
                                    onRevealTile(idx)
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = bgColor,
                            border = BorderStroke(1.2.dp, borderColor)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                when (tile) {
                                    MineTileState.HIDDEN -> Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = "Hidden vault tile",
                                        tint = if (state.isActive) TextSecondary.copy(alpha = 0.5f) else TextMuted.copy(alpha = 0.3f),
                                        modifier = Modifier.size(18.dp)
                                    )

                                    MineTileState.GEM -> Icon(
                                        imageVector = Icons.Rounded.Diamond,
                                        contentDescription = "Emerald Gem",
                                        tint = BrightEmerald,
                                        modifier = Modifier.size(26.dp)
                                    )

                                    MineTileState.MINE -> Icon(
                                        imageVector = Icons.Rounded.LocalFireDepartment,
                                        contentDescription = "Laser Mine",
                                        tint = RoseAlert,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

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
                onClick = { onStartGame(selectedStake, isFreePlay) },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .height(52.dp)
                    .testTag("start_mines_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGold,
                    contentColor = VoidObsidian
                )
            ) {
                Text(
                    text = if (isFreePlay) "START FREE VAULT RUN" else "PLAY FOR $selectedStake COINS",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        } else {
            Button(
                onClick = onCashOut,
                enabled = state.gemsFound > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .height(52.dp)
                    .testTag("cashout_mines_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldMint,
                    contentColor = VoidObsidian,
                    disabledContainerColor = CardSlateElevated,
                    disabledContentColor = TextSecondary
                )
            ) {
                Text(
                    text = if (state.gemsFound > 0) {
                        "CASH OUT +${state.potentialWinCoins} COINS"
                    } else {
                        "TAP A TILE TO UNCOVER GEM"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun StakeSelectorRow(
    coins: Long,
    selectedStake: Long,
    isFreePlay: Boolean,
    onSelectFree: () -> Unit,
    onSelectStake: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onSelectFree,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                width = if (isFreePlay) 1.6.dp else 1.dp,
                color = if (isFreePlay) BrightEmerald else BorderViolet
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isFreePlay) EmeraldMint.copy(alpha = 0.16f) else CardSlate,
                contentColor = if (isFreePlay) BrightEmerald else TextPrimary
            ),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            Text("FREE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }

        listOf(50L, 150L, 500L).forEach { stake ->
            val selected = !isFreePlay && selectedStake == stake
            val affordable = coins >= stake
            OutlinedButton(
                onClick = { onSelectStake(stake) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (selected) 1.6.dp else 1.dp,
                    color = if (selected) NeonGold else BorderViolet
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (selected) NeonGold.copy(alpha = 0.16f) else CardSlate,
                    contentColor = when {
                        selected -> NeonGold
                        affordable -> TextPrimary
                        else -> TextMuted
                    }
                ),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text("$stake", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}
