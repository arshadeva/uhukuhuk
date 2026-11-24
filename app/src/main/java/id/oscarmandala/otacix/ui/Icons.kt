package id.oscarmandala.otacix.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

private fun icon(name: String, d: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).addPath(
        pathData = PathParser().parsePathString(d).toNodes(),
        fill = SolidColor(Color.Black),
    ).build()

/** Ikon bergaya Material (path SVG), dibuat sekali lalu dipakai ulang. */
object Ic {
    val Play by lazy { icon("play", "M8,5v14l11,-7z") }
    val Pause by lazy { icon("pause", "M6,19h4V5H6v14zm8,-14v14h4V5h-4z") }
    val Next by lazy { icon("next", "M6,18l8.5,-6L6,6v12zM16,6v12h2V6h-2z") }
    val Prev by lazy { icon("prev", "M6,6h2v12H6zm3.5,6l8.5,6V6z") }
    val Shuffle by lazy { icon("shuffle", "M10.59,9.17L5.41,4 4,5.41l5.17,5.17 1.42,-1.41zM14.5,4l2.04,2.04L4,18.59 5.41,20 17.96,7.46 20,9.5V4h-5.5zM14.83,13.41l-1.41,1.41 3.13,3.13L14.5,20H20v-5.5l-2.04,2.04 -3.13,-3.13z") }
    val Repeat by lazy { icon("repeat", "M7,7h10v3l4,-4 -4,-4v3H5v6h2V7zM17,17H7v-3l-4,4 4,4v-3h12v-6h-2v4z") }
    val RepeatOne by lazy { icon("repeat_one", "M7,7h10v3l4,-4 -4,-4v3H5v6h2V7zM17,17H7v-3l-4,4 4,4v-3h12v-6h-2v4zM13,15V9h-1l-2,1v1h1.5v4H13z") }
    val Home by lazy { icon("home", "M10,20v-6h4v6h5v-8h3L12,3 2,12h3v8z") }
    val Search by lazy { icon("search", "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z") }
    val Library by lazy { icon("library", "M20,2H8C6.9,2 6,2.9 6,4v12c0,1.1 0.9,2 2,2h12c1.1,0 2,-0.9 2,-2V4c0,-1.1 -0.9,-2 -2,-2zM18,5h-3v5.5c0,1.38 -1.12,2.5 -2.5,2.5S10,11.88 10,10.5s1.12,-2.5 2.5,-2.5c0.57,0 1.08,0.19 1.5,0.51V5h4v2zM4,6H2v14c0,1.1 0.9,2 2,2h14v-2H4V6z") }
    val Person by lazy { icon("person", "M12,12c2.21,0 4,-1.79 4,-4s-1.79,-4 -4,-4 -4,1.79 -4,4 1.79,4 4,4zM12,14c-2.67,0 -8,1.34 -8,4v2h16v-2c0,-2.66 -5.33,-4 -8,-4z") }
    val Heart by lazy { icon("heart", "M12,21.35l-1.45,-1.32C5.4,15.36 2,12.28 2,8.5 2,5.42 4.42,3 7.5,3c1.74,0 3.41,0.81 4.5,2.09C13.09,3.81 14.76,3 16.5,3 19.58,3 22,5.42 22,8.5c0,3.78 -3.4,6.86 -8.55,11.54L12,21.35z") }
    val HeartOutline by lazy { icon("heart_outline", "M16.5,3c-1.74,0 -3.41,0.81 -4.5,2.09C10.91,3.81 9.24,3 7.5,3 4.42,3 2,5.42 2,8.5c0,3.78 3.4,6.86 8.55,11.54L12,21.35l1.45,-1.32C18.6,15.36 22,12.28 22,8.5 22,5.42 19.58,3 16.5,3zM12.1,18.55l-0.1,0.1 -0.1,-0.1C7.14,14.24 4,11.39 4,8.5 4,6.5 5.5,5 7.5,5c1.54,0 3.04,0.99 3.57,2.36h1.87C13.46,5.99 14.96,5 16.5,5c2,0 3.5,1.5 3.5,3.5 0,2.89 -3.14,5.74 -7.9,10.05z") }
    val Add by lazy { icon("add", "M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z") }
    val Close by lazy { icon("close", "M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z") }
    val Down by lazy { icon("down", "M7.41,8.59L12,13.17l4.59,-4.58L18,10l-6,6 -6,-6 1.41,-1.41z") }
    val Moon by lazy { icon("moon", "M12,3c-4.97,0 -9,4.03 -9,9s4.03,9 9,9 9,-4.03 9,-9c0,-0.46 -0.04,-0.92 -0.1,-1.36 -0.98,1.37 -2.58,2.26 -4.4,2.26 -2.98,0 -5.4,-2.42 -5.4,-5.4 0,-1.81 0.89,-3.42 2.26,-4.4 -0.44,-0.06 -0.9,-0.1 -1.36,-0.1z") }
    val Image by lazy { icon("image", "M21,19V5c0,-1.1 -0.9,-2 -2,-2H5c-1.1,0 -2,0.9 -2,2v14c0,1.1 0.9,2 2,2h14c1.1,0 2,-0.9 2,-2zM8.5,13.5l2.5,3.01L14.5,12l4.5,6H5l3.5,-4.5z") }
    val Download by lazy { icon("download", "M19,9h-4V3H9v6H5l7,7 7,-7zM5,18v2h14v-2H5z") }
    val More by lazy { icon("more", "M12,8c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2zM12,10c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2zM12,16c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z") }
    val Delete by lazy { icon("delete", "M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z") }
    val Edit by lazy { icon("edit", "M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25zM20.71,7.04c0.39,-0.39 0.39,-1.02 0,-1.41l-2.34,-2.34c-0.39,-0.39 -1.02,-0.39 -1.41,0l-1.83,1.83 3.75,3.75 1.83,-1.83z") }
    val Queue by lazy { icon("queue", "M15,6H3v2h12V6zM15,10H3v2h12v-2zM3,16h8v-2H3v2zM17,6v8.18c-0.31,-0.11 -0.65,-0.18 -1,-0.18 -1.66,0 -3,1.34 -3,3s1.34,3 3,3 3,-1.34 3,-3V8h3V6h-5z") }
    val Share by lazy { icon("share", "M18,16.08c-0.76,0 -1.44,0.3 -1.96,0.77L8.91,12.7c0.05,-0.23 0.09,-0.46 0.09,-0.7s-0.04,-0.47 -0.09,-0.7l7.05,-4.11c0.54,0.5 1.25,0.81 2.04,0.81 1.66,0 3,-1.34 3,-3s-1.34,-3 -3,-3 -3,1.34 -3,3c0,0.24 0.04,0.47 0.09,0.7L8.04,9.81C7.5,9.31 6.79,9 6,9c-1.66,0 -3,1.34 -3,3s1.34,3 3,3c0.79,0 1.5,-0.31 2.04,-0.81l7.12,4.16c-0.05,0.21 -0.08,0.43 -0.08,0.65 0,1.61 1.31,2.92 2.92,2.92 1.61,0 2.92,-1.31 2.92,-2.92s-1.31,-2.92 -2.92,-2.92z") }
    val Lyrics by lazy { icon("lyrics", "M4,5h16v2H4zM4,9h16v2H4zM4,13h16v2H4zM4,17h10v2H4z") }
    val Bolt by lazy { icon("bolt", "M7,2v11h3v9l7,-12h-4l4,-8z") }
    val Smile by lazy { icon("smile", "M12,2a10,10 0 0,0 0,20a10,10 0 0,0 0,-20zM12,4a8,8 0 0,1 0,16a8,8 0 0,1 0,-16zM9,8.6a1.4,1.4 0 0,0 0,2.8a1.4,1.4 0 0,0 0,-2.8zM15,8.6a1.4,1.4 0 0,0 0,2.8a1.4,1.4 0 0,0 0,-2.8zM7.5,13.5c0.8,2.2 2.4,3.5 4.5,3.5s3.7,-1.3 4.5,-3.5h-1.8c-0.7,1.2 -1.6,1.8 -2.7,1.8s-2,-0.6 -2.7,-1.8z") }
    val Dumbbell by lazy { icon("dumbbell", "M2,10h2v4H2zM4,8h3v8H4zM7,11h10v2H7zM17,8h3v8h-3zM20,10h2v4h-2z") }
    val Drop by lazy { icon("drop", "M12,2C12,2 5,10 5,15a7,7 0 0,0 14,0C19,10 12,2 12,2z") }
    val Target by lazy { icon("target", "M12,2a10,10 0 0,0 0,20a10,10 0 0,0 0,-20zM12,4a8,8 0 0,1 0,16a8,8 0 0,1 0,-16zM12,8a4,4 0 0,0 0,8a4,4 0 0,0 0,-8z") }
    val Star by lazy { icon("star", "M12,17.27L18.18,21l-1.64,-7.03L22,9.24l-7.19,-0.61L12,2 9.19,8.63 2,9.24l5.46,4.73L5.82,21z") }
    val Sparkle by lazy { icon("sparkle", "M12,2l2.4,7.6L22,12l-7.6,2.4L12,22l-2.4,-7.6L2,12l7.6,-2.4z") }
    val Flame by lazy { icon("flame", "M12,2C12.5,5 16.5,7 16.5,12.2A4.5,4.5 0 0,1 7.5,12.2C7.5,10.4 8.3,9.2 9.3,8.2C9.5,9.7 10.2,10.5 11,10.7C11.4,7.6 10.4,5 12,2z") }
    val Speed by lazy { icon("speed", "M20.38,8.57l-1.23,1.85a8,8 0,0 1,-0.22 7.58H5.07A8,8 0,0 1,15.58 6.85l1.85,-1.23A10,10 0,0 0,3.35 19a2,2 0,0 0,1.72 1h13.85a2,2 0,0 0,1.74 -1,10 10,0 0,0 -0.27,-10.44zM10.59,15.41a2,2 0,0 0,2.83 0l5.66,-8.49 -8.49,5.66a2,2 0,0 0,0 2.83z") }
}

/** Ikon suasana hati (kunci dari MOODS), semuanya vektor SVG, tanpa emoji. */
fun moodIcon(key: String): ImageVector = when (key) {
    "bolt" -> Ic.Bolt
    "smile" -> Ic.Smile
    "dumbbell" -> Ic.Dumbbell
    "drop" -> Ic.Drop
    "moon" -> Ic.Moon
    "target" -> Ic.Target
    "star" -> Ic.Star
    else -> Ic.Heart
}
