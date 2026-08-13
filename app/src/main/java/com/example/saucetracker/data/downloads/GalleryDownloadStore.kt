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

const val KEY_GALLERY_DOWNLOAD_TREE_URI = "gallery_download_tree_uri"
const val KEY_GALLERY_DOWNLOAD_SKIP_PROMPT = "gallery_download_skip_prompt"

internal const val GALLERY_DOWNLOAD_PREFS_NAME = "nhtagbook_prefs"
internal const val GALLERY_DOWNLOAD_BACKUP_TREE_URI_KEY = "auto_backup_tree_uri"
internal const val GALLERY_DOWNLOADS_DIR_NAME = "gallery_downloads"
internal const val GALLERY_DOWNLOAD_MANIFEST_NAME = "manifest.json"
internal const val GALLERY_DOWNLOAD_INFO_PAGE_NAME = "000-info.png"
internal const val GALLERY_DOWNLOAD_INFO_PAGE_MIME = "image/png"
internal const val GALLERY_DOWNLOAD_JSON_MIME = "application/json"
internal const val EXPERIMENTAL_GALLERY_DIR_NAME = "experimental_gallery"
internal const val EXPERIMENTAL_GALLERY_MANIFEST_NAME = "manifest.json"
internal const val EXPERIMENTAL_GALLERY_TYPE = "experimental_gallery"
internal const val GALLERY_DOWNLOAD_NOMEDIA_NAME = ".nomedia"
internal const val GALLERY_DOWNLOAD_NOMEDIA_MIME = "application/octet-stream"

data class DownloadedGalleryBundle(
    val code: Int,
    val title: String,
    val folderUri: Uri,
    val pageUriStrings: List<String>
)

data class DownloadedGalleryFile(
    val displayName: String,
    val uriString: String
)

data class DownloadedGalleryFolderContents(
    val code: Int,
    val title: String,
    val folderUri: Uri,
    val files: List<DownloadedGalleryFile>
)

internal data class GalleryDownloadDocumentRef(
    val treeUri: Uri,
    val documentId: String,
    val documentUri: Uri,
    val displayName: String
)

internal val localGalleryDownloadHttpClient: OkHttpClient by lazy {
    HttpClientFactory.create(HttpClientProfile.DOWNLOAD)
}

internal fun localGalleryDownloadPrefs(context: Context) =
    context.applicationContext.getSharedPreferences(GALLERY_DOWNLOAD_PREFS_NAME, Context.MODE_PRIVATE)

fun loadGalleryDownloadTreeUri(context: Context): String {
    return localGalleryDownloadPrefs(context).getString(KEY_GALLERY_DOWNLOAD_TREE_URI, "").orEmpty()
}

fun storeGalleryDownloadTreeUri(context: Context, uriString: String) {
    localGalleryDownloadPrefs(context).edit().putString(KEY_GALLERY_DOWNLOAD_TREE_URI, uriString).apply()
}

fun loadGalleryDownloadSkipPrompt(context: Context): Boolean {
    return localGalleryDownloadPrefs(context).getBoolean(KEY_GALLERY_DOWNLOAD_SKIP_PROMPT, false)
}

fun storeGalleryDownloadSkipPrompt(context: Context, value: Boolean) {
    localGalleryDownloadPrefs(context).edit().putBoolean(KEY_GALLERY_DOWNLOAD_SKIP_PROMPT, value).apply()
}

internal fun loadBackupTreeUriForDownloads(context: Context): String {
    return localGalleryDownloadPrefs(context).getString(GALLERY_DOWNLOAD_BACKUP_TREE_URI_KEY, "").orEmpty()
}

fun resolveEffectiveGalleryDownloadTreeUri(context: Context): String {
    val custom = loadGalleryDownloadTreeUri(context).trim()
    if (custom.isNotBlank()) return custom
    return loadBackupTreeUriForDownloads(context).trim()
}

fun effectiveGalleryDownloadFolderLabel(context: Context): String {
    val custom = loadGalleryDownloadTreeUri(context).trim()
    val effective = resolveEffectiveGalleryDownloadTreeUri(context)
    if (effective.isBlank()) return "Not set"
    return runCatching {
        val treeId = DocumentsContract.getTreeDocumentId(Uri.parse(effective))
        when {
            treeId.isBlank() -> "Selected folder"
            custom.isNotBlank() -> treeId
            else -> "$treeId / SauceTracker Backup"
        }
    }.getOrDefault("Selected folder")
}

fun listDownloadedGalleryCodes(context: Context): Set<Int> {
    val treeUriString = resolveEffectiveGalleryDownloadTreeUri(context)
    if (treeUriString.isBlank()) return emptySet()
    return runCatching {
        val treeUri = Uri.parse(treeUriString)
        val downloadsDir = resolveExistingGalleryDownloadsDirectory(context, treeUri) ?: return@runCatching emptySet()
        listChildDocuments(context, downloadsDir)
            .asSequence()
            .mapNotNull { child -> child.displayName.toIntOrNull() }
            .filter { code -> loadDownloadedGalleryBundle(context, code) != null }
            .toSet()
    }.getOrDefault(emptySet())
}

fun isGalleryDownloaded(context: Context, code: Int): Boolean {
    if (code <= 0) return false
    return loadDownloadedGalleryBundle(context, code) != null
}

fun removeDownloadedGallery(context: Context, code: Int): Boolean {
    if (code <= 0) return false
    val treeUriString = resolveEffectiveGalleryDownloadTreeUri(context)
    if (treeUriString.isBlank()) return false
    return runCatching {
        val treeUri = Uri.parse(treeUriString)
        val downloadsDir = resolveExistingGalleryDownloadsDirectory(context, treeUri) ?: return@runCatching false
        val entryDir = findChildDirectoryRef(context, downloadsDir, code.toString()) ?: return@runCatching false
        DocumentsContract.deleteDocument(context.contentResolver, entryDir.documentUri)
    }.getOrDefault(false)
}

fun openDownloadedGalleryFolder(context: Context, code: Int): Boolean {
    if (code <= 0) return false
    val treeUriString = resolveEffectiveGalleryDownloadTreeUri(context)
    if (treeUriString.isBlank()) return false
    return runCatching {
        val treeUri = Uri.parse(treeUriString)
        val downloadsDir = resolveExistingGalleryDownloadsDirectory(context, treeUri) ?: return@runCatching false
        val entryDir = findChildDirectoryRef(context, downloadsDir, code.toString()) ?: return@runCatching false
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(entryDir.documentUri, DocumentsContract.Document.MIME_TYPE_DIR)
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, entryDir.documentUri)
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

fun loadDownloadedGalleryFolderContents(
    context: Context,
    code: Int
): DownloadedGalleryFolderContents? {
    if (code <= 0) return null
    val treeUriString = resolveEffectiveGalleryDownloadTreeUri(context)
    if (treeUriString.isBlank()) return null
    return runCatching {
        val treeUri = Uri.parse(treeUriString)
        val downloadsDir = resolveExistingGalleryDownloadsDirectory(context, treeUri) ?: return@runCatching null
        val entryDir = findChildDirectoryRef(context, downloadsDir, code.toString()) ?: return@runCatching null
        val manifestRef = findChildFileRef(context, entryDir, GALLERY_DOWNLOAD_MANIFEST_NAME) ?: return@runCatching null
        val manifest = JSONObject(readTextFromUri(context, manifestRef.documentUri))
        val infoPageName = manifest.optString("info_page_file").trim().ifBlank { GALLERY_DOWNLOAD_INFO_PAGE_NAME }
        val pageFiles = manifest.optJSONArray("page_files")
            ?.let { array ->
                buildList {
                    for (index in 0 until array.length()) {
                        val name = array.optString(index).trim()
                        if (name.isNotBlank()) add(name)
                    }
                }
            }
            .orEmpty()
        val childFiles = listChildDocuments(context, entryDir).associateBy { it.displayName }
        val orderedNames = buildList {
            if (infoPageName.isNotBlank()) add(infoPageName)
            addAll(pageFiles)
        }.distinct()
        val files = orderedNames.mapNotNull { name ->
            childFiles[name]?.documentUri?.toString()?.let { uri ->
                DownloadedGalleryFile(displayName = name, uriString = uri)
            }
        }
        if (files.isEmpty()) return@runCatching null
        DownloadedGalleryFolderContents(
            code = code,
            title = manifest.optString("title").ifBlank { "Gallery $code" },
            folderUri = entryDir.documentUri,
            files = files
        )
    }.getOrNull()
}

fun loadDownloadedGalleryBundle(
    context: Context,
    code: Int
): DownloadedGalleryBundle? {
    if (code <= 0) return null
    val treeUriString = resolveEffectiveGalleryDownloadTreeUri(context)
    if (treeUriString.isBlank()) return null
    return runCatching {
        val treeUri = Uri.parse(treeUriString)
        val downloadsDir = resolveExistingGalleryDownloadsDirectory(context, treeUri) ?: return@runCatching null
        val entryDir = findChildDirectoryRef(context, downloadsDir, code.toString()) ?: return@runCatching null
        val manifestRef = findChildFileRef(context, entryDir, GALLERY_DOWNLOAD_MANIFEST_NAME) ?: return@runCatching null
        val manifest = JSONObject(readTextFromUri(context, manifestRef.documentUri))
        val pageFiles = manifest.optJSONArray("page_files")
            ?.let { array ->
                buildList {
                    for (index in 0 until array.length()) {
                        val name = array.optString(index).trim()
                        if (name.isNotBlank() && name != GALLERY_DOWNLOAD_INFO_PAGE_NAME) add(name)
                    }
                }
            }
            .orEmpty()
        if (pageFiles.isEmpty()) return@runCatching null
        val childFiles = listChildDocuments(context, entryDir).associateBy { it.displayName }
        val pageUris = pageFiles.mapNotNull { childFiles[it]?.documentUri?.toString() }
        if (pageUris.isEmpty()) return@runCatching null
        DownloadedGalleryBundle(
            code = code,
            title = manifest.optString("title").ifBlank { "Gallery $code" },
            folderUri = entryDir.documentUri,
            pageUriStrings = pageUris
        )
    }.getOrNull()
}

suspend fun downloadGalleryToLocal(
    context: Context,
    detail: EntryDetail,
    onProgress: (label: String, fraction: Float?) -> Unit
): DownloadedGalleryBundle {
    if (detail.code <= 0) throw IOException("Invalid gallery code.")
    if (detail.mediaId <= 0L || detail.numPages <= 0) {
        throw IOException("Gallery pages are unavailable for this entry.")
    }
    val treeUriString = resolveEffectiveGalleryDownloadTreeUri(context)
    if (treeUriString.isBlank()) {
        throw IOException("Set a procedural backup folder or downloads folder first.")
    }

    val treeUri = Uri.parse(treeUriString)
    onProgress("Preparing download folder...", 0.05f)
    val downloadsDir = resolveOrCreateGalleryDownloadsDirectory(context, treeUri)
    val entryDir = resolveOrCreateChildDirectory(context, downloadsDir, detail.code.toString())
    ensureNoMediaMarker(context, downloadsDir)

    onProgress("Downloading cover and first page...", 0.10f)
    val coverBitmap = fetchGalleryDownloadThumbnailBitmap(detail.thumbnailUrl)
    val artistName = detail.tagsByType["artist"].orEmpty().firstOrNull()
        ?: detail.tagsByType["group"].orEmpty().firstOrNull()
        ?: "-"
    val firstPageAsset = downloadGalleryPageAsset(
        mediaId = detail.mediaId,
        pageNumber = 1,
        preferredExt = detail.coverExt
    ) ?: throw IOException("Could not download page 1.")
    val firstPageBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(firstPageAsset.first, 0, firstPageAsset.first.size, firstPageBounds)
    val infoPageBitmap = buildGalleryInfoPageBitmap(
        title = detail.title.ifBlank { "Gallery ${detail.code}" },
        artistName = artistName,
        code = detail.code,
        thumbnail = coverBitmap,
        pageWidth = firstPageBounds.outWidth.coerceAtLeast(1080),
        pageHeight = firstPageBounds.outHeight.coerceAtLeast(1600)
    )
    val infoBytes = ByteArrayOutputStream().use { out ->
        if (!infoPageBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
            throw IOException("Could not render info page.")
        }
        out.toByteArray()
    }
    val infoUri = resolveOrCreateChildFile(
        context = context,
        parent = entryDir,
        displayName = GALLERY_DOWNLOAD_INFO_PAGE_NAME,
        mimeType = GALLERY_DOWNLOAD_INFO_PAGE_MIME
    )
    writeBytesToUri(context, infoUri, infoBytes)
    if (!infoPageBitmap.isRecycled) infoPageBitmap.recycle()
    coverBitmap?.takeIf { !it.isRecycled }?.recycle()

    val pageFiles = mutableListOf<String>()
    for (pageNumber in 1..detail.numPages) {
        val start = 0.20f
        val span = 0.74f
        val fraction = start + ((pageNumber - 1).toFloat() / detail.numPages.toFloat().coerceAtLeast(1f)) * span
        onProgress("Downloading page $pageNumber / ${detail.numPages}...", fraction.coerceIn(0f, 0.94f))
        val asset = if (pageNumber == 1) {
            firstPageAsset
        } else {
            downloadGalleryPageAsset(
                mediaId = detail.mediaId,
                pageNumber = pageNumber,
                preferredExt = detail.coverExt
            ) ?: throw IOException("Could not download page $pageNumber.")
        }
        val fileName = String.format(Locale.US, "%03d.%s", pageNumber, asset.second)
        val pageUri = resolveOrCreateChildFile(
            context = context,
            parent = entryDir,
            displayName = fileName,
            mimeType = imageMimeTypeForExtension(asset.second)
        )
        writeBytesToUri(context, pageUri, asset.first)
        pageFiles += fileName
    }

    onProgress("Saving download manifest...", 0.97f)
    val manifest = JSONObject()
        .put("code", detail.code)
        .put("title", detail.title)
        .put("artist_name", artistName)
        .put("info_page_file", GALLERY_DOWNLOAD_INFO_PAGE_NAME)
        .put("page_files", JSONArray(pageFiles))
        .put("num_pages", detail.numPages)
        .put("downloaded_at", System.currentTimeMillis())
    val manifestUri = resolveOrCreateChildFile(
        context = context,
        parent = entryDir,
        displayName = GALLERY_DOWNLOAD_MANIFEST_NAME,
        mimeType = GALLERY_DOWNLOAD_JSON_MIME
    )
    writeTextToUri(context, manifestUri, manifest.toString(2))

    onProgress("Finalizing download...", 1f)
    return DownloadedGalleryBundle(
        code = detail.code,
        title = detail.title.ifBlank { "Gallery ${detail.code}" },
        folderUri = entryDir.documentUri,
        pageUriStrings = pageFiles.mapNotNull { findChildFileRef(context, entryDir, it)?.documentUri?.toString() }
    )
}

internal fun ensureNoMediaMarker(
    context: Context,
    parent: GalleryDownloadDocumentRef
) {
    runCatching {
        val noMediaUri = resolveOrCreateChildFile(
            context = context,
            parent = parent,
            displayName = GALLERY_DOWNLOAD_NOMEDIA_NAME,
            mimeType = GALLERY_DOWNLOAD_NOMEDIA_MIME
        )
        writeBytesToUri(context, noMediaUri, ByteArray(0))
    }
}

fun fetchLocalGalleryPageBitmap(
    context: Context,
    uriString: String
): ImageBitmap? {
    if (uriString.isBlank()) return null
    return runCatching {
        val uri = Uri.parse(uriString)
        context.contentResolver.openInputStream(uri)?.use { input ->
            val bytes = input.readBytes()
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@use null
            bitmap.asImageBitmap()
        }
    }.getOrNull()
}

internal fun fetchGalleryDownloadThumbnailBitmap(url: String): Bitmap? {
    if (url.isBlank()) return null
    val request = Request.Builder()
        .url(url)
        .header(
            "User-Agent",
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
        )
        .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
        .header("Referer", "https://nhentai.net/")
        .build()
    return localGalleryDownloadHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        val bytes = response.body?.bytes() ?: return null
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}

internal fun buildGalleryInfoPageBitmap(
    title: String,
    artistName: String,
    code: Int,
    thumbnail: Bitmap?,
    pageWidth: Int,
    pageHeight: Int
): Bitmap {
    val width = pageWidth.coerceAtLeast(720)
    val height = pageHeight.coerceAtLeast(1080)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

    val inset = min(width, height) * 0.025f
    val panelRect = RectF(inset, inset, width - inset, height - inset)
    val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(255, 16, 18, 22)
    }
    val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(226, 18, 20, 24)
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(210, 188, 194, 204)
        textSize = (min(width, height) * 0.0185f).coerceAtLeast(19f)
        isFakeBoldText = true
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(245, 243, 246, 250)
        textSize = (min(width, height) * 0.0275f).coerceAtLeast(28f)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = (min(width, height) * 0.0315f).coerceAtLeast(32f)
        isFakeBoldText = true
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    val roundness = (min(width, height) * 0.028f).coerceAtLeast(24f)
    canvas.drawRoundRect(panelRect, roundness, roundness, panelPaint)

    val clipPath = Path().apply {
        addRoundRect(panelRect, roundness, roundness, Path.Direction.CW)
    }
    val thumbRect = RectF(panelRect)
    thumbnail?.let { bmp ->
        canvas.save()
        canvas.clipPath(clipPath)
        val src = Rect(0, 0, bmp.width, bmp.height)
        val scale = max(thumbRect.width() / bmp.width.toFloat(), thumbRect.height() / bmp.height.toFloat())
        val drawWidth = bmp.width * scale
        val drawHeight = bmp.height * scale
        val dst = RectF(
            thumbRect.centerX() - (drawWidth / 2f),
            thumbRect.centerY() - (drawHeight / 2f),
            thumbRect.centerX() + (drawWidth / 2f),
            thumbRect.centerY() + (drawHeight / 2f)
        )
        canvas.drawBitmap(bmp, src, dst, null)
        canvas.restore()
    }

    val innerPadding = (min(width, height) * 0.024f).coerceAtLeast(20f)
    val metaHeight = panelRect.height() * 0.26f
    val metaTop = panelRect.bottom - metaHeight
    val metaRect = RectF(
        panelRect.left,
        metaTop,
        panelRect.right,
        panelRect.bottom
    )
    canvas.drawRoundRect(metaRect, roundness, roundness, overlayPaint)

    val textInset = innerPadding * 1.24f
    var currentY = metaRect.top + textInset + labelPaint.textSize
    canvas.drawText("LOCAL DOWNLOAD", metaRect.left + textInset, currentY, labelPaint)
    currentY += titlePaint.textSize * 1.08f

    val titleLines = wrapText(title.ifBlank { "Gallery $code" }, titlePaint, (metaRect.width() - (textInset * 2f)).toInt())
    titleLines.take(2).forEach { line ->
        canvas.drawText(line, metaRect.left + textInset, currentY, titlePaint)
        currentY += titlePaint.textSize * 1.02f
    }

    currentY += textInset * 0.62f
    canvas.drawText("Artist: ${artistName.ifBlank { "-" }}", metaRect.left + textInset, currentY, valuePaint)
    currentY += valuePaint.textSize * 1.22f
    canvas.drawText("Code: $code", metaRect.left + textInset, currentY, valuePaint)

    return bitmap
}

internal fun wrapText(text: String, paint: Paint, maxWidthPx: Int): List<String> {
    if (text.isBlank()) return listOf("-")
    val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return listOf("-")
    val lines = mutableListOf<String>()
    var current = ""
    words.forEach { word ->
        val candidate = if (current.isBlank()) word else "$current $word"
        if (paint.measureText(candidate) <= maxWidthPx) {
            current = candidate
        } else {
            if (current.isNotBlank()) lines += current
            current = word
        }
    }
    if (current.isNotBlank()) lines += current
    return lines
}

internal fun normalizeDownloadImageExtension(raw: String?): String {
    return when (raw?.trim()?.lowercase(Locale.US).orEmpty()) {
        "j", "jpg", "jpeg" -> "jpg"
        "p", "png" -> "png"
        "w", "webp" -> "webp"
        "g", "gif" -> "gif"
        else -> ""
    }
}

internal fun imageMimeTypeForExtension(ext: String): String {
    return when (normalizeDownloadImageExtension(ext)) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        else -> "image/jpeg"
    }
}

internal fun buildDownloadGalleryImageUrl(mediaId: Long, pageNumber: Int, extension: String): String {
    return "https://i.nhentai.net/galleries/$mediaId/$pageNumber.$extension"
}

internal fun buildDownloadGalleryPageExtensions(
    preferredExt: String
): List<String> {
    val preferred = normalizeDownloadImageExtension(preferredExt)
    return buildList {
        if (preferred.isNotBlank()) add(preferred)
        add("jpg")
        add("png")
        add("webp")
        add("gif")
    }.distinct()
}

internal fun downloadGalleryPageAsset(
    mediaId: Long,
    pageNumber: Int,
    preferredExt: String
): Pair<ByteArray, String>? {
    if (mediaId <= 0L || pageNumber <= 0) return null
    val extCandidates = buildDownloadGalleryPageExtensions(preferredExt)
    extCandidates.forEach { ext ->
        val request = Request.Builder()
            .url(buildDownloadGalleryImageUrl(mediaId, pageNumber, ext))
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
            )
            .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
            .header("Referer", "https://nhentai.net/")
            .build()
        val fetched = runCatching {
            localGalleryDownloadHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val bytes = response.body?.bytes() ?: return@use null
                bytes to ext
            }
        }.getOrNull()
        if (fetched != null) return fetched
    }
    return null
}

internal fun resolveExistingGalleryDownloadsDirectory(
    context: Context,
    treeUri: Uri
): GalleryDownloadDocumentRef? {
    return runCatching {
        val baseDir = if (loadGalleryDownloadTreeUri(context).isBlank()) {
            documentRefFromUri(context, treeUri, resolveOrCreateBackupContainerUri(context, treeUri))
        } else {
            rootDocumentRef(treeUri)
        }
        findChildDirectoryRef(context, baseDir, GALLERY_DOWNLOADS_DIR_NAME)
    }.getOrNull()
}

internal fun resolveOrCreateGalleryDownloadsDirectory(
    context: Context,
    treeUri: Uri
): GalleryDownloadDocumentRef {
    val baseDir = if (loadGalleryDownloadTreeUri(context).isBlank()) {
        documentRefFromUri(context, treeUri, resolveOrCreateBackupContainerUri(context, treeUri))
    } else {
        rootDocumentRef(treeUri)
    }
    return resolveOrCreateChildDirectory(context, baseDir, GALLERY_DOWNLOADS_DIR_NAME)
}

internal fun documentRefFromUri(
    context: Context,
    treeUri: Uri,
    documentUri: Uri
): GalleryDownloadDocumentRef {
    val documentId = DocumentsContract.getDocumentId(documentUri)
    return GalleryDownloadDocumentRef(
        treeUri = treeUri,
        documentId = documentId,
        documentUri = documentUri,
        displayName = documentId.substringAfterLast('/')
    )
}

internal fun rootDocumentRef(treeUri: Uri): GalleryDownloadDocumentRef {
    val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
    if (treeDocId.isBlank()) throw IOException("Invalid downloads folder URI.")
    return GalleryDownloadDocumentRef(
        treeUri = treeUri,
        documentId = treeDocId,
        documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId),
        displayName = treeDocId.substringAfterLast('/')
    )
}

internal fun listChildDocuments(
    context: Context,
    parent: GalleryDownloadDocumentRef
): List<GalleryDownloadDocumentRef> {
    val resolver = context.applicationContext.contentResolver
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parent.treeUri, parent.documentId)
    return buildList {
        resolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            if (idIndex < 0 || nameIndex < 0) return@use
            while (cursor.moveToNext()) {
                val docId = cursor.getString(idIndex).orEmpty()
                val name = cursor.getString(nameIndex).orEmpty()
                if (docId.isBlank()) continue
                add(
                    GalleryDownloadDocumentRef(
                        treeUri = parent.treeUri,
                        documentId = docId,
                        documentUri = DocumentsContract.buildDocumentUriUsingTree(parent.treeUri, docId),
                        displayName = name
                    )
                )
            }
        }
    }
}

internal fun findChildDirectoryRef(
    context: Context,
    parent: GalleryDownloadDocumentRef,
    displayName: String
): GalleryDownloadDocumentRef? {
    if (displayName.isBlank()) return null
    return listChildDocuments(context, parent)
        .firstOrNull { it.displayName == displayName }
}

internal fun findChildFileRef(
    context: Context,
    parent: GalleryDownloadDocumentRef,
    displayName: String
): GalleryDownloadDocumentRef? {
    if (displayName.isBlank()) return null
    return listChildDocuments(context, parent)
        .firstOrNull { it.displayName == displayName }
}

internal fun resolveOrCreateChildDirectory(
    context: Context,
    parent: GalleryDownloadDocumentRef,
    displayName: String
): GalleryDownloadDocumentRef {
    findChildDirectoryRef(context, parent, displayName)?.let { return it }
    val createdUri = DocumentsContract.createDocument(
        context.applicationContext.contentResolver,
        parent.documentUri,
        DocumentsContract.Document.MIME_TYPE_DIR,
        displayName
    ) ?: throw IOException("Could not create directory '$displayName'.")
    return documentRefFromUri(context, parent.treeUri, createdUri)
}

internal fun resolveOrCreateChildFile(
    context: Context,
    parent: GalleryDownloadDocumentRef,
    displayName: String,
    mimeType: String
): Uri {
    findChildFileRef(context, parent, displayName)?.let { return it.documentUri }
    return DocumentsContract.createDocument(
        context.applicationContext.contentResolver,
        parent.documentUri,
        mimeType,
        displayName
    ) ?: throw IOException("Could not create file '$displayName'.")
}

internal fun readTextFromUri(context: Context, uri: Uri): String {
    return context.applicationContext.contentResolver.openInputStream(uri)?.use { input ->
        input.readBytes().toString(Charsets.UTF_8)
    } ?: throw IOException("Could not read document.")
}

internal fun writeTextToUri(context: Context, uri: Uri, text: String) {
    writeBytesToUri(context, uri, text.toByteArray(Charsets.UTF_8))
}

internal fun writeBytesToUri(context: Context, uri: Uri, bytes: ByteArray) {
    val out = context.applicationContext.contentResolver.openOutputStream(uri, "w")
        ?: context.applicationContext.contentResolver.openOutputStream(uri, "rwt")
        ?: throw IOException("Could not open destination file.")
    out.use {
        it.write(bytes)
        it.flush()
    }
}
