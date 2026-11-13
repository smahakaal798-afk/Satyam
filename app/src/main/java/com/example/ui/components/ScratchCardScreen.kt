package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.R
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidObsidian

private data class ScratchDot(val x: Float, val y: Float)

@Composable
fun ScratchCardScreen(
    prizeCoins: Long,
    isRevealed: Boolean,
    cardVersion: Int,
    onScratchStroke: () -> Unit,
    onRevealReward: () -> Unit,
    onNewCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scratchPoints = remember(cardVersion) { mutableStateListOf<ScratchDot>() }
    val gridCols = 16
    val gridRows = 9
    val scratchedCells = remember(cardVersion) { BooleanArray(gridCols * gridRows) }
    var scratchedPercent by remember(cardVersion) { mutableIntStateOf(0) }
    var cardSize by remember { mutableStateOf(IntSize(1, 1)) }
    var strokeCounter by remember { mutableIntStateOf(0) }

    fun markScratchAt(pos: Offset) {
        if (isRevealed) return
        scratchPoints.add(ScratchDot(pos.x, pos.y))
        val w = cardSize.width.coerceAtLeast(1)
        val h = cardSize.height.coerceAtLeast(1)
        val cx = ((pos.x / w) * gridCols).toInt().coerceIn(0, gridCols - 1)
        val cy = ((pos.y / h) * gridRows).toInt().coerceIn(0, gridRows - 1)

        // Mark 3x3 brush neighborhood for natural coverage detection
        for (dy in -1..1) {
            for (dx in -1..1) {
                val nx = cx + dx
                val ny = cy + dy
                if (nx in 0 until gridCols && ny in 0 until gridRows) {
                    scratchedCells[ny * gridCols + nx] = true
                }
            }
        }
        val count = scratchedCells.count { it }
        val pct = (count * 100) / scratchedCells.size
        scratchedPercent = pct

        strokeCounter++
        if (strokeCounter % 6 == 0) {
            onScratchStroke()
        }
        if (pct >= 52 && !isRevealed) {
            onRevealReward()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Holographic Mint Scratch",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = if (isRevealed) "CLAIMED" else "$scratchedPercent%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isRevealed) BrightEmerald else NeonGold,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Interactive Scratch Card Surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .height(230.dp)
                .clip(RoundedCornerShape(26.dp))
                .onSizeChanged { cardSize = it }
                .testTag("scratch_card_canvas")
                .pointerInput(cardVersion, isRevealed) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        markScratchAt(change.position)
                    }
                }
                .pointerInput(cardVersion, isRevealed) {
                    detectTapGestures { pos ->
                        markScratchAt(pos)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Underlying Prize Layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(DeepIndigo, CardSlateElevated, Color(0xFF2D195E))
                        )
                    )
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (prizeCoins >= 500) "JACKPOT PRIZE!" else "INSTANT REWARD",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (prizeCoins >= 500) NeonGold else EmeraldMint,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "+$prizeCoins",
                        style = MaterialTheme.typography.displaySmall,
                        color = NeonGold,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "MINT COINS",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
            }

            // Holographic Foil Canvas Layer (erased via BlendMode.Clear when not revealed)
            if (!isRevealed) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                ) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                ElectricViolet,
                                NeonMagenta,
                                NeonCyan,
                                ElectricViolet
                            )
                        )
                    )
                    // Subtle metallic diagonal stripes on foil
                    val step = 28.dp.toPx()
                    var x = -size.height
                    while (x < size.width + size.height) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.14f),
                            start = Offset(x, 0f),
                            end = Offset(x + size.height, size.height),
                            strokeWidth = 6.dp.toPx()
                        )
                        x += step
                    }

                    // Erase brushed dots
                    val brushRadius = 26.dp.toPx()
                    scratchPoints.forEach { pt ->
                        drawCircle(
                            color = Color.Transparent,
                            radius = brushRadius,
                            center = Offset(pt.x, pt.y),
                            blendMode = BlendMode.Clear
                        )
                    }

                    // Outer border frame
                    drawRect(
                        color = NeonGold.copy(alpha = 0.7f),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // Helpful prompt pill only before user starts scratching
                if (scratchPoints.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = VoidObsidian.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "SCRATCH HERE TO WIN",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (isRevealed) {
                "Reward credited! Grab a fresh holographic card below."
            } else {
                stringResource(R.string.scratch_hint)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        AnimatedVisibility(
            visible = isRevealed,
            enter = fadeIn() + scaleIn()
        ) {
            Button(
                onClick = onNewCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .height(52.dp)
                    .testTag("new_scratch_card_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldMint,
                    contentColor = VoidObsidian
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.new_scratch_card),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
