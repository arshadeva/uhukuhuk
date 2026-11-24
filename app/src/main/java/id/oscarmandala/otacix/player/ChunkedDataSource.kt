package id.oscarmandala.otacix.player

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener

/**
 * Teknik dari pemutar Metrolist: audio YouTube tidak diminta sekali jalan ("bytes=0-"),
 * tapi per potongan kecil (maks 1 MiB, "bytes=a-b"). Permintaan terbuka sering ditolak (403),
 * dibatasi kecepatannya, atau terputus di server googlevideo; permintaan terbatas tidak.
 * Kode ini ditulis sendiri berdasarkan ide itu, bukan salinan kode Metrolist.
 */
@UnstableApi
class ChunkedDataSource(
    private val factory: DataSource.Factory,
    private val chunk: Long,
) : DataSource {

    class Factory(
        private val upstream: DataSource.Factory,
        private val chunk: Long = 1_048_576L,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource = ChunkedDataSource(upstream, chunk)
    }

    private val listeners = ArrayList<TransferListener>()
    private var cur: DataSource? = null
    private var spec: DataSpec? = null
    private var pos = 0L        // posisi absolut berikutnya yang akan dibaca
    private var end = -1L       // batas akhir (eksklusif) yang diminta pemanggil, -1 = tidak ada
    private var chunkEnd = 0L   // batas akhir potongan yang sedang dibuka
    private var total = -1L     // ukuran total file bila diketahui dari Content-Range

    override fun addTransferListener(transferListener: TransferListener) {
        listeners.add(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        spec = dataSpec
        pos = dataSpec.position
        end = if (dataSpec.length == C.LENGTH_UNSET.toLong()) -1L else dataSpec.position + dataSpec.length
        total = -1L
        openChunk()
        return when {
            end >= 0 -> dataSpec.length
            total >= 0 -> total - pos
            else -> C.LENGTH_UNSET.toLong()
        }
    }

    private fun openChunk(): Boolean {
        closeCur()
        var len = chunk
        if (end >= 0) len = minOf(len, end - pos)
        if (total >= 0) len = minOf(len, total - pos)
        if (len <= 0) return false
        val ds = factory.createDataSource()
        listeners.forEach { ds.addTransferListener(it) }
        val s = spec!!.buildUpon().setPosition(pos).setLength(len).build()
        try {
            ds.open(s)
        } catch (e: HttpDataSource.InvalidResponseCodeException) {
            try { ds.close() } catch (_: Exception) { }
            if (e.responseCode == 416 && pos > 0) return false   // sudah lewat akhir file
            throw e
        }
        if (total < 0) {
            val cr = ds.responseHeaders.entries
                .firstOrNull { it.key.equals("Content-Range", ignoreCase = true) }?.value?.firstOrNull()
            total = cr?.substringAfter('/', "")?.toLongOrNull() ?: -1L
        }
        cur = ds
        chunkEnd = pos + len
        return true
    }

    private fun limitEnd(): Long = when {
        end >= 0 && total >= 0 -> minOf(end, total)
        end >= 0 -> end
        else -> total
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        while (true) {
            val limit = limitEnd()
            if (limit in 0..pos) return C.RESULT_END_OF_INPUT
            val c = cur ?: return C.RESULT_END_OF_INPUT
            val want = if (limit >= 0) minOf(length.toLong(), limit - pos).toInt() else length
            val n = c.read(buffer, offset, want)
            if (n == C.RESULT_END_OF_INPUT) {
                val full = pos >= chunkEnd
                closeCur()
                if (!full) return C.RESULT_END_OF_INPUT      // server berhenti lebih awal = akhir file
                if (!openChunk()) return C.RESULT_END_OF_INPUT
                continue
            }
            pos += n
            return n
        }
    }

    override fun getUri(): Uri? = cur?.uri ?: spec?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = cur?.responseHeaders ?: emptyMap()

    private fun closeCur() {
        try { cur?.close() } catch (_: Exception) { }
        cur = null
    }

    override fun close() {
        closeCur()
        spec = null
    }
}
