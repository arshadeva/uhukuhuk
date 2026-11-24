package id.oscarmandala.otacix.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.oscarmandala.otacix.R
import id.oscarmandala.otacix.util.Tier
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Splash screen animasi (menyambung splash sistem): logo membesar pegas, nama + "by Oscar Mandala", lalu memudar. */
@Composable
fun SplashOverlay(onDone: () -> Unit) {
    val full = LocalTier.current == Tier.FULL
    val scale = remember { Animatable(0.65f) }
    val appear = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow)) }
            launch { appear.animateTo(1f, tween(if (full) 600 else 250)) }
        }
        delay(if (full) 900L else 450L)
        fade.animateTo(0f, tween(if (full) 450 else 200))
        onDone()
    }

    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = fade.value }
            .background(Navy)
            .clickable(remember { MutableInteractionSource() }, null) { },
        contentAlignment = Alignment.Center,
    ) {
        AuroraBackground(BlueBright)
        Column(
            Modifier.graphicsLayer { alpha = appear.value },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(176.dp)
                    .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                    .clip(RoundedCornerShape(44.dp)).background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painterResource(R.drawable.logo), "Logo Otacix",
                    Modifier.fillMaxSize().padding(20.dp), contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.height(26.dp))
            Text("OTACIX", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 6.sp)
            Spacer(Modifier.height(6.dp))
            Text("by Oscar Mandala", color = Yellow, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        }
    }
}
