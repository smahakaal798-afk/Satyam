package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AmberGold
import com.example.ui.theme.BorderViolet
import com.example.ui.theme.BrightEmerald
import com.example.ui.theme.CardSlate
import com.example.ui.theme.DeepGold
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonGold
import com.example.ui.theme.RoseAlert
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidObsidian
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private data class FloatingTapDelta(
    val id: Long,
    val startX: Float,
    val startY: Float,
    val label: String,
    val createdNanos: Long
)

@Composable
fun TapToEarnScreen(
    energy: Int,
    maxEnergy: Int,
    tapPower: Long,
    totalTaps: Long,
    onCoinTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val coinScale = remember { Animatable(1f) }
    var tiltX by remember { mutableStateOf(0f) }
    var tiltY by remember { mutableStateOf(0f) }
    var ringProgress by remember { mutableStateOf(0f) }
    val floatingDeltas = remember { mutableStateListOf<FloatingTapDelta>() }
    var deltaIdCounter by remember { mutableLongStateOf(0L) }
    var frameNanos by remember { mutableLongStateOf(0L) }

    LaunchedEffect(floatingDeltas.size) {
        if (floatingDeltas.isNotEmpty()) {
            while (floatingDeltas.isNotEmpty()) {
                val now = withFrameNanos { it }
                frameNanos = now
                floatingDeltas.removeAll { (now - it.createdNanos) > 750_000_000L }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Hint Card
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
                        imageVector = Icons.Rounded.TouchApp,
                        contentDescription = null,
                        tint = NeonGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+$tapPower Coins / Tap",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonGold,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "$totalTaps Taps",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3D Glowing Gold Coin Interactive Target
        Box(
            modifier = Modifier
                .size(270.dp)
                .testTag("tap_gold_coin")
                .pointerInput(energy) {
                    detectTapGestures { offset ->
                        onCoinTap()
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = ((offset.x - cx) / cx).coerceIn(-1f, 1f)
                        val dy = ((offset.y - cy) / cy).coerceIn(-1f, 1f)

                        if (energy > 0) {
                            val now = System.nanoTime()
                            floatingDeltas.add(
                                FloatingTapDelta(
                                    id = ++deltaIdCounter,
                                    startX = offset.x,
                                    startY = offset.y,
                                    label = "+$tapPower",
                                    createdNanos = now
                                )
                            )
                            tiltX = -dy * 12f
                            tiltY = dx * 12f
                            scope.launch {
                                coinScale.snapTo(0.91f)
                                coinScale.animateTo(
                                    targetValue = 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                                tiltX = 0f
                                tiltY = 0f
                            }
                            scope.launch {
                                val anim = Animatable(0f)
                                anim.animateTo(1f, tween(380)) {
                                    ringProgress = value
                                }
                                ringProgress = 0f
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = coinScale.value
                        scaleY = coinScale.value
                        rotationX = tiltX
                        rotationY = tiltY
                        cameraDistance = 14f * density
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = size.minDimension * 0.41f

                // Ambient Aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NeonGold.copy(alpha = if (energy > 0) 0.42f else 0.14f),
                            AmberGold.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.5f
                    ),
                    radius = size.minDimension * 0.5f,
                    center = center
                )

                // Expanding Tap Pulse Ring
                if (ringProgress > 0f) {
                    val pulseR = baseRadius * (1f + ringProgress * 0.34f)
                    drawCircle(
                        color = NeonGold.copy(alpha = (1f - ringProgress) * 0.7f),
                        radius = pulseR,
                        center = center,
                        style = Stroke(width = 4.dp.toPx() * (1f - ringProgress * 0.5f))
                    )
                }

                // 3D Coin Depth Shadow / Bottom Rim
                drawCircle(
                    color = Color(0xFF7A4100),
                    radius = baseRadius,
                    center = center + Offset(0f, 9.dp.toPx())
                )

                // Outer Metallic Beveled Gold Rim
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFFF3B0), NeonGold, DeepGold, AmberGold),
                        start = Offset(center.x - baseRadius, center.y - baseRadius),
                        end = Offset(center.x + baseRadius, center.y + baseRadius)
                    ),
                    radius = baseRadius,
                    center = center
                )

                // Milled Coin Edge Ridges
                val ridgeCount = 36
                for (i in 0 until ridgeCount) {
                    val angle = (i * (360f / ridgeCount)) * (PI / 180.0)
                    val r1 = baseRadius * 0.91f
                    val r2 = baseRadius * 0.97f
                    drawLine(
                        color = DeepGold.copy(alpha = 0.5f),
                        start = Offset(center.x + (cos(angle) * r1).toFloat(), center.y + (sin(angle) * r1).toFloat()),
                        end = Offset(center.x + (cos(angle) * r2).toFloat(), center.y + (sin(angle) * r2).toFloat()),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // Recessed Inner Gold Disc
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFE57F), AmberGold, Color(0xFFC97808)),
                        center = center - Offset(baseRadius * 0.25f, baseRadius * 0.25f),
                        radius = baseRadius * 1.15f
                    ),
                    radius = baseRadius * 0.83f,
                    center = center
                )

                drawCircle(
                    color = Color(0xFFFFF8D6).copy(alpha = 0.55f),
                    radius = baseRadius * 0.83f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Embossed 8-Point Mint Star Emblem inside Coin
                val starPath = Path()
                val outerR = baseRadius * 0.48f
                val innerR = baseRadius * 0.22f
                for (i in 0 until 16) {
                    val r = if (i % 2 == 0) outerR else innerR
                    val rad = (i * 22.5 - 90.0) * (PI / 180.0)
                    val px = center.x + (cos(rad) * r).toFloat()
                    val py = center.y + (sin(rad) * r).toFloat()
                    if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
                }
                starPath.close()
                drawPath(
                    path = starPath,
                    color = DeepGold.copy(alpha = 0.45f)
                )
                drawPath(
                    path = starPath,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFFFCE6), NeonGold)
                    ),
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // Floating "+10" particles
            val density = LocalDensity.current
            floatingDeltas.forEach { item ->
                val ageSec = ((frameNanos - item.createdNanos).coerceAtLeast(0L)) / 1_000_000_000f
                val progress = (ageSec / 0.75f).coerceIn(0f, 1f)
                val risePx = with(density) { (78.dp.toPx()) * progress }
                val alpha = (1f - progress).coerceIn(0f, 1f)

                Text(
                    text = item.label,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonGold.copy(alpha = alpha),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset {
                            IntOffset(
                                x = (item.startX - 24f).roundToInt(),
                                y = (item.startY - 36f - risePx).roundToInt()
                            )
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.tap_to_earn_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Energy Bar Card
        val energyFraction = (energy.toFloat() / maxEnergy.toFloat()).coerceIn(0f, 1f)
        val energyColor = if (energy > 20) BrightEmerald else RoseAlert

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            shape = RoundedCornerShape(20.dp),
            color = CardSlate,
            border = BorderStroke(1.dp, BorderViolet)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = energyColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.energy_label),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "$energy / $maxEnergy",
                        style = MaterialTheme.typography.labelLarge,
                        color = energyColor,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { energyFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(50)),
                    color = energyColor,
                    trackColor = DeepIndigo
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (energy < maxEnergy) {
                        "Recharging automatically (+1 Energy / sec)"
                    } else {
                        "Energy Full — Tap rapidly to mine Coins!"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (energy < maxEnergy) EmeraldMint else TextSecondary
                )
            }
        }
    }
}
