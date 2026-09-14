package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.*
import kotlin.random.Random

enum class ConfettiShape {
    RECTANGLE,
    CIRCLE,
    STAR,
    RIBBON
}

data class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var width: Float,
    var height: Float,
    var color: Color,
    var shape: ConfettiShape,
    var rotation: Float,
    var rotationSpeed: Float,
    var flipAngle: Float,
    var flipSpeed: Float,
    var alpha: Float = 1f,
    var life: Float = 1f,
    var decay: Float = 0.003f
)

private val ConfettiPalette = listOf(
    Color(0xFFFFD700), // Pure Gold
    Color(0xFFFFA000), // Amber
    Color(0xFF10B981), // Emerald
    Color(0xFF06B6D4), // Cyan
    Color(0xFF8B5CF6), // Violet
    Color(0xFFF43F5E), // Rose
    Color(0xFF3B82F6), // Sky Blue
    Color(0xFFFF6B6B), // Coral
    Color(0xFFF59E0B), // Golden Sunset
    Color(0xFFEC4899)  // Pink
)

/**
 * High-performance, physics-driven celebratory confetti animation effect.
 * Emits vibrant particles that burst upwards from both corners and center,
 * then gently flutter downward with realistic 3D tumbling and wind drift.
 */
@Composable
fun CelebrationConfettiOverlay(
    modifier: Modifier = Modifier,
    triggerKey: Any? = Unit,
    particleCount: Int = 110,
    burstDurationMs: Long = 4500
) {
    val context = LocalContext.current

    // Trigger haptic celebratory pulse on launch
    LaunchedEffect(triggerKey) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                val pattern = longArrayOf(0, 80, 50, 100, 60, 140)
                val amplitudes = intArrayOf(0, 180, 0, 220, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val pattern = longArrayOf(0, 80, 50, 100, 60, 140)
                    val amplitudes = intArrayOf(0, 180, 0, 220, 0, 255)
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(250)
                }
            }
        } catch (_: Exception) {
            // Graceful fallback if vibrator is unavailable
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidth = constraints.maxWidth.toFloat().coerceAtLeast(400f)
        val screenHeight = constraints.maxHeight.toFloat().coerceAtLeast(800f)

        val particles = remember(triggerKey) {
            generateConfettiParticles(particleCount, screenWidth, screenHeight)
        }

        var frameTick by remember(triggerKey) { mutableLongStateOf(0L) }

        LaunchedEffect(triggerKey) {
            val startTime = System.currentTimeMillis()
            var lastTime = System.nanoTime()

            while (isActive && (System.currentTimeMillis() - startTime < burstDurationMs + 2000)) {
                withFrameNanos { now ->
                    val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    lastTime = now

                    val gravity = 900f // pixels / s^2
                    val wind = sin(now / 500_000_000.0).toFloat() * 60f

                    particles.forEach { p ->
                        p.vy += gravity * dt
                        p.vx += wind * dt * 0.1f
                        p.x += p.vx * dt
                        p.y += p.vy * dt
                        p.rotation += p.rotationSpeed * dt
                        p.flipAngle += p.flipSpeed * dt
                        p.life -= p.decay
                        p.alpha = (p.life).coerceIn(0f, 1f)
                    }
                    frameTick = now
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Read frameTick to trigger recomposition on animation frames
            if (frameTick >= 0) {
                particles.forEach { p ->
                    if (p.alpha > 0.02f && p.y < size.height + 40f) {
                        drawConfettiParticle(p)
                    }
                }
            }
        }
    }
}

private fun generateConfettiParticles(
    count: Int,
    screenWidth: Float,
    screenHeight: Float
): List<ConfettiParticle> {
    val rng = Random(System.currentTimeMillis())
    return (0 until count).map { i ->
        // Alternate origins: left cannon, right cannon, and top center shower
        val originType = i % 3
        val startX: Float
        val startY: Float
        val vx: Float
        val vy: Float

        when (originType) {
            0 -> {
                // Bottom-left cannon blasting up & right
                startX = rng.nextFloat() * 80f
                startY = screenHeight * 0.75f + rng.nextFloat() * 100f
                val angle = (-rng.nextFloat() * 45f - 25f) * (PI.toFloat() / 180f)
                val speed = 650f + rng.nextFloat() * 550f
                vx = cos(angle) * speed
                vy = sin(angle) * speed
            }
            1 -> {
                // Bottom-right cannon blasting up & left
                startX = screenWidth - rng.nextFloat() * 80f
                startY = screenHeight * 0.75f + rng.nextFloat() * 100f
                val angle = (-180f + (rng.nextFloat() * 45f + 25f)) * (PI.toFloat() / 180f)
                val speed = 650f + rng.nextFloat() * 550f
                vx = cos(angle) * speed
                vy = sin(angle) * speed
            }
            else -> {
                // Center-top aerial burst
                startX = screenWidth * 0.5f + (rng.nextFloat() - 0.5f) * (screenWidth * 0.6f)
                startY = screenHeight * 0.15f + rng.nextFloat() * 80f
                val angle = (rng.nextFloat() * 360f) * (PI.toFloat() / 180f)
                val speed = 250f + rng.nextFloat() * 400f
                vx = cos(angle) * speed
                vy = sin(angle) * speed - 200f
            }
        }

        val shape = when (rng.nextInt(4)) {
            0 -> ConfettiShape.RECTANGLE
            1 -> ConfettiShape.CIRCLE
            2 -> ConfettiShape.STAR
            else -> ConfettiShape.RIBBON
        }

        val baseSize = 14f + rng.nextFloat() * 16f
        val w = if (shape == ConfettiShape.RIBBON) baseSize * 0.5f else baseSize
        val h = if (shape == ConfettiShape.RIBBON) baseSize * 2.2f else baseSize

        ConfettiParticle(
            x = startX,
            y = startY,
            vx = vx,
            vy = vy,
            width = w,
            height = h,
            color = ConfettiPalette[rng.nextInt(ConfettiPalette.size)],
            shape = shape,
            rotation = rng.nextFloat() * 360f,
            rotationSpeed = (rng.nextFloat() - 0.5f) * 480f,
            flipAngle = rng.nextFloat() * 360f,
            flipSpeed = (rng.nextFloat() * 300f + 150f),
            life = 1f,
            decay = 0.0018f + rng.nextFloat() * 0.002f
        )
    }
}

private fun DrawScope.drawConfettiParticle(p: ConfettiParticle) {
    val flipScale = cos(p.flipAngle * (PI / 180f)).toFloat().absoluteValue.coerceIn(0.1f, 1f)
    val effectiveWidth = p.width * flipScale
    val particleColor = p.color.copy(alpha = p.alpha)

    rotate(degrees = p.rotation, pivot = Offset(p.x, p.y)) {
        when (p.shape) {
            ConfettiShape.RECTANGLE, ConfettiShape.RIBBON -> {
                drawRect(
                    color = particleColor,
                    topLeft = Offset(p.x - effectiveWidth / 2, p.y - p.height / 2),
                    size = Size(effectiveWidth, p.height)
                )
            }
            ConfettiShape.CIRCLE -> {
                drawOval(
                    color = particleColor,
                    topLeft = Offset(p.x - effectiveWidth / 2, p.y - p.height / 2),
                    size = Size(effectiveWidth, p.height)
                )
            }
            ConfettiShape.STAR -> {
                drawStar(
                    center = Offset(p.x, p.y),
                    radius = (p.width / 2) * flipScale,
                    color = particleColor
                )
            }
        }
    }
}

private fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    if (radius <= 1f) return
    val path = Path()
    val points = 5
    val innerRadius = radius * 0.45f
    val step = PI / points

    for (i in 0 until 2 * points) {
        val r = if (i % 2 == 0) radius else innerRadius
        val angle = i * step - (PI / 2)
        val x = center.x + (cos(angle) * r).toFloat()
        val y = center.y + (sin(angle) * r).toFloat()
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    drawPath(path = path, color = color)
}

/**
 * Animated celebratory header badge displayed specifically for perfect scores (100%).
 * Features a spring bounce-in effect, pulsing golden halo, gleaming gradient text,
 * and animated orbiting sparkle stars.
 */
@Composable
fun PerfectScoreCelebrationBadge(
    modifier: Modifier = Modifier,
    onReplayCelebration: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "celebration_halo")

    // Pulsing radial aura scale
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )

    // Shimmer beam sweep
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -200f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_beam"
    )

    // Orbiting sparkle star rotation
    val starRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "star_rotation"
    )

    // Bouncy scale-in on first appearance
    var isAppeared by remember { mutableStateOf(false) }
    val entryScale by animateFloatAsState(
        targetValue = if (isAppeared) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "entry_bounce"
    )

    LaunchedEffect(Unit) {
        delay(60)
        isAppeared = true
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(entryScale)
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFFFFD700),
                        Color(0xFFFFA000),
                        Color(0xFF10B981),
                        Color(0xFFFFD700)
                    )
                ),
                shape = RoundedCornerShape(26.dp)
            ),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFBEB) // Warm golden champagne tint
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle celebratory golden glow behind card
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(26.dp))
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            Color(0xFFFFD700).copy(alpha = 0.28f * haloPulse),
                            Color(0xFFFFA000).copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(size.width / 2, size.height * 0.35f),
                        radius = size.width * 0.65f * haloPulse
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Golden Trophy with orbiting sparkle stars & pulsating halo
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing golden aura circle
                    Box(
                        modifier = Modifier
                            .size(96.dp * haloPulse)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color(0xFFFFD700).copy(alpha = 0.45f),
                                        Color(0xFFF59E0B).copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Core Golden Trophy Orb
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFFFFE082),
                                        Color(0xFFFFB300),
                                        Color(0xFFF57C00)
                                    )
                                )
                            )
                            .border(2.5.dp, Color(0xFFFFF8E1), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Perfect Score Trophy",
                            tint = Color.White,
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    // Orbiting Star 1
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(starRotation),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB300),
                            modifier = Modifier
                                .size(18.dp)
                                .scale(haloPulse)
                        )
                    }

                    // Orbiting Star 2 (offset 180 degrees)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(starRotation + 180f),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Congratulatory Headline Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFFEF3C7),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "PERFECT SCORE ACHIEVED",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFB45309),
                            letterSpacing = 1.sp
                        )
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Flawless Mastery!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF78350F)
                )

                Text(
                    text = "You answered every single question correctly with 100% precision!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF92400E),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Replay Confetti Effect Button
                OutlinedButton(
                    onClick = onReplayCelebration,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFB45309)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Blast Confetti Again",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
