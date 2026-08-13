package com.example.saucetracker.data.downloads

import com.example.saucetracker.*
import com.example.saucetracker.data.backup.resolveOrCreateBackupContainerUri
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.saucetracker.core.network.HttpClientFactory
import com.example.saucetracker.core.network.HttpClientProfile
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
data class ExperimentalGalleryPhoto(
    val id: String,
    val displayName: String,
    val fileName: String,
    val uriString: String,
    val addedAtMillis: Long,
    val pinnedAtMillis: Long = 0L
)

data class ExperimentalGalleryContents(
    val folderUri: Uri,
    val photos: List<ExperimentalGalleryPhoto>
)

data class ExperimentalGalleryImportConflict(
    val sourceUri: Uri,
    val displayName: String,
    val existingPhoto: ExperimentalGalleryPhoto
)

data class ExperimentalGalleryImportResult(
    val importedCount: Int,
    val movedCount: Int,
    val deleteFailureCount: Int
)

fun experimentalGalleryFolderLabel(context: Context): String {
    val base = effectiveGalleryDownloadFolderLabel(context)
    return if (base == "Not set") {
        base
    } else {
        "$base / $GALLERY_DOWNLOADS_DIR_NAME / $EXPERIMENTAL_GALLERY_DIR_NAME"
    }
}

fun loadExperimentalGalleryContents(
    context: Context
): ExperimentalGalleryContents? {
    val treeUriString = resolveEffectiveGalleryDownloadTreeUri(context)
    if (treeUriString.isBlank()) return null
    return runCatching {
        val treeUri = Uri.parse(treeUriString)
        val downloadsDir = resolveExistingGalleryDownloadsDirectory(context, treeUri) ?: return@runCatching null
        val galleryDir = findChildDirectoryRef(context, downloadsDir, EXPERIMENTAL_GALLERY_DIR_NAME) ?: return@runCatching null
        val manifestEntries = readExperimentalGalleryManifestEntries(context, galleryDir)
        val childFiles = listChildDocuments(context, galleryDir).associateBy { it.displayName }
        val orderedPhotos = manifestEntries.mapNotNull { entry ->
            val doc = childFiles[entry.fileName] ?: return@mapNotNull null
            ExperimentalGalleryPhoto(
                id = entry.id,
                displayName = entry.displayName.ifBlank { entry.fileName },
                fileName = entry.fileName,
                uriString = doc.documentUri.toString(),
                addedAtMillis = entry.addedAtMillis,
                pinnedAtMillis = entry.pinnedAtMillis
            )
        }.sortedWith(
            compareByDescending<ExperimentalGalleryPhoto> { it.pinnedAtMillis > 0L }
                .thenByDescending { it.pinnedAtMillis }
                .thenByDescending { it.addedAtMillis }
        )
        ExperimentalGalleryContents(
            folderUri = galleryDir.documentUri,
            photos = orderedPhotos
        )
    }.getOrNull()
}

fun openExperimentalGalleryFolder(context: Context): Boolean {
    val treeUriString = resolveEffectiveGalleryDownloadTreeUri(context)
    if (treeUriString.isBlank()) return false
    return runCatching {
        val treeUri = Uri.parse(treeUriString)
        val downloadsDir = resolveOrCreateGalleryDownloadsDirectory(context, treeUri)
        val galleryDir = resolveOrCreateChildDirectory(context, downloadsDir, EXPERIMENTAL_GALLERY_DIR_NAME)
        ensureNoMediaMarker(context, downloadsDir)
        ensureNoMediaMarker(context, galleryDir)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(galleryDir.documentUri, DocumentsContract.Document.MIME_TYPE_DIR)
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, galleryDir.documentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
            addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}

suspend fun findExperimentalGalleryImportConflicts(
    context: Context,
    sourceUris: List<Uri>
): List<ExperimentalGalleryImportConflict> {
    val cleanUris = sourceUris.distinct().filter { it.toString().isNotBlank() }
    if (cleanUris.isEmpty()) return emptyList()
    val existingPhotos = loadExperimentalGalleryContents(context)?.photos.orEmpty()
    if (existingPhotos.isEmpty()) return emptyList()
    val existingByName = existingPhotos.associateBy { normalizeExperimentalGalleryDisplayName(it.displayName) }
    return cleanUris.mapNotNull { uri ->
        val displayName = queryOpenableDisplayName(context, uri).ifBlank { "Selected image" }
        val existing = existingByName[normalizeExperimentalGalleryDisplayName(displayName)] ?: return@mapNotNull null
        ExperimentalGalleryImportConflict(
            sourceUri = uri,
            displayName = displayName,
            existingPhoto = existing
        )
    }
}

suspend fun importExperimentalGalleryPhotos(
    context: Context,
    sourceUris: List<Uri>,
    replaceSourceUriStrings: Set<String> = emptySet(),
    skipSourceUriStrings: Set<String> = emptySet(),
    onProgress: (label: String, fraction: Float?) -> Unit
): ExperimentalGalleryImportResult {
    val cleanUris = sourceUris.distinct().filter { it.toString().isNotBlank() }
    if (cleanUris.isEmpty()) return ExperimentalGalleryImportResult(0, 0, 0)
    val treeUriString = resolveEffectiveGalleryDownloadTreeUri(context)
    if (treeUriString.isBlank()) {
        throw IOException("Set a procedural backup folder or downloads folder first.")
    }
    val treeUri = Uri.parse(treeUriString)
    onProgress("Preparing experimental gallery...", 0.05f)
    val downloadsDir = resolveOrCreateGalleryDownloadsDirectory(context, treeUri)
    val galleryDir = resolveOrCreateChildDirectory(context, downloadsDir, EXPERIMENTAL_GALLERY_DIR_NAME)
    ensureNoMediaMarker(context, downloadsDir)
    ensureNoMediaMarker(context, galleryDir)
    val manifestEntries = readExperimentalGalleryManifestEntries(context, galleryDir).toMutableList()
    val existingNames = manifestEntries.map { it.fileName }.toMutableSet()
    val existingByDisplayName = manifestEntries.associateBy {
        normalizeExperimentalGalleryDisplayName(it.displayName)
    }.toMutableMap()
    var imported = 0
    var moved = 0
    var deleteFailures = 0
    cleanUris.forEachIndexed { index, uri ->
        val uriKey = uri.toString()
        if (uriKey in skipSourceUriStrings) return@forEachIndexed
        val progressBase = 0.10f + (index.toFloat() / cleanUris.size.toFloat().coerceAtLeast(1f)) * 0.78f
        val displayName = queryOpenableDisplayName(context, uri).ifBlank { "Photo ${index + 1}" }
        val normalizedDisplayName = normalizeExperimentalGalleryDisplayName(displayName)
        if (uriKey in replaceSourceUriStrings) {
            existingByDisplayName.remove(normalizedDisplayName)?.let { existing ->
                findChildFileRef(context, galleryDir, existing.fileName)?.let { doc ->
                    runCatching {
                        DocumentsContract.deleteDocument(context.contentResolver, doc.documentUri)
                    }
                }
                manifestEntries.removeAll { it.id == existing.id || it.fileName == existing.fileName }
                existingNames.remove(existing.fileName)
            }
        } else if (existingByDisplayName.containsKey(normalizedDisplayName)) {
            return@forEachIndexed
        }
        onProgress("Importing ${index + 1} / ${cleanUris.size}...", progressBase.coerceIn(0f, 0.92f))
        val extension = resolveExperimentalGalleryExtension(context, uri, displayName)
        val fileName = generateExperimentalGalleryFileName(displayName, extension, existingNames, index)
        val mimeType = imageMimeTypeForExtension(extension)
        val destinationUri = resolveOrCreateChildFile(
            context = context,
            parent = galleryDir,
            displayName = fileName,
            mimeType = mimeType
        )
        copyUriToUri(context, uri, destinationUri)
        manifestEntries.removeAll { it.fileName == fileName || it.id == fileName.substringBeforeLast('.') }
        manifestEntries += ExperimentalGalleryManifestEntry(
            id = fileName.substringBeforeLast('.'),
            displayName = displayName,
            fileName = fileName,
            addedAtMillis = System.currentTimeMillis(),
            pinnedAtMillis = 0L
        )
        existingNames += fileName
        existingByDisplayName[normalizedDisplayName] = manifestEntries.last()
        imported += 1
        if (deleteSourceDocument(context, uri)) {
            moved += 1
        } else {
            deleteFailures += 1
        }
    }
    onProgress("Saving experimental gallery manifest...", 0.97f)
    writeExperimentalGalleryManifest(context, galleryDir, manifestEntries)
    onProgress("Finalizing experimental gallery...", 1f)
    return ExperimentalGalleryImportResult(imported, moved, deleteFailures)
}

fun removeExperimentalGalleryPhoto(
    context: Context,
    photoId: String
): Boolean {
    if (photoId.isBlank()) return false
    val treeUriString = resolveEffectiveGalleryDownloadTreeUri(context)
    if (treeUriString.isBlank()) return false
    return runCatching {
        val treeUri = Uri.parse(treeUriString)
        val downloadsDir = resolveExistingGalleryDownloadsDirectory(context, treeUri) ?: return@runCatching false
        val galleryDir = findChildDirectoryRef(context, downloadsDir, EXPERIMENTAL_GALLERY_DIR_NAME) ?: return@runCatching false
        val manifestEntries = readExperimentalGalleryManifestEntries(context, galleryDir).toMutableList()
        val target = manifestEntries.firstOrNull { it.id == photoId } ?: return@runCatching false
        findChildFileRef(context, galleryDir, target.fileName)?.let { doc ->
            DocumentsContract.deleteDocument(context.contentResolver, doc.documentUri)
        }
        manifestEntries.removeAll { it.id == photoId }
        writeExperimentalGalleryManifest(context, galleryDir, manifestEntries)
        true
    }.getOrDefault(false)
}

fun removeExperimentalGalleryPhotos(
    context: Context,
    photoIds: Set<String>
): Int {
    if (photoIds.isEmpty()) return 0
    val treeUriString = resolveEffectiveGalleryDownloadTreeUri(context)
    if (treeUriString.isBlank()) return 0
    return runCatching {
        val treeUri = Uri.parse(treeUriString)
        val downloadsDir = resolveExistingGalleryDownloadsDirectory(context, treeUri) ?: return@runCatching 0
        val galleryDir = findChildDirectoryRef(context, downloadsDir, EXPERIMENTAL_GALLERY_DIR_NAME) ?: return@runCatching 0
        val manifestEntries = readExperimentalGalleryManifestEntries(context, galleryDir).toMutableList()
        val targets = manifestEntries.filter { it.id in photoIds }
        targets.forEach { target ->
            findChildFileRef(context, galleryDir, target.fileName)?.let { doc ->
                runCatching {
                    DocumentsContract.deleteDocument(context.contentResolver, doc.documentUri)
                }
            }
        }
        manifestEntries.removeAll { it.id in photoIds }
        writeExperimentalGalleryManifest(context, galleryDir, manifestEntries)
        targets.size
    }.getOrDefault(0)
}

fun setExperimentalGalleryPhotoPinned(
    context: Context,
    photoId: String,
    pinned: Boolean
): Boolean {
    if (photoId.isBlank()) return false
    val treeUriString = resolveEffectiveGalleryDownloadTreeUri(context)
    if (treeUriString.isBlank()) return false
    return runCatching {
        val treeUri = Uri.parse(treeUriString)
        val downloadsDir = resolveExistingGalleryDownloadsDirectory(context, treeUri) ?: return@runCatching false
        val galleryDir = findChildDirectoryRef(context, downloadsDir, EXPERIMENTAL_GALLERY_DIR_NAME) ?: return@runCatching false
        val manifestEntries = readExperimentalGalleryManifestEntries(context, galleryDir).toMutableList()
        val index = manifestEntries.indexOfFirst { it.id == photoId }
        if (index < 0) return@runCatching false
        manifestEntries[index] = manifestEntries[index].copy(
            pinnedAtMillis = if (pinned) System.currentTimeMillis() else 0L
        )
        writeExperimentalGalleryManifest(context, galleryDir, manifestEntries)
        true
    }.getOrDefault(false)
}

private data class ExperimentalGalleryManifestEntry(
    val id: String,
    val displayName: String,
    val fileName: String,
    val addedAtMillis: Long,
    val pinnedAtMillis: Long
)

private fun readExperimentalGalleryManifestEntries(
    context: Context,
    galleryDir: GalleryDownloadDocumentRef
): List<ExperimentalGalleryManifestEntry> {
    val manifestRef = findChildFileRef(context, galleryDir, EXPERIMENTAL_GALLERY_MANIFEST_NAME)
    val fromManifest = manifestRef?.let { ref ->
        runCatching {
            val payload = JSONObject(readTextFromUri(context, ref.documentUri))
            val rows = payload.optJSONArray("photos") ?: JSONArray()
            buildList {
                for (index in 0 until rows.length()) {
                    val obj = rows.optJSONObject(index) ?: continue
                    val fileName = obj.optString("file_name", "").trim()
                    if (fileName.isBlank()) continue
                    add(
                        ExperimentalGalleryManifestEntry(
                            id = obj.optString("id", fileName.substringBeforeLast('.')).trim().ifBlank {
                                fileName.substringBeforeLast('.')
                            },
                            displayName = obj.optString("display_name", "").trim().ifBlank { fileName },
                            fileName = fileName,
                            addedAtMillis = obj.optLong("added_at_ms", 0L).coerceAtLeast(0L),
                            pinnedAtMillis = obj.optLong("pinned_at_ms", 0L).coerceAtLeast(0L)
                        )
                    )
                }
            }
        }.getOrNull()
    }.orEmpty()
    if (fromManifest.isNotEmpty()) return fromManifest
    return listChildDocuments(context, galleryDir)
        .asSequence()
        .filterNot {
            it.displayName == EXPERIMENTAL_GALLERY_MANIFEST_NAME ||
                it.displayName == GALLERY_DOWNLOAD_NOMEDIA_NAME
        }
        .map { child ->
            ExperimentalGalleryManifestEntry(
                id = child.displayName.substringBeforeLast('.'),
                displayName = child.displayName,
                fileName = child.displayName,
                addedAtMillis = 0L,
                pinnedAtMillis = 0L
            )
        }
        .toList()
}

private fun writeExperimentalGalleryManifest(
    context: Context,
    galleryDir: GalleryDownloadDocumentRef,
    entries: List<ExperimentalGalleryManifestEntry>
) {
    val manifest = JSONObject()
        .put("type", EXPERIMENTAL_GALLERY_TYPE)
        .put("updated_at_ms", System.currentTimeMillis())
        .put(
            "photos",
            JSONArray().apply {
                entries.sortedWith(
                    compareByDescending<ExperimentalGalleryManifestEntry> { it.pinnedAtMillis > 0L }
                        .thenByDescending { it.pinnedAtMillis }
                        .thenByDescending { it.addedAtMillis }
                ).forEach { entry ->
                    put(
                        JSONObject()
                            .put("id", entry.id)
                            .put("display_name", entry.displayName)
                            .put("file_name", entry.fileName)
                            .put("added_at_ms", entry.addedAtMillis)
                            .put("pinned_at_ms", entry.pinnedAtMillis)
                    )
                }
            }
        )
    val manifestUri = resolveOrCreateChildFile(
        context = context,
        parent = galleryDir,
        displayName = EXPERIMENTAL_GALLERY_MANIFEST_NAME,
        mimeType = GALLERY_DOWNLOAD_JSON_MIME
    )
    writeTextToUri(context, manifestUri, manifest.toString(2))
}

private fun queryOpenableDisplayName(
    context: Context,
    uri: Uri
): String {
    return runCatching {
        context.applicationContext.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                cursor.getString(index)?.trim().orEmpty()
            } else {
                ""
            }
        }.orEmpty()
    }.getOrDefault("")
}

private fun resolveExperimentalGalleryExtension(
    context: Context,
    uri: Uri,
    displayName: String
): String {
    val fromName = displayName.substringAfterLast('.', "").trim().lowercase(Locale.US)
    val normalizedName = normalizeDownloadImageExtension(fromName)
    if (normalizedName.isNotBlank()) return normalizedName
    val mimeType = context.applicationContext.contentResolver.getType(uri).orEmpty()
    val fromMime = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType).orEmpty()
    return normalizeDownloadImageExtension(fromMime).ifBlank { "jpg" }
}

private fun generateExperimentalGalleryFileName(
    displayName: String,
    extension: String,
    existingNames: Set<String>,
    index: Int
): String {
    val safeBase = displayName
        .substringBeforeLast('.')
        .replace(Regex("[^A-Za-z0-9._-]+"), "_")
        .trim('_')
        .ifBlank { "photo" }
        .take(48)
    val ext = normalizeDownloadImageExtension(extension).ifBlank { "jpg" }
    var attempt = 0
    while (true) {
        val stamp = System.currentTimeMillis()
        val suffix = if (attempt == 0) {
            String.format(Locale.US, "%03d", index + 1)
        } else {
            String.format(Locale.US, "%03d", attempt)
        }
        val candidate = "${safeBase}_${stamp}_$suffix.$ext"
        if (candidate !in existingNames) return candidate
        attempt += 1
    }
}

private fun normalizeExperimentalGalleryDisplayName(displayName: String): String {
    return displayName.trim().lowercase(Locale.US)
}

private fun deleteSourceDocument(
    context: Context,
    sourceUri: Uri
): Boolean {
    val resolver = context.applicationContext.contentResolver
    val direct = runCatching {
        resolver.delete(sourceUri, null, null) > 0
    }.getOrDefault(false)
    if (direct) return true
    return runCatching {
        DocumentsContract.deleteDocument(resolver, sourceUri)
    }.getOrDefault(false)
}

private fun copyUriToUri(
    context: Context,
    sourceUri: Uri,
    destinationUri: Uri
) {
    val resolver = context.applicationContext.contentResolver
    val input = resolver.openInputStream(sourceUri) ?: throw IOException("Could not read selected image.")
    val output = resolver.openOutputStream(destinationUri, "w")
        ?: resolver.openOutputStream(destinationUri, "rwt")
        ?: throw IOException("Could not write imported image.")
    input.use { source ->
        output.use { destination ->
            source.copyTo(destination)
            destination.flush()
        }
    }
}
