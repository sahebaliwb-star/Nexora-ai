package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.NexoraOrbState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldBullish
import com.example.ui.theme.MutedSlateText
import com.example.ui.theme.VioletNeon

@Composable
fun NexoraNeuralOrb(
    state: NexoraOrbState,
    rmsLevel: Float,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 196.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "nexora_orb_anim")

    val slowRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    NexoraOrbState.THINKING -> 2200
                    NexoraOrbState.WORKING -> 1500
                    NexoraOrbState.LISTENING -> 3600
                    else -> 9000
                },
                easing = LinearEasing
            )
        ),
        label = "orb_rotation"
    )

    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    NexoraOrbState.WORKING -> 1800
                    NexoraOrbState.THINKING -> 2800
                    else -> 12000
                },
                easing = LinearEasing
            )
        ),
        label = "orb_counter_rotation"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    NexoraOrbState.LISTENING -> 550
                    NexoraOrbState.SPEAKING -> 650
                    NexoraOrbState.WORKING -> 450
                    else -> 1800
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    val primaryGlow = when (state) {
        NexoraOrbState.IDLE -> ElectricCyan
        NexoraOrbState.LISTENING -> ElectricCyan
        NexoraOrbState.THINKING -> VioletNeon
        NexoraOrbState.WORKING -> EmeraldBullish
        NexoraOrbState.SPEAKING -> Color(0xFF38BDF8)
        NexoraOrbState.PAUSED -> MutedSlateText
    }

    val secondaryGlow = when (state) {
        NexoraOrbState.WORKING -> ElectricCyan
        NexoraOrbState.THINKING -> ElectricCyan
        NexoraOrbState.PAUSED -> MutedSlateText.copy(alpha = 0.4f)
        else -> VioletNeon
    }

    val statusText = when (state) {
        NexoraOrbState.IDLE -> "TAP TO SPEAK • প্রস্তুত"
        NexoraOrbState.LISTENING -> "LISTENING • শুনছি..."
        NexoraOrbState.THINKING -> "THINKING • বিশ্লেষণ চলছে..."
        NexoraOrbState.WORKING -> "WORKING • কাজ সম্পন্ন হচ্ছে..."
        NexoraOrbState.SPEAKING -> "SPEAKING • উত্তর বলছি..."
        NexoraOrbState.PAUSED -> "PAUSED • AI বন্ধ আছে"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .testTag("nexora_ai_orb")
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClickLabel = "Activate NEXORA AI Voice",
                    onClick = onTap
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val baseRadius = this.size.minDimension / 2f
                val voiceBoost = if (state == NexoraOrbState.LISTENING) rmsLevel * 0.14f else 0f
                val effectivePulse = (pulse + voiceBoost).coerceAtMost(1.2f)

                // Ambient radial aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryGlow.copy(alpha = 0.34f),
                            secondaryGlow.copy(alpha = 0.14f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = baseRadius * effectivePulse
                    ),
                    radius = baseRadius * effectivePulse,
                    center = center
                )

                // Outer orbital arc ring
                rotate(degrees = slowRotation, pivot = center) {
                    val ringRadius = baseRadius * 0.86f
                    val arcSize = Size(ringRadius * 2f, ringRadius * 2f)
                    val topLeft = Offset(center.x - ringRadius, center.y - ringRadius)
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                primaryGlow,
                                secondaryGlow,
                                Color.Transparent
                            ),
                            center = center
                        ),
                        startAngle = 0f,
                        sweepAngle = 210f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Inner counter-rotating neural ring
                rotate(degrees = counterRotation, pivot = center) {
                    val innerRadius = baseRadius * 0.74f
                    val arcSize = Size(innerRadius * 2f, innerRadius * 2f)
                    val topLeft = Offset(center.x - innerRadius, center.y - innerRadius)
                    drawArc(
                        color = secondaryGlow.copy(alpha = 0.85f),
                        startAngle = 40f,
                        sweepAngle = 110f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = primaryGlow.copy(alpha = 0.75f),
                        startAngle = 220f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Live audio wave ring when listening or speaking
                if (state == NexoraOrbState.LISTENING || state == NexoraOrbState.SPEAKING) {
                    val waveRadius = baseRadius * (0.64f + (effectivePulse - 0.92f) * 0.7f)
                    drawCircle(
                        color = if (state == NexoraOrbState.LISTENING) ElectricCyan.copy(alpha = 0.45f) else AmberWarning.copy(alpha = 0.35f),
                        radius = waveRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // Central Metallic NEXORA "N" Neural Core Logo
            Image(
                painter = painterResource(id = R.drawable.img_nexora_logo_transparent),
                contentDescription = "NEXORA AI Neural Core",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size * 0.62f)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = statusText,
            style = MaterialTheme.typography.labelMedium,
            color = primaryGlow
        )
    }
}
