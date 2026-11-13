package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.ui.theme.BrightEmerald
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonMagenta
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val xFraction: Float,
    val startYFraction: Float,
    val vx: Float,
    val vy: Float,
    val sizePx: Float,
    val rotationSpeed: Float,
    val initialRotation: Float,
    val wobblePhase: Float,
    val color: Color,
    val isCircle: Boolean
)

@Composable
fun ConfettiOverlay(
    triggerId: Long,
    modifier: Modifier = Modifier
) {
    if (triggerId <= 0L) return

    var particles by remember { mutableStateOf<List<ConfettiParticle>>(emptyList()) }
    var elapsedSec by remember { mutableLongStateOf(0L) }
    var animSeconds by remember { mutableStateOf(0f) }

    LaunchedEffect(triggerId) {
        val palette = listOf(NeonGold, BrightEmerald, NeonMagenta, NeonCyan, ElectricViolet, Color.White)
        particles = List(72) {
            ConfettiParticle(
                xFraction = Random.nextFloat() * 0.9f + 0.05f,
                startYFraction = -Random.nextFloat() * 0.25f,
                vx = (Random.nextFloat() - 0.5f) * 0.24f,
                vy = Random.nextFloat() * 0.42f + 0.36f,
                sizePx = Random.nextFloat() * 14f + 10f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 420f,
                initialRotation = Random.nextFloat() * 360f,
                wobblePhase = Random.nextFloat() * 6.28f,
                color = palette[Random.nextInt(palette.size)],
                isCircle = Random.nextBoolean()
            )
        }
        val startNanos = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val t = (now - startNanos) / 1_000_000_000f
            animSeconds = t
            elapsedSec = t.toLong()
            if (t >= 2.8f) {
                particles = emptyList()
                break
            }
        }
    }

    if (particles.isEmpty()) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val alpha = (1f - (animSeconds / 2.8f)).coerceIn(0f, 1f)
        particles.forEach { p ->
            val x = (p.xFraction + p.vx * animSeconds) * size.width +
                sin(animSeconds * 5f + p.wobblePhase) * 18f
            val y = (p.startYFraction + p.vy * animSeconds + 0.14f * animSeconds * animSeconds) * size.height
            if (y <= size.height + 40f) {
                val rot = p.initialRotation + p.rotationSpeed * animSeconds
                withTransform({
                    translate(x, y)
                    rotate(rot, pivot = Offset.Zero)
                }) {
                    if (p.isCircle) {
                        drawCircle(
                            color = p.color.copy(alpha = alpha),
                            radius = p.sizePx * 0.42f,
                            center = Offset.Zero
                        )
                    } else {
                        drawRect(
                            color = p.color.copy(alpha = alpha),
                            topLeft = Offset(-p.sizePx / 2f, -p.sizePx * 0.3f),
                            size = Size(p.sizePx, p.sizePx * 0.6f)
                        )
                    }
                }
            }
        }
    }
}
