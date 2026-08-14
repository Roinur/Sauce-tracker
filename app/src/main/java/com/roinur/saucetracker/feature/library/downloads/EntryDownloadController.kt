package com.roinur.saucetracker.feature.library.downloads

import android.content.Context
import android.os.Handler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.roinur.saucetracker.EntryDetail
import com.roinur.saucetracker.EntryDownloadBatchMode
import com.roinur.saucetracker.EntryDownloadBatchProgressState
import com.roinur.saucetracker.EntryDownloadProgressState
import com.roinur.saucetracker.data.downloads.downloadGalleryToLocal
import com.roinur.saucetracker.data.downloads.effectiveGalleryDownloadFolderLabel
import com.roinur.saucetracker.data.downloads.isGalleryDownloaded
import com.roinur.saucetracker.data.downloads.listDownloadedGalleryCodes
import com.roinur.saucetracker.data.downloads.openDownloadedGalleryFolder
import com.roinur.saucetracker.data.downloads.removeDownloadedGallery
import com.roinur.saucetracker.data.repository.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class EntryDownloadController(
    private val appContext: Context,
    private val scope: CoroutineScope,
    private val libraryRepository: LibraryRepository,
    private val mainHandler: Handler,
    private val shouldReloadDownloadedEntries: () -> Boolean,
    private val reloadEntries: () -> Unit,
    private val onStatus: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onInfo: (String) -> Unit
) {
    var progressState by mutableStateOf<EntryDownloadProgressState?>(null)
        private set
    var batchMode by mutableStateOf<EntryDownloadBatchMode?>(null)
        private set
    var batchSelectedCodes by mutableStateOf<Set<Int>>(emptySet())
        private set
    var batchProgressState by mutableStateOf<EntryDownloadBatchProgressState?>(null)
        private set
    var downloadedGalleryNonce by mutableStateOf(0L)
        private set
    var downloadedCodes: Set<Int> = emptySet()
        private set

    fun refreshDownloadedCodes(invalidateUi: Boolean = false) {
        scope.launch(Dispatchers.IO) {
            downloadedCodes = listDownloadedGalleryCodes(appContext)
        }
        if (invalidateUi) {
            downloadedGalleryNonce += 1L
        }
    }

    fun folderLabel(): String = effectiveGalleryDownloadFolderLabel(appContext)

    fun isDownloaded(code: Int): Boolean = code > 0 && isGalleryDownloaded(appContext, code)

    fun download(detail: EntryDetail) {
        if (progressState != null) return
        progressState = EntryDownloadProgressState(
            code = detail.code,
            label = "Preparing download...",
            fraction = 0f
        )
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    downloadGalleryToLocal(appContext, detail) { label, fraction ->
                        mainHandler.post {
                            progressState = EntryDownloadProgressState(
                                code = detail.code,
                                label = label,
                                fraction = fraction
                            )
                        }
                    }
                }
            }
            progressState = null
            result.onSuccess {
                downloadedCodes = downloadedCodes + detail.code
                downloadedGalleryNonce += 1L
                reloadDownloadedEntriesIfNeeded()
                onStatus("Downloaded code ${detail.code} locally.")
            }.onFailure { exc ->
                onError("Could not download code ${detail.code}:\n${exc.message ?: "unknown error"}")
                onStatus("Could not download code ${detail.code}.")
            }
        }
    }

    fun startBatch(mode: EntryDownloadBatchMode, initialCode: Int?) {
        if (progressState != null || batchProgressState != null) return
        batchMode = mode
        batchSelectedCodes = initialCode?.let(::setOf) ?: emptySet()
        onStatus(
            when (mode) {
                EntryDownloadBatchMode.DOWNLOAD -> "Batch download selection enabled."
                EntryDownloadBatchMode.REDOWNLOAD -> "Batch re-download selection enabled."
            }
        )
    }

    fun cancelBatchSelection() {
        batchMode = null
        batchSelectedCodes = emptySet()
    }

    fun isBatchSelecting(): Boolean = batchMode != null

    fun isBatchSelected(code: Int): Boolean = code in batchSelectedCodes

    fun toggleBatchSelection(code: Int) {
        if (code <= 0 || batchMode == null) return
        batchSelectedCodes = batchSelectedCodes.toMutableSet().also { selected ->
            if (!selected.add(code)) selected.remove(code)
        }
    }

    fun runBatch() {
        val selectedMode = batchMode ?: return
        if (progressState != null || batchProgressState != null) return
        val codes = batchSelectedCodes.toList()
        if (codes.isEmpty()) {
            onInfo("No entries selected for batch download.")
            return
        }
        batchMode = null
        batchSelectedCodes = emptySet()
        scope.launch {
            val details = withContext(Dispatchers.IO) {
                libraryRepository.entryDetails(codes)
            }.associateBy { it.code }
            val total = codes.size
            var processed = 0
            var succeeded = 0
            batchProgressState = EntryDownloadBatchProgressState(
                mode = selectedMode,
                processed = 0,
                total = total,
                currentCode = null,
                label = "Preparing batch...",
                itemFraction = 0f
            )
            codes.forEach { code ->
                val detail = details[code]
                if (detail == null) {
                    processed++
                    batchProgressState = EntryDownloadBatchProgressState(
                        mode = selectedMode,
                        processed = processed,
                        total = total,
                        currentCode = code,
                        label = "Skipping code $code (details unavailable)...",
                        itemFraction = 1f
                    )
                    return@forEach
                }
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        if (selectedMode == EntryDownloadBatchMode.REDOWNLOAD) {
                            removeDownloadedGallery(appContext, detail.code)
                        }
                        downloadGalleryToLocal(appContext, detail) { label, fraction ->
                            mainHandler.post {
                                batchProgressState = EntryDownloadBatchProgressState(
                                    mode = selectedMode,
                                    processed = processed,
                                    total = total,
                                    currentCode = detail.code,
                                    label = label,
                                    itemFraction = fraction
                                )
                            }
                        }
                    }
                }
                processed++
                result.onSuccess {
                    succeeded++
                    downloadedCodes = downloadedCodes + detail.code
                }
                batchProgressState = EntryDownloadBatchProgressState(
                    mode = selectedMode,
                    processed = processed,
                    total = total,
                    currentCode = detail.code,
                    label = if (result.isSuccess) {
                        "${if (selectedMode == EntryDownloadBatchMode.REDOWNLOAD) "Re-downloaded" else "Downloaded"} code ${detail.code}"
                    } else {
                        "Failed code ${detail.code}"
                    },
                    itemFraction = 1f
                )
            }
            batchProgressState = null
            downloadedGalleryNonce += 1L
            reloadDownloadedEntriesIfNeeded()
            onStatus(
                when (selectedMode) {
                    EntryDownloadBatchMode.DOWNLOAD -> "Batch download finished: $succeeded / $total succeeded."
                    EntryDownloadBatchMode.REDOWNLOAD -> "Batch re-download finished: $succeeded / $total succeeded."
                }
            )
        }
    }

    fun redownload(detail: EntryDetail) {
        if (progressState != null) return
        progressState = EntryDownloadProgressState(
            code = detail.code,
            label = "Replacing local download...",
            fraction = 0f
        )
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    removeDownloadedGallery(appContext, detail.code)
                    downloadGalleryToLocal(appContext, detail) { label, fraction ->
                        mainHandler.post {
                            progressState = EntryDownloadProgressState(
                                code = detail.code,
                                label = label,
                                fraction = fraction
                            )
                        }
                    }
                }
            }
            progressState = null
            result.onSuccess {
                downloadedCodes = downloadedCodes + detail.code
                downloadedGalleryNonce += 1L
                reloadDownloadedEntriesIfNeeded()
                onStatus("Re-downloaded code ${detail.code} locally.")
            }.onFailure { exc ->
                onError("Could not re-download code ${detail.code}:\n${exc.message ?: "unknown error"}")
                onStatus("Could not re-download code ${detail.code}.")
            }
        }
    }

    fun openFolder(code: Int) {
        if (openDownloadedGalleryFolder(appContext, code)) {
            onStatus("Opened local download folder for code $code.")
        } else {
            onError("Could not open the local download folder for code $code.")
            onStatus("Could not open local download folder.")
        }
    }

    fun remove(code: Int) {
        if (code <= 0) return
        if (removeDownloadedGallery(appContext, code)) {
            downloadedCodes = downloadedCodes - code
            downloadedGalleryNonce += 1L
            reloadDownloadedEntriesIfNeeded()
            onStatus("Removed local download for code $code.")
        } else {
            onError("Could not remove the local download for code $code.")
            onStatus("Could not remove local download.")
        }
    }

    private fun reloadDownloadedEntriesIfNeeded() {
        if (shouldReloadDownloadedEntries()) reloadEntries()
    }
}
