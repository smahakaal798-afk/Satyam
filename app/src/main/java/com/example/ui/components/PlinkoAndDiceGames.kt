package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.DiceChoice
import com.example.model.DiceGameState
import com.example.model.PlinkoDropPlan
import com.example.model.PlinkoGameState
import com.example.ui.theme.AmberGold
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
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.RoseAlert
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidObsidian
import com.example.viewmodel.GameViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun PlinkoDropGame(
    coins: Long,
    state: PlinkoGameState,
    onRequestDrop: suspend (Long, Boolean) -> PlinkoDropPlan?,
    onPegHit: () -> Unit,
    onCompleteDrop: (PlinkoDropPlan) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStake by remember { mutableLongStateOf(50L) }
    var isFreePlay by remember { mutableStateOf(true) }
    val dropProgress = remember { Animatable(0f) }
    var activePlan by remember { mutableStateOf<PlinkoDropPlan?>(null) }
    val scope = rememberCoroutineScope()

    val bucketLabels = if (isFreePlay) {
        listOf("+250", "+120", "+60", "+25", "+60", "+120", "+250")
    } else {
        listOf("5.0x", "2.2x", "1.3x", "0.6x", "1.3x", "2.2x", "5.0x")
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PLINKO COIN DROP",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonGold,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = state.lastWinCoins?.let { "Last: +$it Coins" } ?: "Outer edges = 5.0x!",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrightEmerald,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Peg Board + Buckets
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
                .height(270.dp),
            shape = RoundedCornerShape(22.dp),
            color = DeepIndigo,
            border = BorderStroke(1.2.dp, BorderViolet)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val rows = 6
                    val colStep = size.width / 8f
                    val rowStep = size.height / (rows + 1.2f)
                    val centerX = size.width / 2f

                    // Draw triangular pegs (Row 0 has 3 pegs -> Row 5 has 8 pegs? Let's align cleanly with 7 buckets)
                    // Center alignment: at row r (0..5), draw (r + 2) pegs spaced by colStep
                    for (r in 0 until rows) {
                        val pegsInRow = r + 2
                        val y = rowStep * (r + 1f)
                        val rowStartX = centerX - ((pegsInRow - 1) * colStep) / 2f
                        for (c in 0 until pegsInRow) {
                            val px = rowStartX + c * colStep
                            drawCircle(
                                color = ElectricViolet.copy(alpha = 0.4f),
                                radius = 7.dp.toPx(),
                                center = Offset(px, y)
                            )
                            drawCircle(
                                color = NeonCyan,
                                radius = 3.8.dp.toPx(),
                                center = Offset(px, y)
                            )
                        }
                    }

                    // Draw animated falling coin along the exact plan path
                    val plan = activePlan
                    if (plan != null && state.isDropping) {
                        val t = dropProgress.value.coerceIn(0f, 1f)
                        val totalSteps = plan.directions.size.toFloat() // 6
                        val stepFloat = t * totalSteps
                        val currentStep = stepFloat.toInt().coerceIn(0, 5)
                        val frac = stepFloat - currentStep

                        var offsetSteps = 0f
                        for (i in 0 until currentStep) {
                            offsetSteps += plan.directions[i] * 0.5f
                        }
                        val nextOffsetSteps = offsetSteps + plan.directions[currentStep] * 0.5f
                        val interpOffset = offsetSteps + (nextOffsetSteps - offsetSteps) * frac

                        val ballX = centerX + interpOffset * colStep
                        val baseY = rowStep * (currentStep + frac + 0.35f)
                        // Subtle bounce arc between pegs
                        val bounceLift = sin(frac * Math.PI).toFloat() * (rowStep * 0.22f)
                        val ballY = (baseY - bounceLift).coerceIn(8f, size.height - 8f)

                        drawCircle(
                            color = NeonGold.copy(alpha = 0.35f),
                            radius = 14.dp.toPx(),
                            center = Offset(ballX, ballY)
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFFFF4B8), NeonGold, AmberGold)
                            ),
                            radius = 8.5.dp.toPx(),
                            center = Offset(ballX, ballY)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 7 Multiplier Buckets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    bucketLabels.forEachIndexed { idx, label ->
                        val isJackpot = idx == 0 || idx == 6
                        val isGood = idx == 1 || idx == 5
                        val isHighlighted = state.lastBucketIndex == idx && !state.isDropping
                        val bucketColor = when {
                            isJackpot -> NeonGold
                            isGood -> EmeraldMint
                            idx == 3 -> NeonMagenta
                            else -> NeonCyan
                        }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isHighlighted) bucketColor else bucketColor.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, bucketColor)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isHighlighted) VoidObsidian else TextPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        StakeSelectorRow(
            coins = coins,
            selectedStake = selectedStake,
            isFreePlay = isFreePlay,
            onSelectFree = { if (!state.isDropping) isFreePlay = true },
            onSelectStake = { stake ->
                if (!state.isDropping) {
                    isFreePlay = false
                    selectedStake = stake
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                scope.launch {
                    val plan = onRequestDrop(selectedStake, isFreePlay) ?: return@launch
                    activePlan = plan
                    dropProgress.snapTo(0f)
                    var lastPegStep = -1
                    dropProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 1550, easing = LinearEasing)
                    ) {
                        val step = (value * 6f).toInt()
                        if (step != lastPegStep && step < 6) {
                            lastPegStep = step
                            onPegHit()
                        }
                    }
                    onCompleteDrop(plan)
                }
            },
            enabled = !state.isDropping,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .height(52.dp)
                .testTag("drop_plinko_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonGold,
                contentColor = VoidObsidian,
                disabledContainerColor = CardSlateElevated,
                disabledContentColor = TextSecondary
            )
        ) {
            Text(
                text = when {
                    state.isDropping -> "DROPPING…"
                    isFreePlay -> "DROP FREE GOLD COIN"
                    else -> "DROP FOR $selectedStake COINS"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun LuckyDiceGame(
    coins: Long,
    state: DiceGameState,
    onRollDice: (DiceChoice, Long, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedChoice by remember { mutableStateOf(DiceChoice.OVER_7) }
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
                    text = "LUCKY DICE 7",
                    style = MaterialTheme.typography.titleMedium,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = state.lastResultSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if ((state.lastWinCoins ?: 0L) > 0L) BrightEmerald else TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Dual Neon Dice Visual
        Row(
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeonDieFace(value = state.dieOne, isRolling = state.isRolling, accent = NeonCyan)
            Text(
                text = "+",
                style = MaterialTheme.typography.headlineMedium,
                color = TextSecondary
            )
            NeonDieFace(value = state.dieTwo, isRolling = state.isRolling, accent = NeonGold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "SUM: ${state.dieOne + state.dieTwo}",
            style = MaterialTheme.typography.titleLarge,
            color = NeonGold,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Prediction Choice Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DiceChoice.entries.forEach { choice ->
                val selected = selectedChoice == choice
                val accent = when (choice) {
                    DiceChoice.UNDER_7 -> NeonCyan
                    DiceChoice.EXACT_7 -> NeonGold
                    DiceChoice.OVER_7 -> EmeraldMint
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(68.dp)
                        .testTag("dice_choice_${choice.name.lowercase()}")
                        .clickable(enabled = !state.isRolling) { selectedChoice = choice },
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) accent.copy(alpha = 0.2f) else CardSlate,
                    border = BorderStroke(
                        width = if (selected) 1.8.dp else 1.dp,
                        color = if (selected) accent else BorderViolet
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = choice.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) accent else TextPrimary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${choice.multiplier}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        StakeSelectorRow(
            coins = coins,
            selectedStake = selectedStake,
            isFreePlay = isFreePlay,
            onSelectFree = { if (!state.isRolling) isFreePlay = true },
            onSelectStake = { stake ->
                if (!state.isRolling) {
                    isFreePlay = false
                    selectedStake = stake
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = { onRollDice(selectedChoice, selectedStake, isFreePlay) },
            enabled = !state.isRolling,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .height(52.dp)
                .testTag("roll_dice_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonCyan,
                contentColor = VoidObsidian,
                disabledContainerColor = CardSlateElevated,
                disabledContentColor = TextSecondary
            )
        ) {
            Text(
                text = when {
                    state.isRolling -> "ROLLING…"
                    isFreePlay -> "FREE ROLL (${selectedChoice.label})"
                    else -> "ROLL $selectedStake COINS"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun NeonDieFace(
    value: Int,
    isRolling: Boolean,
    accent: Color
) {
    val tilt = if (isRolling) ((value % 3) - 1) * 8f else 0f
    Canvas(
        modifier = Modifier
            .size(86.dp)
            .graphicsLayer {
                rotationZ = tilt
            }
    ) {
        val corner = CornerRadius(18.dp.toPx(), 18.dp.toPx())
        drawRoundRect(
            brush = Brush.linearGradient(listOf(CardSlateElevated, DeepIndigo)),
            cornerRadius = corner
        )
        drawRoundRect(
            color = accent,
            cornerRadius = corner,
            style = Stroke(width = 2.5.dp.toPx())
        )

        val pips = when (value.coerceIn(1, 6)) {
            1 -> listOf(Offset(0.5f, 0.5f))
            2 -> listOf(Offset(0.28f, 0.28f), Offset(0.72f, 0.72f))
            3 -> listOf(Offset(0.26f, 0.26f), Offset(0.5f, 0.5f), Offset(0.74f, 0.74f))
            4 -> listOf(Offset(0.28f, 0.28f), Offset(0.72f, 0.28f), Offset(0.28f, 0.72f), Offset(0.72f, 0.72f))
            5 -> listOf(
                Offset(0.26f, 0.26f), Offset(0.74f, 0.26f),
                Offset(0.5f, 0.5f),
                Offset(0.26f, 0.74f), Offset(0.74f, 0.74f)
            )
            else -> listOf(
                Offset(0.28f, 0.24f), Offset(0.72f, 0.24f),
                Offset(0.28f, 0.5f), Offset(0.72f, 0.5f),
                Offset(0.28f, 0.76f), Offset(0.72f, 0.76f)
            )
        }

        val pipRadius = 7.dp.toPx()
        pips.forEach { p ->
            val center = Offset(size.width * p.x, size.height * p.y)
            drawCircle(
                color = accent.copy(alpha = 0.25f),
                radius = pipRadius * 1.5f,
                center = center
            )
            drawCircle(
                color = accent,
                radius = pipRadius,
                center = center
            )
        }
    }
}
