package id.oscarmandala.otacix.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Warna diambil dari logo: biru #064A94 + kuning #FDC12B
val Navy = Color(0xFF040B1E)
val NavyMid = Color(0xFF081C48)
val Blue = Color(0xFF064A94)
val BlueBright = Color(0xFF2B7BFF)
val Yellow = Color(0xFFFDC12B)
val YellowSoft = Color(0xFFFFE08A)

@Composable
fun OtacixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Yellow, onPrimary = Navy, secondary = BlueBright,
            background = Navy, surface = NavyMid, onSurface = Color.White, onBackground = Color.White,
        ),
        content = content,
    )
}
