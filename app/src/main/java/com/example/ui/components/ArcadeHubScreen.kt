package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AndarBaharSide
import com.example.model.AndarBaharState
import com.example.model.ArcadeGameType
import com.example.model.BlackjackState
import com.example.model.DiceChoice
import com.example.model.DiceGameState
import com.example.model.FruitSlotsState
import com.example.model.LudoGameState
import com.example.model.MinesGameState
import com.example.model.PlinkoDropPlan
import com.example.model.PlinkoGameState
import com.example.model.TeenPattiState
import com.example.ui.theme.BorderViolet
import com.example.ui.theme.BrightEmerald
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CardSlateElevated
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun ArcadeHubScreen(
    activeGame: ArcadeGameType,
    coins: Long,
    ludoState: LudoGameState,
    fruitState: FruitSlotsState,
    teenPattiState: TeenPattiState,
    andarBaharState: AndarBaharState,
    blackjackState: BlackjackState,
    minesState: MinesGameState,
    plinkoState: PlinkoGameState,
    diceState: DiceGameState,
    onSelectGame: (ArcadeGameType) -> Unit,
    onStartLudo: (Long, Boolean) -> Unit,
    onRollLudoDice: () -> Unit,
    onMoveLudoToken: (Int) -> Unit,
    onSpinFruits: (Long, Boolean) -> Unit,
    onDealTeenPatti: (Long, Boolean) -> Unit,
    onPlayAndarBahar: (AndarBaharSide, Long, Boolean) -> Unit,
    onStartBlackjack: (Long, Boolean) -> Unit,
    onHitBlackjack: () -> Unit,
    onStandBlackjack: () -> Unit,
    onStartMines: (Long, Boolean) -> Unit,
    onRevealMineTile: (Int) -> Unit,
    onCashOutMines: () -> Unit,
    onRequestPlinkoDrop: suspend (Long, Boolean) -> PlinkoDropPlan?,
    onPlinkoPegHit: () -> Unit,
    onCompletePlinkoDrop: (PlinkoDropPlan) -> Unit,
    onRollDice: (DiceChoice, Long, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Horizontal Scrollable Game Selector (Ludo, Fruit, Card Games, Vault & Dice)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ArcadeGameType.entries) { game ->
                val selected = activeGame == game
                val accent = when (game) {
                    ArcadeGameType.LUDO -> BrightEmerald
                    ArcadeGameType.FRUIT -> NeonMagenta
                    ArcadeGameType.TEEN_PATTI -> NeonGold
                    ArcadeGameType.ANDAR_BAHAR -> NeonCyan
                    ArcadeGameType.BLACKJACK -> ElectricViolet
                    ArcadeGameType.MINES -> EmeraldMint
                    ArcadeGameType.PLINKO -> NeonGold
                    ArcadeGameType.DICE -> NeonCyan
                }
                val icon = when (game) {
                    ArcadeGameType.LUDO -> Icons.Rounded.Extension
                    ArcadeGameType.FRUIT -> Icons.Rounded.AutoAwesome
                    ArcadeGameType.TEEN_PATTI, ArcadeGameType.ANDAR_BAHAR, ArcadeGameType.BLACKJACK -> Icons.Rounded.Style
                    ArcadeGameType.MINES -> Icons.Rounded.Diamond
                    ArcadeGameType.PLINKO -> Icons.Rounded.Grain
                    ArcadeGameType.DICE -> Icons.Rounded.Casino
                }

                Surface(
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("arcade_tab_${game.name.lowercase()}")
                        .clickable { onSelectGame(game) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) CardSlateElevated else CardSlate,
                    border = BorderStroke(
                        width = if (selected) 1.6.dp else 1.dp,
                        color = if (selected) accent else BorderViolet
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = game.title,
                            tint = if (selected) accent else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = game.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) TextPrimary else TextMuted,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = game.badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) accent else TextMuted.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Crossfade(
            targetState = activeGame,
            animationSpec = tween(200),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            label = "arcade_game_crossfade"
        ) { game ->
            when (game) {
                ArcadeGameType.LUDO -> LudoGameScreen(
                    coins = coins,
                    state = ludoState,
                    onStartMatch = onStartLudo,
                    onRollDice = onRollLudoDice,
                    onMoveToken = onMoveLudoToken
                )

                ArcadeGameType.FRUIT -> FruitSlotsGame(
                    coins = coins,
                    state = fruitState,
                    onSpinReels = onSpinFruits
                )

                ArcadeGameType.TEEN_PATTI -> TeenPattiGame(
                    coins = coins,
                    state = teenPattiState,
                    onDealRound = onDealTeenPatti
                )

                ArcadeGameType.ANDAR_BAHAR -> AndarBaharGame(
                    coins = coins,
                    state = andarBaharState,
                    onPlayRound = onPlayAndarBahar
                )

                ArcadeGameType.BLACKJACK -> BlackjackGame(
                    coins = coins,
                    state = blackjackState,
                    onStartRound = onStartBlackjack,
                    onHit = onHitBlackjack,
                    onStand = onStandBlackjack
                )

                ArcadeGameType.MINES -> GemMinesGame(
                    coins = coins,
                    state = minesState,
                    onStartGame = onStartMines,
                    onRevealTile = onRevealMineTile,
                    onCashOut = onCashOutMines
                )

                ArcadeGameType.PLINKO -> PlinkoDropGame(
                    coins = coins,
                    state = plinkoState,
                    onRequestDrop = onRequestPlinkoDrop,
                    onPegHit = onPlinkoPegHit,
                    onCompleteDrop = onCompletePlinkoDrop
                )

                ArcadeGameType.DICE -> LuckyDiceGame(
                    coins = coins,
                    state = diceState,
                    onRollDice = onRollDice
                )
            }
        }
    }
}
