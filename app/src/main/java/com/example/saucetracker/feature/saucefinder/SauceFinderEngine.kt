package com.example.saucetracker.feature.saucefinder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.saucetracker.EntryDetail
import com.example.saucetracker.core.media.DuplicateLocalHashIndex
import com.example.saucetracker.core.media.computeDHash64
import com.example.saucetracker.core.media.fetchThumbnailBitmapRawOnce
import com.example.saucetracker.data.downloads.listDownloadedGalleryCodes
import com.example.saucetracker.data.downloads.loadDownloadedGalleryBundle
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

internal class SauceFinderEngine(
    private val context: Context,
    private val store: SauceFinderIndexStore = SauceFinderIndexStore(context)
) {
    fun stats(): SauceFinderIndexStats = store.stats()

    fun indexAvailableLocalImages(details: List<EntryDetail>, onProgress: (Int, Int) -> Unit) {
        details.forEach { detail ->
            if (!store.contains(detail.code, 0)) {
                DuplicateLocalHashIndex.get(detail.code)?.let { hash ->
                    store.put(
                        SauceFinderIndexRecord(
                            entryCode = detail.code,
                            pageNumber = 0,
                            source = "cover-cache",
                            fingerprint = SauceImageFingerprint(LongArray(5) { hash })
                        )
                    )
                }
            }
        }

        val detailsByCode = details.associateBy(EntryDetail::code)
        val downloadedCodes = listDownloadedGalleryCodes(context).filter { it in detailsByCode }
        downloadedCodes.forEachIndexed { codeIndex, code ->
            val bundle = loadDownloadedGalleryBundle(context, code)
            bundle?.pageUriStrings.orEmpty().forEachIndexed { pageIndex, uriString ->
                val pageNumber = pageIndex + 1
                if (!store.contains(code, pageNumber)) {
                    decodeBitmap(Uri.parse(uriString))?.useBitmap { bitmap ->
                        store.put(
                            SauceFinderIndexRecord(
                                entryCode = code,
                                pageNumber = pageNumber,
                                source = "downloaded",
                                fingerprint = fingerprint(bitmap)
                            )
                        )
                    }
                }
            }
            onProgress(codeIndex + 1, downloadedCodes.size)
        }
    }

    suspend fun buildFullIndex(details: List<EntryDetail>, onProgress: (Int, Int) -> Unit) = coroutineScope {
        val total = details.sumOf { it.numPages.coerceAtLeast(0) + 1 }.coerceAtLeast(1)
        val completed = AtomicInteger(0)
        val progressLock = Any()
        fun markCompleted() {
            val done = completed.incrementAndGet()
            synchronized(progressLock) { onProgress(done, total) }
        }

        val queue = Channel<RemoteIndexTask>(capacity = REMOTE_WORKER_COUNT * 3)
        val workers = List(REMOTE_WORKER_COUNT) {
            launch(Dispatchers.IO) {
                for (task in queue) {
                    val candidates = if (task.pageNumber == 0) {
                        coverCandidates(task.detail)
                    } else {
                        pageThumbnailCandidates(task.detail, task.pageNumber)
                    }
                    fetchFirstBitmap(candidates)?.useBitmap { bitmap ->
                        store.put(
                            SauceFinderIndexRecord(
                                entryCode = task.detail.code,
                                pageNumber = task.pageNumber,
                                source = if (task.pageNumber == 0) "remote-cover" else "remote-thumbnail",
                                fingerprint = fingerprint(bitmap)
                            )
                        )
                    }
                    markCompleted()
                    delay(REMOTE_WORKER_DELAY_MS)
                }
            }
        }

        try {
            details.forEach { detail ->
                for (pageNumber in 0..detail.numPages.coerceAtLeast(0)) {
                    if (store.contains(detail.code, pageNumber)) {
                        markCompleted()
                    } else {
                        queue.send(RemoteIndexTask(detail, pageNumber))
                    }
                }
            }
        } finally {
            queue.close()
        }
        workers.joinAll()
    }

    fun match(uri: Uri, details: List<EntryDetail>): List<SauceFinderMatch> {
        val query = decodeBitmap(uri)?.useBitmap(::fingerprint) ?: return emptyList()
        val detailsByCode = details.associateBy(EntryDetail::code)
        return store.all()
            .asSequence()
            .mapNotNull { record ->
                val detail = detailsByCode[record.entryCode] ?: return@mapNotNull null
                val similarity = sauceFingerprintSimilarity(query, record.fingerprint)
                SauceFinderMatch(
                    entryCode = record.entryCode,
                    title = detail.title,
                    pageNumber = record.pageNumber,
                    similarity = similarity,
                    confidence = sauceConfidenceLabel(similarity),
                    thumbnailUrl = detail.thumbnailUrl
                )
            }
            .sortedByDescending(SauceFinderMatch::similarity)
            .distinctBy(SauceFinderMatch::entryCode)
            .take(4)
            .toList()
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
        return open(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }

    private fun open(uri: Uri): InputStream? = runCatching {
        context.contentResolver.openInputStream(uri)
    }.getOrNull()

    private fun fingerprint(bitmap: Bitmap): SauceImageFingerprint {
        val width = bitmap.width.coerceAtLeast(1)
        val height = bitmap.height.coerceAtLeast(1)
        val centerX = width / 10
        val centerY = height / 10
        val crops = listOf(
            intArrayOf(0, 0, width, height),
            intArrayOf(centerX, centerY, (width - centerX * 2).coerceAtLeast(1), (height - centerY * 2).coerceAtLeast(1)),
            intArrayOf(0, 0, width, (height * 2 / 3).coerceAtLeast(1)),
            intArrayOf(0, height / 3, width, (height - height / 3).coerceAtLeast(1)),
            intArrayOf(width / 6, 0, (width * 2 / 3).coerceAtLeast(1), height)
        )
        return SauceImageFingerprint(LongArray(crops.size) { index ->
            val crop = crops[index]
            if (crop[0] == 0 && crop[1] == 0 && crop[2] == width && crop[3] == height) {
                computeDHash64(bitmap)
            } else {
                Bitmap.createBitmap(bitmap, crop[0], crop[1], crop[2], crop[3]).useBitmap(::computeDHash64)
            }
        })
    }

    private fun coverCandidates(detail: EntryDetail): List<String> {
        if (detail.mediaId <= 0L) return emptyList()
        val preferred = detail.coverExt.trim().lowercase().ifBlank { "jpg" }
        return listOf(preferred, "jpg", "png", "webp").distinct()
            .map { "https://t.nhentai.net/galleries/${detail.mediaId}/cover.$it" }
    }

    private fun pageThumbnailCandidates(detail: EntryDetail, pageNumber: Int): List<String> {
        if (detail.mediaId <= 0L || pageNumber <= 0) return emptyList()
        val preferred = detail.coverExt.trim().lowercase().ifBlank { "jpg" }
        return listOf(preferred, "jpg", "png", "webp").distinct()
            .map { "https://t.nhentai.net/galleries/${detail.mediaId}/${pageNumber}t.$it" }
    }

    private fun fetchFirstBitmap(urls: List<String>): Bitmap? = urls.firstNotNullOfOrNull { url ->
        runCatching { fetchThumbnailBitmapRawOnce(url, lowRes = true) }.getOrNull()
    }

    private data class RemoteIndexTask(val detail: EntryDetail, val pageNumber: Int)

    private companion object {
        const val REMOTE_WORKER_COUNT = 4
        const val REMOTE_WORKER_DELAY_MS = 350L
    }
}

private inline fun <T> Bitmap.useBitmap(block: (Bitmap) -> T): T = try {
    block(this)
} finally {
    if (!isRecycled) recycle()
}
