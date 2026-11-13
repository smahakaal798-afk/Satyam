package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DEFAULT_WHEEL_SEGMENTS
import com.example.model.WheelSegment
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BorderViolet
import com.example.ui.theme.BrightEmerald
import com.example.ui.theme.CardSlate
import com.example.ui.theme.CardSlateElevated
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidObsidian
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun LuckySpinScreen(
    freeSpinsLeft: Int,
    maxDailySpins: Int,
    resetCountdown: String,
    adCooldownSeconds: Int,
    isSpinning: Boolean,
    isWatchingAd: Boolean,
    adProgress: Float,
    onRequestSpin: suspend () -> WheelSegment?,
    onWheelTick: () -> Unit,
    onSpinComplete: (WheelSegment) -> Unit,
    onWatchAd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val segments = DEFAULT_WHEEL_SEGMENTS
    val sweepAngle = 360f / segments.size
    val rotationAnim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var lastPegIndex by remember { mutableIntStateOf(0) }
    var ledPulse by remember { mutableStateOf(false) }

    LaunchedEffect(isSpinning) {
        while (true) {
            ledPulse = !ledPulse
            delay(if (isSpinning) 160L else 550L)
        }
    }

    val textPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status strip
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
                        tint = NeonGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$freeSpinsLeft / $maxDailySpins Free Spins",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (freeSpinsLeft > 0) NeonGold else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reset $resetCountdown",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Spin Wheel Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 330.dp)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val diameter = size.minDimension
                val radius = diameter / 2f
                val center = Offset(size.width / 2f, size.height / 2f)
                val wheelRadius = radius * 0.87f

                // Outer Neon Aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonGold.copy(alpha = 0.28f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )

                // Outer Metallic Bezel
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF3E3580), DeepIndigo, Color(0xFF282257))
                    ),
                    radius = radius * 0.94f,
                    center = center
                )
                drawCircle(
                    color = NeonGold.copy(alpha = 0.65f),
                    radius = radius * 0.94f,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Twinkling Rim LED Dots
                val ledCount = 16
                for (i in 0 until ledCount) {
                    val angleRad = (i * (360f / ledCount) + rotationAnim.value * 0.25f) * (PI / 180.0)
                    val ledDist = radius * 0.905f
                    val lx = center.x + (cos(angleRad) * ledDist).toFloat()
                    val ly = center.y + (sin(angleRad) * ledDist).toFloat()
                    val isLit = (i % 2 == 0) == ledPulse
                    drawCircle(
                        color = if (isLit) NeonGold else BrightEmerald,
                        radius = if (isLit) 4.5.dp.toPx() else 3.dp.toPx(),
                        center = Offset(lx, ly)
                    )
                }

                // Rotating Wheel Wedges
                rotate(degrees = rotationAnim.value, pivot = center) {
                    segments.forEachIndexed { index, segment ->
                        val startAngle = index * sweepAngle - (sweepAngle / 2f) - 90f
                        drawArc(
                            color = segment.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            topLeft = Offset(center.x - wheelRadius, center.y - wheelRadius),
                            size = Size(wheelRadius * 2f, wheelRadius * 2f)
                        )
                        drawArc(
                            color = VoidObsidian.copy(alpha = 0.65f),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            topLeft = Offset(center.x - wheelRadius, center.y - wheelRadius),
                            size = Size(wheelRadius * 2f, wheelRadius * 2f),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Segment Label
                        val midAngleDeg = index * sweepAngle - 90f
                        val midAngleRad = midAngleDeg * (PI / 180.0)
                        val labelRadius = wheelRadius * 0.64f
                        val tx = center.x + (cos(midAngleRad) * labelRadius).toFloat()
                        val ty = center.y + (sin(midAngleRad) * labelRadius).toFloat()

                        drawContext.canvas.nativeCanvas.apply {
                            save()
                            translate(tx, ty)
                            rotate(midAngleDeg + 90f)
                            textPaint.color = if (segment.isJackpot || segment.color == NeonGold || segment.color == EmeraldMint || segment.color == AmberGold) {
                                VoidObsidian.toArgb()
                            } else {
                                Color.White.toArgb()
                            }
                            textPaint.textSize = (wheelRadius * 0.14f).coerceIn(28f, 44f)
                            drawText(segment.shortLabel, 0f, textPaint.textSize * 0.35f, textPaint)
                            restore()
                        }
                    }

                    // Inner ring gloss
                    drawCircle(
                        color = Color.White.copy(alpha = 0.14f),
                        radius = wheelRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // Center Hub
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonGold, AmberGold, Color(0xFF8C4A00)),
                        center = center,
                        radius = wheelRadius * 0.22f
                    ),
                    radius = wheelRadius * 0.21f,
                    center = center
                )
                drawCircle(
                    color = VoidObsidian,
                    radius = wheelRadius * 0.15f,
                    center = center
                )
                drawContext.canvas.nativeCanvas.apply {
                    textPaint.color = NeonGold.toArgb()
                    textPaint.textSize = (wheelRadius * 0.11f).coerceIn(24f, 34f)
                    drawText("MINT", center.x, center.y + textPaint.textSize * 0.35f, textPaint)
                }

                // Top Gold Pointer Triangle
                val pointerPath = Path().apply {
                    moveTo(center.x, center.y - wheelRadius + 16.dp.toPx())
                    lineTo(center.x - 14.dp.toPx(), center.y - wheelRadius - 14.dp.toPx())
                    lineTo(center.x + 14.dp.toPx(), center.y - wheelRadius - 14.dp.toPx())
                    close()
                }
                drawPath(path = pointerPath, color = NeonGold)
                drawPath(path = pointerPath, color = VoidObsidian, style = Stroke(width = 2.dp.toPx()))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Spin Action Button
        Button(
            onClick = {
                scope.launch {
                    val chosen = onRequestSpin() ?: return@launch
                    val current = rotationAnim.value
                    val currentMod = ((current % 360f) + 360f) % 360f
                    val targetCenterOffset = (360f - (chosen.id * sweepAngle)) % 360f
                    val jitter = Random.nextFloat() * 22f - 11f
                    var delta = targetCenterOffset - currentMod + jitter
                    if (delta <= 0f) delta += 360f
                    val totalTarget = current + (6f * 360f) + delta

                    lastPegIndex = floor((current + sweepAngle / 2f) / sweepAngle).toInt()

                    rotationAnim.animateTo(
                        targetValue = totalTarget,
                        animationSpec = tween(
                            durationMillis = 4100,
                            easing = CubicBezierEasing(0.10f, 0.92f, 0.18f, 1.0f)
                        )
                    ) {
                        val pegIndex = floor((value + sweepAngle / 2f) / sweepAngle).toInt()
                        if (pegIndex != lastPegIndex) {
                            lastPegIndex = pegIndex
                            onWheelTick()
                        }
                    }
                    onSpinComplete(chosen)
                }
            },
            enabled = !isSpinning && !isWatchingAd,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .height(54.dp)
                .testTag("spin_wheel_button"),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonGold,
                contentColor = VoidObsidian,
                disabledContainerColor = CardSlateElevated,
                disabledContentColor = TextSecondary
            )
        ) {
            Text(
                text = if (isSpinning) stringResource(R.string.spinning) else stringResource(R.string.spin_now),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Simulated Rewarded Ad Section
        if (isWatchingAd) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp),
                shape = RoundedCornerShape(16.dp),
                color = CardSlateElevated,
                border = BorderStroke(1.dp, EmeraldMint.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sponsor Reward Video Playing…",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldMint
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { adProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = BrightEmerald,
                        trackColor = DeepIndigo
                    )
                }
            }
        } else {
            OutlinedButton(
                onClick = onWatchAd,
                enabled = !isSpinning && adCooldownSeconds == 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .height(48.dp)
                    .testTag("watch_ad_button"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = 1.2.dp,
                    color = if (adCooldownSeconds == 0) EmeraldMint else BorderViolet
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (adCooldownSeconds == 0) EmeraldMint else TextSecondary
                ),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (adCooldownSeconds > 0) {
                        "Bonus Ad Ready in ${adCooldownSeconds}s"
                    } else {
                        stringResource(R.string.watch_ad_spin)
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
