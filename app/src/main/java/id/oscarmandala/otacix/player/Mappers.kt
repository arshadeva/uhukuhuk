package id.oscarmandala.otacix.player

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import id.oscarmandala.otacix.data.Track
import id.oscarmandala.otacix.data.isItunesId

fun Track.toMediaItem(): MediaItem {
    val extras = Bundle().apply {
        putInt("dur", dur)
        putString("cover", cover)
        putString("src", src)
    }
    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(src ?: if (isItunesId(id)) "otacix://yt/$id?q=" + Uri.encode("$artist $title") else "otacix://yt/$id") // otacix:// diterjemahkan ke URL audio asli oleh PlaybackService
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title).setArtist(artist)
                .setArtworkUri(Uri.parse(cover))
                .setExtras(extras)
                .build()
        ).build()
}

fun MediaItem.toTrack(): Track {
    val m = mediaMetadata
    val e = m.extras
    return Track(
        id = mediaId,
        title = m.title?.toString().orEmpty(),
        artist = m.artist?.toString().orEmpty(),
        dur = e?.getInt("dur") ?: 0,
        cover = e?.getString("cover") ?: m.artworkUri?.toString().orEmpty(),
        src = e?.getString("src"),
    )
}
