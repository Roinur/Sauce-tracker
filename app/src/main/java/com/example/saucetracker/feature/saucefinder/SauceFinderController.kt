package com.example.saucetracker.feature.saucefinder

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.example.saucetracker.feature.dashboard.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class SauceFinderController(
    private val owner: DashboardViewModel,
    application: Application,
    private val loadDetails: () -> List<com.example.saucetracker.EntryDetail>
) {
    private val engine = SauceFinderEngine(application)
    private val _state = MutableStateFlow(SauceFinderUiState())
    val state: StateFlow<SauceFinderUiState> = _state.asStateFlow()
    private var indexJob: Job? = null

    fun refreshStats() {
        owner.viewModelScope.launch(Dispatchers.IO) {
            publishStats()
        }
    }

    fun prepareLocalIndex() {
        if (indexJob?.isActive == true) return
        indexJob = owner.viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(indexing = true, message = "Checking local images...")
            runCatching {
                val details = loadDetails()
                engine.indexAvailableLocalImages(details) { done, total ->
                    _state.value = _state.value.copy(
                        progress = if (total > 0) done.toFloat() / total else null,
                        message = if (total > 0) "Indexing downloaded galleries $done / $total" else "Local index ready."
                    )
                }
            }.onFailure { error ->
                _state.value = _state.value.copy(message = "Could not update local index: ${error.message ?: "Unknown error"}")
            }
            publishStats(indexing = false, progress = null)
        }
    }

    fun buildFullIndex() {
        if (indexJob?.isActive == true) return
        indexJob = owner.viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(
                indexing = true,
                progress = 0f,
                message = "Building the full library image index. You can leave this page."
            )
            runCatching {
                val details = loadDetails()
                engine.indexAvailableLocalImages(details) { _, _ -> }
                val activeContext = coroutineContext
                engine.buildFullIndex(details) { done, total ->
                    activeContext.ensureActive()
                    _state.value = _state.value.copy(
                        progress = done.toFloat() / total.coerceAtLeast(1),
                        message = "Full index $done / $total images"
                    )
                }
                _state.value = _state.value.copy(message = "Full library index is ready.")
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    message = if (error is CancellationException) {
                        "Index paused. Tap Resume index when you want to continue."
                    } else {
                        "Index paused: ${error.message ?: "Unknown error"}. Tap Resume index to continue."
                    }
                )
            }
            publishStats(indexing = false, progress = null)
        }
    }

    fun pauseFullIndex() {
        indexJob?.cancel()
        _state.value = _state.value.copy(
            indexing = false,
            progress = null,
            message = "Index paused. Progress is saved."
        )
    }

    fun find(uri: Uri) {
        owner.viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(matching = true, match = null, alternatives = emptyList(), message = "Searching local image index...")
            val matches = runCatching {
                val details = loadDetails()
                engine.indexAvailableLocalImages(details) { _, _ -> }
                engine.match(uri, details)
            }.getOrElse { error ->
                _state.value = _state.value.copy(
                    matching = false,
                    message = "Could not read that image: ${error.message ?: "Unknown error"}"
                )
                return@launch
            }
            val best = matches.firstOrNull()
            val message = when {
                best == null -> "The index is empty. Build the full index or download galleries first."
                best.similarity < 0.68f -> "No reliable match yet. A larger index or a less cropped image may help."
                else -> "${best.confidence} found in your library."
            }
            _state.value = _state.value.copy(
                matching = false,
                message = message,
                match = best?.takeIf { it.similarity >= 0.68f },
                alternatives = matches.drop(1).filter { it.similarity >= 0.68f }.take(2)
            )
            publishStats()
        }
    }

    fun requestOpen() {
        _state.value = _state.value.copy(openRequestNonce = _state.value.openRequestNonce + 1L)
    }

    private fun publishStats(indexing: Boolean = _state.value.indexing, progress: Float? = _state.value.progress) {
        val stats = engine.stats()
        _state.value = _state.value.copy(
            indexedImages = stats.images,
            indexedEntries = stats.entries,
            indexBytes = stats.bytes,
            indexing = indexing,
            progress = progress
        )
    }
}
