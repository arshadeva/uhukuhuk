package id.oscarmandala.otacix.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.oscarmandala.otacix.R
import id.oscarmandala.otacix.util.PosterRenderer

/** Saran sawer / dukung developer lewat QRIS. Muncul setelah splash, dan bisa dibuka lagi dari tab Kamu. */
@Composable
fun SupportDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    GlassDialog(onDismiss) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Ic.Heart, null, Modifier.size(26.dp), tint = Yellow)
                Txt("Dukung Developer", 20.sp, FontWeight.ExtraBold)
            }
            Txt(
                "Otacix gratis dan tanpa iklan. Kalau kamu suka, boleh sawer lewat QRIS di bawah untuk membantu Oscar Mandala terus mengembangkan aplikasi ini. Berapa pun sangat berarti.",
                13.5.sp, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.padding(top = 10.dp),
            )
            Image(
                painterResource(R.drawable.qris), "QRIS dukung developer Otacix",
                Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White),
                contentScale = ContentScale.FillWidth,
            )
            Txt(
                "Scan dari HP lain, atau simpan lalu unggah dari galeri lewat e-wallet / m-banking apa pun.",
                12.sp, color = Color.White.copy(alpha = 0.6f), align = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
            PillButton("Nanti saja", onClick = onDismiss)
            PillButton("Simpan QRIS", Ic.Download, primary = true) { saveQris(ctx) }
        }
    }
}

private fun saveQris(ctx: Context) {
    val bmp = BitmapFactory.decodeResource(ctx.resources, R.drawable.qris)
    when {
        bmp == null -> toast(ctx, "Gagal memuat QRIS")
        PosterRenderer.saveToGallery(ctx, bmp, "otacix-qris-oscar-mandala.png") ->
            toast(ctx, "QRIS tersimpan di Galeri › Pictures/Otacix")
        else -> PosterRenderer.share(ctx, bmp, "Bagikan QRIS")
    }
}
