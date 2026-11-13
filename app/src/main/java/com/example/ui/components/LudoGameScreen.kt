package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.CardAndBoardEngine
import com.example.model.LudoGameState
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
import kotlin.math.hypot

private val LUDO_OUTER_TRACK: List<Pair<Int, Int>> = buildList {
    for (r in 13 downTo 9) add(6 to r)       // 0..4
    for (c in 5 downTo 0) add(c to 8)        // 5..10
    add(0 to 7)                               // 11
    for (c in 0..5) add(c to 6)              // 12..17
    for (r in 5 downTo 0) add(6 to r)        // 18..23
    add(7 to 0)                               // 24
    for (r in 0..5) add(8 to r)              // 25..30 (26 = Bot start)
    for (c in 9..14) add(c to 6)             // 31..36
    add(14 to 7)                              // 37
    for (c in 14 downTo 9) add(c to 8)       // 38..43
    for (r in 9..14) add(8 to r)             // 44..49
    add(7 to 14)                              // 50
    add(6 to 14)                              // 51
}

private val PLAYER_BASE_SLOTS = listOf(1.8f to 10.8f, 3.2f to 10.8f, 1.8f to 12.2f, 3.2f to 12.2f)
private val BOT_BASE_SLOTS = listOf(10.8f to 1.8f, 12.2f to 1.8f, 10.8f to 3.2f, 12.2f to 3.2f)

private fun tokenBoardCoords(progress: Int, tokenIndex: Int, isPlayer: Boolean): Pair<Float, Float> {
    return when {
        progress < 0 -> if (isPlayer) PLAYER_BASE_SLOTS[tokenIndex] else BOT_BASE_SLOTS[tokenIndex]
        progress <= 50 -> {
            val globalIdx = if (isPlayer) progress else (26 + progress) % 52
            val (c, r) = LUDO_OUTER_TRACK[globalIdx]
            c.toFloat() to r.toFloat()
        }
        progress <= 55 -> {
            val step = progress - 51
            if (isPlayer) 7f to (13f - step) else 7f to (1f + step)
        }
        else -> {
            if (isPlayer) (6.6f + tokenIndex * 0.25f) to 7.8f else (6.6f + tokenIndex * 0.25f) to 6.1f
        }
    }
}

@Composable
fun LudoGameScreen(
    coins: Long,
    state: LudoGameState,
    onStartMatch: (Long, Boolean) -> Unit,
    onRollDice: () -> Unit,
    onMoveToken: (Int) -> Unit,
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
        // Status & Score Banner
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.winnerLabel ?: "LUDO QUICK BATTLE (YOU vs BOT)",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (state.winnerLabel != null) NeonGold else BrightEmerald,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = state.statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "YOU ${state.playerTokens.count { it == 56 }}/2",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrightEmerald,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                    Text(
                        text = "BOT ${state.botTokens.count { it == 56 }}/2",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonGold,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 15x15 Ludo Board Canvas
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .aspectRatio(1f),
            shape = RoundedCornerShape(18.dp),
            color = DeepIndigo,
            border = BorderStroke(1.5.dp, BorderViolet)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("ludo_board_canvas")
                    .pointerInput(state.movableTokenIndices, state.playerTokens, state.isActive) {
                        detectTapGestures { tapOffset ->
                            if (!state.isActive || state.movableTokenIndices.isEmpty()) return@detectTapGestures
                            val cell = size.width / 15f
                            var bestIdx: Int? = null
                            var bestDist = Float.MAX_VALUE
                            for (idx in state.movableTokenIndices) {
                                val (cx, cy) = tokenBoardCoords(state.playerTokens[idx], idx, isPlayer = true)
                                val px = (cx + 0.5f) * cell
                                val py = (cy + 0.5f) * cell
                                val d = hypot(tapOffset.x - px, tapOffset.y - py)
                                if (d < cell * 1.45f && d < bestDist) {
                                    bestDist = d
                                    bestIdx = idx
                                }
                            }
                            bestIdx?.let(onMoveToken)
                        }
                    }
            ) {
                val cell = size.width / 15f

                // Track grid background
                for (r in 0 until 15) {
                    for (c in 0 until 15) {
                        val inCorner = (r < 6 && c < 6) || (r < 6 && c > 8) || (r > 8 && c < 6) || (r > 8 && c > 8)
                        val inCenter = r in 6..8 && c in 6..8
                        if (!inCorner && !inCenter) {
                            val cellColor = when {
                                c == 7 && r in 9..13 -> EmeraldMint.copy(alpha = 0.42f) // Player home stretch
                                c == 6 && r == 13 -> EmeraldMint.copy(alpha = 0.72f) // Player start
                                c == 7 && r in 1..5 -> NeonGold.copy(alpha = 0.42f) // Bot home stretch
                                c == 8 && r == 1 -> NeonGold.copy(alpha = 0.72f) // Bot start
                                else -> CardSlateElevated.copy(alpha = 0.75f)
                            }
                            drawRect(
                                color = cellColor,
                                topLeft = Offset(c * cell, r * cell),
                                size = Size(cell, cell)
                            )
                            drawRect(
                                color = BorderViolet.copy(alpha = 0.65f),
                                topLeft = Offset(c * cell, r * cell),
                                size = Size(cell, cell),
                                style = Stroke(width = 1.dp.toPx())
                            )
                        }
                    }
                }

                // Draw Safe Star Markers
                CardAndBoardEngine.SAFE_GLOBAL_CELLS.forEach { gIdx ->
                    val (sc, sr) = LUDO_OUTER_TRACK[gIdx]
                    drawCircle(
                        color = Color.White.copy(alpha = 0.35f),
                        radius = cell * 0.22f,
                        center = Offset((sc + 0.5f) * cell, (sr + 0.5f) * cell),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // 4 Corner Yard Boxes
                fun drawYard(startCol: Int, startRow: Int, color: Color) {
                    drawRoundRect(
                        color = color.copy(alpha = 0.24f),
                        topLeft = Offset(startCol * cell + 2f, startRow * cell + 2f),
                        size = Size(6 * cell - 4f, 6 * cell - 4f),
                        cornerRadius = CornerRadius(10.dp.toPx())
                    )
                    drawRoundRect(
                        color = color.copy(alpha = 0.7f),
                        topLeft = Offset(startCol * cell + 2f, startRow * cell + 2f),
                        size = Size(6 * cell - 4f, 6 * cell - 4f),
                        cornerRadius = CornerRadius(10.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                drawYard(0, 0, RoseAlert)
                drawYard(9, 0, NeonGold)
                drawYard(0, 9, EmeraldMint)
                drawYard(9, 9, NeonCyan)

                // Center Home Triangles
                val cx = 7.5f * cell
                val cy = 7.5f * cell
                val left = 6f * cell
                val right = 9f * cell
                val top = 6f * cell
                val bottom = 9f * cell

                val bottomTri = Path().apply { moveTo(left, bottom); lineTo(right, bottom); lineTo(cx, cy); close() }
                drawPath(bottomTri, EmeraldMint.copy(alpha = 0.85f))

                val topTri = Path().apply { moveTo(left, top); lineTo(right, top); lineTo(cx, cy); close() }
                drawPath(topTri, NeonGold.copy(alpha = 0.85f))

                val leftTri = Path().apply { moveTo(left, top); lineTo(left, bottom); lineTo(cx, cy); close() }
                drawPath(leftTri, RoseAlert.copy(alpha = 0.55f))

                val rightTri = Path().apply { moveTo(right, top); lineTo(right, bottom); lineTo(cx, cy); close() }
                drawPath(rightTri, NeonCyan.copy(alpha = 0.55f))

                // Bot Gold Tokens
                state.botTokens.forEachIndexed { idx, progress ->
                    val (tc, tr) = tokenBoardCoords(progress, idx, isPlayer = false)
                    val center = Offset((tc + 0.5f) * cell, (tr + 0.5f) * cell)
                    drawCircle(color = VoidObsidian, radius = cell * 0.36f, center = center)
                    drawCircle(color = NeonGold, radius = cell * 0.30f, center = center)
                    drawCircle(color = Color.White.copy(alpha = 0.65f), radius = cell * 0.12f, center = center)
                }

                // Player Emerald Tokens
                state.playerTokens.forEachIndexed { idx, progress ->
                    val (tc, tr) = tokenBoardCoords(progress, idx, isPlayer = true)
                    val center = Offset((tc + 0.5f) * cell, (tr + 0.5f) * cell)
                    val isMovable = idx in state.movableTokenIndices
                    if (isMovable) {
                        drawCircle(
                            color = NeonGold,
                            radius = cell * 0.48f,
                            center = center,
                            style = Stroke(width = 2.6.dp.toPx())
                        )
                    }
                    drawCircle(color = VoidObsidian, radius = cell * 0.36f, center = center)
                    drawCircle(color = BrightEmerald, radius = cell * 0.30f, center = center)
                    drawCircle(color = Color.White.copy(alpha = 0.8f), radius = cell * 0.12f, center = center)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

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
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { onStartMatch(selectedStake, isFreePlay) },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .height(50.dp)
                    .testTag("start_ludo_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldMint,
                    contentColor = VoidObsidian
                )
            ) {
                Text(
                    text = if (isFreePlay) "PLAY FREE LUDO (WIN +250)" else "START LUDO (${selectedStake * 2} WIN)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = CardSlateElevated,
                    border = BorderStroke(1.5.dp, if (state.isPlayerTurn) BrightEmerald else NeonGold)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${state.diceValue}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Button(
                    onClick = onRollDice,
                    enabled = state.isPlayerTurn && !state.hasRolled && !state.isBotThinking,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("roll_ludo_dice_button"),
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
                            state.isBotThinking -> "BOT THINKING…"
                            state.movableTokenIndices.isNotEmpty() -> "TAP GLOWING TOKEN"
                            else -> "ROLL LUDO DICE"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
