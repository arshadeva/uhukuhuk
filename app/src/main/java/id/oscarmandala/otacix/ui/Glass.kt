package id.oscarmandala.otacix.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import id.oscarmandala.otacix.util.Tier

val LocalTier = staticCompositionLocalOf { Tier.FULL }
val LocalTilt = compositionLocalOf<State<Offset>> { mutableStateOf(Offset.Zero) }

/**
 * Efek "liquid glass": kaca translusen + tepi berkilau.
 * Di HP spek atas (Tier.FULL) kilau spekular ikut bergeser mengikuti kemiringan HP (sensor gravitasi).
 */
@Composable
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(26.dp),
    tint: Color = Color.White,
    strength: Float = 1f,
    base: Color = Color.Transparent,
): Modifier {
    val tier = LocalTier.current
    val tilt = LocalTilt.current
    val sheen = if (tier == Tier.FULL) Modifier.drawWithContent {
        drawContent()
        val t = tilt.value
        val c = Offset(size.width * (0.5f + t.x * 0.7f), size.height * (0.15f + t.y * 0.6f))
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.24f), Color.Transparent),
                center = c, radius = size.maxDimension * 0.75f,
            )
        )
    } else Modifier
    return this
        .clip(shape)
        .background(base)
        .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.20f * strength), tint.copy(alpha = 0.06f * strength))))
        .then(sheen)
        .border(
            1.dp,
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.24f))),
            shape,
        )
}

/** Klik dengan efek tekan (mengecil sedikit), tanpa ripple. */
@Composable
fun Modifier.tap(onClick: () -> Unit): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.96f else 1f, tween(120), label = "tap")
    return this
        .graphicsLayer { scaleX = s; scaleY = s }
        .clickable(interactionSource = src, indication = null, onClick = onClick)
}

/** Kemiringan HP (-1..1). Hanya aktif jika enabled (Tier.FULL). */
@Composable
fun rememberTilt(enabled: Boolean): State<Offset> {
    val ctx = LocalContext.current
    val st = remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(enabled) {
        if (!enabled) {
            st.value = Offset.Zero
            return@DisposableEffect onDispose { }
        }
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_GRAVITY) ?: sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor == null) return@DisposableEffect onDispose { }
        val l = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val x = (-e.values[0] / 9.81f).coerceIn(-1f, 1f)
                val y = (e.values[2] / 9.81f - 0.35f).coerceIn(-1f, 1f)
                val o = st.value
                st.value = Offset(o.x + (x - o.x) * 0.15f, o.y + (y - o.y) * 0.15f)
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }
        sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(l) }
    }
    return st
}

/** Latar "aurora" biru-kuning. [accent] = warna dominan sampul lagu yang sedang diputar. */
@Composable
fun AuroraBackground(accent: Color) {
    val tier = LocalTier.current
    val acc by animateColorAsState(accent, tween(900), label = "accent")
    val phase: State<Float> = if (tier == Tier.FULL) {
        rememberInfiniteTransition(label = "aurora").animateFloat(
            0f, 1f, infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Reverse), label = "p"
        )
    } else remember { mutableStateOf(0f) }

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val p = phase.value
        drawRect(Brush.verticalGradient(listOf(Color(0xFF061435), Navy)))
        fun blob(c: Color, cx: Float, cy: Float, r: Float) =
            drawRect(Brush.radialGradient(listOf(c, Color.Transparent), center = Offset(cx, cy), radius = r))
        blob(BlueBright.copy(alpha = 0.50f), w * (0.12f + 0.10f * p), h * (0.08f + 0.05f * p), w * 0.95f)
        blob(Yellow.copy(alpha = 0.20f), w * (0.95f - 0.10f * p), h * (0.22f + 0.04f * p), w * 0.60f)
        blob(acc.copy(alpha = 0.34f), w * (0.30f + 0.12f * p), h * (0.95f - 0.06f * p), w * 0.95f)
        blob(Blue.copy(alpha = 0.55f), w * 0.90f, h * (0.80f + 0.05f * p), w * 0.70f)
    }
}
