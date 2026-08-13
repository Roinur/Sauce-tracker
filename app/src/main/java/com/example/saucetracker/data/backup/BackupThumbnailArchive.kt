package com.example.saucetracker.data.backup

import com.example.saucetracker.*

import com.example.saucetracker.core.media.computeDHash64
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.DocumentsContract
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Executors

const val KEY_BACKUP_THUMBNAIL_ARCHIVE_ENABLED = "backup_thumbnail_archive_enabled"

private const val BACKUP_ARCHIVE_PREFS_NAME = "nhtagbook_prefs"
private const val BACKUP_ARCHIVE_TREE_URI_KEY = "auto_backup_tree_uri"
private const val BACKUP_ROOT_DIR_NAME = "SauceTracker Backup"
private const val BACKUP_ROOT_NOMEDIA_NAME = ".nomedia"
private const val BACKUP_ARCHIVE_DIR_NAME = "thumbnail_archive"
private const val BACKUP_ARCHIVE_THUMBS_DIR_NAME = "covers"
private const val BACKUP_ARCHIVE_HASHES_DIR_NAME = "dhash"
private const val BACKUP_ARCHIVE_THUMB_EXTENSION = ".stthumb"
private const val BACKUP_ARCHIVE_LEGACY_THUMB_EXTENSION = ".jpg"
private const val BACKUP_ARCHIVE_THUMB_MIME = "application/octet-stream"
private const val BACKUP_ARCHIVE_DEFAULT_ESTIMATED_THUMB_BYTES = 92_000L
private const val BACKUP_ARCHIVE_HASH_BYTES_PER_ITEM = 24L
private const val BACKUP_ARCHIVE_JPEG_QUALITY = 82
private val BACKUP_ARCHIVE_THUMB_MAGIC = byteArrayOf(0x53, 0x54, 0x42, 0x31)
private const val BACKUP_ARCHIVE_OBFUSCATION_SEED = 0x4F1BBCDC0A61D2E5L

data class BackupThumbnailStorageEstimate(
    val entryCount: Int,
    val storedCount: Int,
    val estimatedTotalBytes: Long
)

data class BackupThumbnailSyncResult(
    val syncedCount: Int,
    val totalBytes: Long,
    val writtenCount: Int,
    val reusedCount: Int,
    val failedCount: Int
)

data class BackupThumbnailSyncProgress(
    val processedCount: Int,
    val totalCount: Int,
    val writtenCount: Int,
    val reusedCount: Int,
    val failedCount: Int
)

private data class BackupDocumentRef(
    val treeUri: Uri,
    val documentId: String,
    val documentUri: Uri,
    val displayName: String,
    val sizeBytes: Long = -1L
)

private data class BackupThumbnailSeedPlan(
    val seed: LocalDuplicateSeed,
    val existingThumb: BackupDocumentRef?,
    val legacyThumb: BackupDocumentRef?,
    val existingHash: BackupDocumentRef?
)

private object BackupThumbnailArchiveIndex {
    private var indexedTreeUriString: String = ""
    private var coversByCode: Map<Int, BackupDocumentRef> = emptyMap()

    @Synchronized
    fun clear() {
        indexedTreeUriString = ""
        coversByCode = emptyMap()
    }

    @Synchronized
    fun coversByCode(context: Context, treeUriString: String): Map<Int, BackupDocumentRef> {
        if (treeUriString.isBlank()) return emptyMap()
        if (indexedTreeUriString == treeUriString && coversByCode.isNotEmpty()) {
            return coversByCode
        }
        val treeUri = Uri.parse(treeUriString)
        val coversDir = resolveExistingBackupRootDirectory(context, treeUri)
            ?.let { findChildDirectoryRef(context, it, BACKUP_ARCHIVE_DIR_NAME) }
            ?.let { findChildDirectoryRef(context, it, BACKUP_ARCHIVE_THUMBS_DIR_NAME) }
        val next = coversDir
            ?.let { listChildDocuments(context, it) }
            ?.let { buildBackupDocumentMapByCode(it, preferArchivedThumb = true) }
            .orEmpty()
        indexedTreeUriString = treeUriString
        coversByCode = next
        return next
    }
}

private fun backupArchivePrefs(context: Context) =
    context.applicationContext.getSharedPreferences(BACKUP_ARCHIVE_PREFS_NAME, Context.MODE_PRIVATE)

fun isBackupThumbnailArchiveEnabled(context: Context): Boolean {
    return backupArchivePrefs(context).getBoolean(KEY_BACKUP_THUMBNAIL_ARCHIVE_ENABLED, false)
}

fun loadBackupThumbnailArchiveTreeUri(context: Context): String {
    return backupArchivePrefs(context).getString(BACKUP_ARCHIVE_TREE_URI_KEY, "").orEmpty()
}

fun formatStorageSize(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L)
    if (safe < 1024L) return "$safe B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = safe.toDouble()
    var unitIndex = -1
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex += 1
    }
    val rounded = if (value >= 100.0) {
        String.format(Locale.US, "%.0f", value)
    } else if (value >= 10.0) {
        String.format(Locale.US, "%.1f", value)
    } else {
        String.format(Locale.US, "%.2f", value)
    }
    return "$rounded ${units[unitIndex.coerceAtLeast(0)]}"
}

fun computeBackupThumbnailArchiveEstimate(
    context: Context,
    treeUriString: String,
    seeds: List<LocalDuplicateSeed>
): BackupThumbnailStorageEstimate {
    val eligibleCount = seeds.count { it.code > 0 && it.thumbnailUrl.isNotBlank() }
    if (eligibleCount <= 0) {
        return BackupThumbnailStorageEstimate(entryCount = 0, storedCount = 0, estimatedTotalBytes = 0L)
    }
    val treeUri = treeUriString.trim().takeIf { it.isNotBlank() }?.let(Uri::parse)
    val storedRefs = if (treeUri != null) {
        runCatching {
            val coversDir = resolveExistingBackupRootDirectory(context, treeUri)
                ?.let { findChildDirectoryRef(context, it, BACKUP_ARCHIVE_DIR_NAME) }
                ?.let { findChildDirectoryRef(context, it, BACKUP_ARCHIVE_THUMBS_DIR_NAME) }
            coversDir?.let { listChildDocuments(context, it) }.orEmpty()
        }.getOrDefault(emptyList())
    } else {
        emptyList()
    }
    val storedCount = buildBackupDocumentMapByCode(storedRefs, preferArchivedThumb = true).size
    val storedBytes = storedRefs.sumOf { it.sizeBytes.coerceAtLeast(0L) }
    val averageBytes = when {
        storedCount > 0 && storedBytes > 0L -> (storedBytes / storedCount).coerceAtLeast(1L)
        else -> BACKUP_ARCHIVE_DEFAULT_ESTIMATED_THUMB_BYTES
    }
    val estimatedThumbBytes = if (eligibleCount <= storedCount) {
        storedBytes.coerceAtLeast(eligibleCount * BACKUP_ARCHIVE_HASH_BYTES_PER_ITEM)
    } else {
        storedBytes + ((eligibleCount - storedCount).toLong() * averageBytes)
    }
    val estimatedHashBytes = eligibleCount.toLong() * BACKUP_ARCHIVE_HASH_BYTES_PER_ITEM
    return BackupThumbnailStorageEstimate(
        entryCount = eligibleCount,
        storedCount = storedCount.coerceAtMost(eligibleCount),
        estimatedTotalBytes = estimatedThumbBytes + estimatedHashBytes
    )
}

fun readBackupThumbnailHashForCode(context: Context, code: Int): Long? {
    if (code <= 0) return null
    if (!isBackupThumbnailArchiveEnabled(context)) return null
    val treeUriString = loadBackupThumbnailArchiveTreeUri(context).trim()
    if (treeUriString.isBlank()) return null
    return runCatching {
        val hashesDir = resolveExistingBackupRootDirectory(context, Uri.parse(treeUriString))
            ?.let { findChildDirectoryRef(context, it, BACKUP_ARCHIVE_DIR_NAME) }
            ?.let { findChildDirectoryRef(context, it, BACKUP_ARCHIVE_HASHES_DIR_NAME) }
            ?: return@runCatching null
        val file = findBackupDocumentByCode(listChildDocuments(context, hashesDir), code)
            ?: return@runCatching null
        val raw = readTextFromUri(context, file.documentUri).trim()
        raw.toLongOrNull()
    }.getOrNull()
}

fun readBackupThumbnailHashesByCode(
    context: Context,
    codes: Set<Int>? = null
): Map<Int, Long> {
    if (!isBackupThumbnailArchiveEnabled(context)) return emptyMap()
    val treeUriString = loadBackupThumbnailArchiveTreeUri(context).trim()
    if (treeUriString.isBlank()) return emptyMap()
    val allowedCodes = codes
        ?.asSequence()
        ?.filter { it > 0 }
        ?.toHashSet()
        ?.takeIf { it.isNotEmpty() }
    return runCatching {
        val hashesDir = resolveExistingBackupRootDirectory(context, Uri.parse(treeUriString))
            ?.let { findChildDirectoryRef(context, it, BACKUP_ARCHIVE_DIR_NAME) }
            ?.let { findChildDirectoryRef(context, it, BACKUP_ARCHIVE_HASHES_DIR_NAME) }
            ?: return@runCatching emptyMap()
        val hashFilesByCode = buildBackupDocumentMapByCode(
            listChildDocuments(context, hashesDir),
            preferArchivedThumb = false
        )
        buildMap {
            hashFilesByCode.forEach { (code, file) ->
                if (allowedCodes != null && code !in allowedCodes) return@forEach
                val hash = readTextFromUri(context, file.documentUri).trim().toLongOrNull() ?: return@forEach
                put(code, hash)
            }
        }
    }.getOrDefault(emptyMap())
}

fun readBackupThumbnailBitmapForCode(context: Context, code: Int): Bitmap? {
    if (code <= 0) return null
    if (!isBackupThumbnailArchiveEnabled(context)) return null
    val treeUriString = loadBackupThumbnailArchiveTreeUri(context).trim()
    if (treeUriString.isBlank()) return null
    return runCatching {
        val file = BackupThumbnailArchiveIndex.coversByCode(context, treeUriString)[code]
            ?: return@runCatching null
        decodeArchivedBitmap(context, code, file)
    }.getOrNull()
}

fun readBackupThumbnailBitmapsByCode(context: Context, codes: Set<Int>): Map<Int, Bitmap> {
    val requestedCodes = codes.filter { it > 0 }.toSet()
    if (requestedCodes.isEmpty()) return emptyMap()
    if (!isBackupThumbnailArchiveEnabled(context)) return emptyMap()
    val treeUriString = loadBackupThumbnailArchiveTreeUri(context).trim()
    if (treeUriString.isBlank()) return emptyMap()
    return runCatching {
        val coversByCode = BackupThumbnailArchiveIndex.coversByCode(context, treeUriString)
        requestedCodes.mapNotNull { code ->
            val file = coversByCode[code] ?: return@mapNotNull null
            decodeArchivedBitmap(context, code, file)?.let { bitmap -> code to bitmap }
        }.toMap()
    }.getOrDefault(emptyMap())
}

fun syncBackupThumbnailArchive(
    context: Context,
    treeUriString: String,
    seeds: List<LocalDuplicateSeed>,
    fetchBitmap: (String) -> Bitmap?,
    onProgress: ((BackupThumbnailSyncProgress) -> Unit)? = null
): BackupThumbnailSyncResult {
    BackupThumbnailArchiveIndex.clear()
    val treeUri = treeUriString.trim().takeIf { it.isNotBlank() }?.let(Uri::parse)
        ?: throw IOException("Set procedural backup folder first.")
    val eligibleSeeds = seeds
        .asSequence()
        .filter { it.code > 0 && it.thumbnailUrl.isNotBlank() }
        .distinctBy { it.code }
        .sortedBy { it.code }
        .toList()

    val backupRoot = resolveOrCreateBackupRootDirectory(context, treeUri)
    val archiveRoot = resolveOrCreateChildDirectory(context, backupRoot, BACKUP_ARCHIVE_DIR_NAME)
    val coversDir = resolveOrCreateChildDirectory(context, archiveRoot, BACKUP_ARCHIVE_THUMBS_DIR_NAME)
    val hashesDir = resolveOrCreateChildDirectory(context, archiveRoot, BACKUP_ARCHIVE_HASHES_DIR_NAME)
    val existingCoverFilesByCode = buildBackupDocumentMapByCode(
        listChildDocuments(context, coversDir),
        preferArchivedThumb = true
    )
    val existingHashFilesByCode = buildBackupDocumentMapByCode(
        listChildDocuments(context, hashesDir),
        preferArchivedThumb = false
    )
    val seedPlans = eligibleSeeds.map { seed ->
        BackupThumbnailSeedPlan(
            seed = seed,
            existingThumb = existingCoverFilesByCode[seed.code]
                ?.takeIf { it.displayName.endsWith(BACKUP_ARCHIVE_THUMB_EXTENSION, ignoreCase = true) },
            legacyThumb = existingCoverFilesByCode[seed.code]
                ?.takeIf { !it.displayName.endsWith(BACKUP_ARCHIVE_THUMB_EXTENSION, ignoreCase = true) },
            existingHash = existingHashFilesByCode[seed.code]
        )
    }

    var syncedCount = 0
    var totalBytes = 0L
    var writtenCount = 0
    var reusedCount = 0
    var failedCount = 0
    val activeCodes = seedPlans.map { it.seed.code.toString() }.toSet()
    val totalCount = seedPlans.size

    fun dispatchProgress(processedCount: Int) {
        onProgress?.invoke(
            BackupThumbnailSyncProgress(
                processedCount = processedCount.coerceIn(0, totalCount),
                totalCount = totalCount,
                writtenCount = writtenCount,
                reusedCount = reusedCount,
                failedCount = failedCount
            )
        )
    }

    dispatchProgress(processedCount = 0)

    if (seedPlans.isNotEmpty()) {
        val parallelism = Runtime.getRuntime().availableProcessors()
            .coerceAtLeast(2)
            .coerceAtMost(8)
        val executor = Executors.newFixedThreadPool(parallelism)
        val completion = java.util.concurrent.ExecutorCompletionService<BackupThumbnailSyncResult>(executor)
        try {
            seedPlans.forEach { plan ->
                completion.submit(
                    Callable {
                        syncSingleBackupThumbnailSeed(
                            context = context,
                            coversDir = coversDir,
                            hashesDir = hashesDir,
                            plan = plan,
                            fetchBitmap = fetchBitmap
                        )
                    }
                )
            }
            repeat(seedPlans.size) { completedIndex ->
                val result = completion.take().get()
                syncedCount += result.syncedCount
                totalBytes += result.totalBytes
                writtenCount += result.writtenCount
                reusedCount += result.reusedCount
                failedCount += result.failedCount
                dispatchProgress(processedCount = completedIndex + 1)
            }
        } finally {
            executor.shutdownNow()
        }
    }

    cleanupStaleBackupChildren(context, coversDir, activeCodes, BACKUP_ARCHIVE_THUMB_EXTENSION)
    cleanupStaleBackupChildren(context, coversDir, activeCodes, BACKUP_ARCHIVE_LEGACY_THUMB_EXTENSION)
    cleanupStaleBackupChildren(context, hashesDir, activeCodes, ".txt")
    BackupThumbnailArchiveIndex.clear()

    return BackupThumbnailSyncResult(
        syncedCount = syncedCount,
        totalBytes = totalBytes,
        writtenCount = writtenCount,
        reusedCount = reusedCount,
        failedCount = failedCount
    )
}

fun resolveOrCreateBackupContainerUri(context: Context, treeUri: Uri): Uri {
    return resolveOrCreateBackupRootDirectory(context, treeUri).documentUri
}

private fun syncSingleBackupThumbnailSeed(
    context: Context,
    coversDir: BackupDocumentRef,
    hashesDir: BackupDocumentRef,
    plan: BackupThumbnailSeedPlan,
    fetchBitmap: (String) -> Bitmap?
): BackupThumbnailSyncResult {
    val seed = plan.seed
    val thumbName = "${seed.code}$BACKUP_ARCHIVE_THUMB_EXTENSION"
    val hashName = "${seed.code}.txt"
    val existingThumb = plan.existingThumb
    val legacyThumb = plan.legacyThumb
    val existingHash = plan.existingHash

    if (existingThumb != null && existingHash != null) {
        return BackupThumbnailSyncResult(
            syncedCount = 1,
            totalBytes = existingThumb.sizeBytes.coerceAtLeast(0L),
            writtenCount = 0,
            reusedCount = 1,
            failedCount = 0
        )
    }

    val bitmap = when {
        existingThumb != null -> decodeArchivedBitmap(context, seed.code, existingThumb)
        legacyThumb != null -> decodeArchivedBitmap(context, seed.code, legacyThumb)
        else -> fetchBitmap(seed.thumbnailUrl)
    } ?: return BackupThumbnailSyncResult(
        syncedCount = 0,
        totalBytes = 0L,
        writtenCount = 0,
        reusedCount = 0,
        failedCount = 1
    )

    try {
        val dHash = runCatching { computeDHash64(bitmap) }.getOrNull()
            ?: return BackupThumbnailSyncResult(
                syncedCount = 0,
                totalBytes = 0L,
                writtenCount = 0,
                reusedCount = 0,
                failedCount = 1
            )

        var storedBytesSize = existingThumb?.sizeBytes?.coerceAtLeast(0L) ?: 0L
        var wroteAnything = false

        if (existingThumb == null) {
            val jpegBytes = ByteArrayOutputStream().use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, BACKUP_ARCHIVE_JPEG_QUALITY, out)) {
                    return BackupThumbnailSyncResult(
                        syncedCount = 0,
                        totalBytes = 0L,
                        writtenCount = 0,
                        reusedCount = 0,
                        failedCount = 1
                    )
                }
                out.toByteArray()
            }
            val storedBytes = encodeBackupThumbnailBlob(seed.code, jpegBytes)
            val thumbUri = resolveOrCreateChildFile(context, coversDir, thumbName, BACKUP_ARCHIVE_THUMB_MIME)
            writeBytesToUri(context, thumbUri, storedBytes)
            storedBytesSize = storedBytes.size.toLong()
            wroteAnything = true
        }

        if (existingHash == null) {
            val hashUri = resolveOrCreateChildFile(context, hashesDir, hashName, "text/plain")
            writeTextToUri(context, hashUri, dHash.toString())
            wroteAnything = true
        }

        if (legacyThumb != null) {
            deleteDocumentQuietly(context, legacyThumb.documentUri)
            wroteAnything = true
        }

        return BackupThumbnailSyncResult(
            syncedCount = 1,
            totalBytes = storedBytesSize,
            writtenCount = if (wroteAnything) 1 else 0,
            reusedCount = if (wroteAnything) 0 else 1,
            failedCount = 0
        )
    } finally {
        if (!bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}

private fun rootDocumentRef(treeUri: Uri): BackupDocumentRef {
    val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
    if (treeDocId.isBlank()) {
        throw IOException("Invalid backup folder URI.")
    }
    return BackupDocumentRef(
        treeUri = treeUri,
        documentId = treeDocId,
        documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId),
        displayName = treeDocId
    )
}

private fun listChildDocuments(context: Context, parent: BackupDocumentRef): List<BackupDocumentRef> {
    val resolver = context.applicationContext.contentResolver
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parent.treeUri, parent.documentId)
    return buildList {
        resolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_SIZE
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            if (idIdx < 0 || nameIdx < 0) return@use
            while (cursor.moveToNext()) {
                val docId = cursor.getString(idIdx).orEmpty()
                val name = cursor.getString(nameIdx).orEmpty()
                if (docId.isBlank() || name.isBlank()) continue
                add(
                    BackupDocumentRef(
                        treeUri = parent.treeUri,
                        documentId = docId,
                        documentUri = DocumentsContract.buildDocumentUriUsingTree(parent.treeUri, docId),
                        displayName = name,
                        sizeBytes = if (sizeIdx >= 0) cursor.getLong(sizeIdx).coerceAtLeast(0L) else -1L
                    )
                )
            }
        }
    }
}

private fun findDirectoryRef(context: Context, treeUri: Uri, displayName: String): BackupDocumentRef? {
    return listChildDocuments(context, rootDocumentRef(treeUri)).firstOrNull { it.displayName == displayName }
}

private fun findBackupRootDirectory(context: Context, treeUri: Uri): BackupDocumentRef? {
    return findDirectoryRef(context, treeUri, BACKUP_ROOT_DIR_NAME)
}

private fun resolveExistingBackupRootDirectory(context: Context, treeUri: Uri): BackupDocumentRef? {
    val root = rootDocumentRef(treeUri)
    val rootName = queryDocumentDisplayName(context, root).ifBlank { root.displayName }
    return if (rootName == BACKUP_ROOT_DIR_NAME || looksLikeBackupRootDirectory(context, root)) {
        root.copy(displayName = rootName)
    } else {
        findBackupRootDirectory(context, treeUri)
    }
}

private fun findChildDirectoryRef(
    context: Context,
    parent: BackupDocumentRef,
    displayName: String
): BackupDocumentRef? {
    return listChildDocuments(context, parent).firstOrNull { it.displayName == displayName }
}

private fun findChildFileRef(
    context: Context,
    parent: BackupDocumentRef,
    displayName: String
): BackupDocumentRef? {
    return listChildDocuments(context, parent).firstOrNull { it.displayName == displayName }
}

private fun findArchivedThumbRef(
    context: Context,
    parent: BackupDocumentRef,
    code: Int
): BackupDocumentRef? {
    if (code <= 0) return null
    return findBackupDocumentByCode(
        children = listChildDocuments(context, parent),
        code = code,
        preferArchivedThumb = true
    )
}

private fun resolveOrCreateChildDirectory(
    context: Context,
    parent: BackupDocumentRef,
    displayName: String
): BackupDocumentRef {
    findChildDirectoryRef(context, parent, displayName)?.let { return it }
    val created = DocumentsContract.createDocument(
        context.applicationContext.contentResolver,
        parent.documentUri,
        DocumentsContract.Document.MIME_TYPE_DIR,
        displayName
    ) ?: throw IOException("Could not create backup folder '$displayName'.")
    val createdId = DocumentsContract.getDocumentId(created)
    return BackupDocumentRef(
        treeUri = parent.treeUri,
        documentId = createdId,
        documentUri = created,
        displayName = displayName
    )
}

private fun resolveOrCreateBackupRootDirectory(context: Context, treeUri: Uri): BackupDocumentRef {
    val root = rootDocumentRef(treeUri)
    val rootName = queryDocumentDisplayName(context, root).ifBlank { root.displayName }
    val backupRoot = if (rootName == BACKUP_ROOT_DIR_NAME || looksLikeBackupRootDirectory(context, root)) {
        root.copy(displayName = rootName)
    } else {
        resolveOrCreateChildDirectory(context, root, BACKUP_ROOT_DIR_NAME)
    }
    ensureBackupRootNoMedia(context, backupRoot)
    return backupRoot
}

private fun ensureBackupRootNoMedia(context: Context, backupRoot: BackupDocumentRef) {
    runCatching {
        val uri = resolveOrCreateChildFile(
            context = context,
            parent = backupRoot,
            displayName = BACKUP_ROOT_NOMEDIA_NAME,
            mimeType = "application/octet-stream"
        )
        writeBytesToUri(context, uri, ByteArray(0))
    }
}

private fun resolveOrCreateChildFile(
    context: Context,
    parent: BackupDocumentRef,
    displayName: String,
    mimeType: String
): Uri {
    findChildFileRef(context, parent, displayName)?.let { return it.documentUri }
    return DocumentsContract.createDocument(
        context.applicationContext.contentResolver,
        parent.documentUri,
        mimeType,
        displayName
    ) ?: throw IOException("Could not create backup file '$displayName'.")
}

private fun cleanupStaleBackupChildren(
    context: Context,
    parent: BackupDocumentRef,
    activeCodes: Set<String>,
    suffix: String
) {
    val resolver = context.applicationContext.contentResolver
    listChildDocuments(context, parent).forEach { child ->
        val stem = extractBackupCodeString(child.displayName)
            ?: child.displayName.removeSuffix(suffix).takeIf { it != child.displayName }
        if (stem.isNullOrBlank() || stem in activeCodes) return@forEach
        runCatching { DocumentsContract.deleteDocument(resolver, child.documentUri) }
    }
}

private fun writeBytesToUri(context: Context, uri: Uri, bytes: ByteArray) {
    val resolver = context.applicationContext.contentResolver
    val out = resolver.openOutputStream(uri, "w")
        ?: throw IOException("Could not open destination file.")
    out.use {
        it.write(bytes)
        it.flush()
    }
}

private fun readBytesFromUri(context: Context, uri: Uri): ByteArray {
    return context.applicationContext.contentResolver.openInputStream(uri)?.use { input ->
        input.readBytes()
    } ?: throw IOException("Could not open source file.")
}

private fun readTextFromUri(context: Context, uri: Uri): String {
    return readBytesFromUri(context, uri).toString(Charsets.UTF_8)
}

private fun writeTextToUri(context: Context, uri: Uri, text: String) {
    writeBytesToUri(context, uri, text.toByteArray(Charsets.UTF_8))
}

private fun decodeArchivedBitmap(
    context: Context,
    code: Int,
    file: BackupDocumentRef
): Bitmap? {
    val raw = readBytesFromUri(context, file.documentUri)
    val decoded = when {
        file.displayName.endsWith(BACKUP_ARCHIVE_THUMB_EXTENSION, ignoreCase = true) ->
            decodeBackupThumbnailBlob(code, raw)
        else -> raw
    } ?: return null
    val options = BitmapFactory.Options().apply {
        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
    }
    return BitmapFactory.decodeByteArray(decoded, 0, decoded.size, options)
}

private fun encodeBackupThumbnailBlob(code: Int, jpegBytes: ByteArray): ByteArray {
    val payload = applyBackupThumbnailXor(code, jpegBytes)
    return ByteArray(BACKUP_ARCHIVE_THUMB_MAGIC.size + payload.size).also { out ->
        BACKUP_ARCHIVE_THUMB_MAGIC.copyInto(out, startIndex = 0)
        payload.copyInto(out, destinationOffset = BACKUP_ARCHIVE_THUMB_MAGIC.size)
    }
}

private fun decodeBackupThumbnailBlob(code: Int, storedBytes: ByteArray): ByteArray? {
    if (storedBytes.size <= BACKUP_ARCHIVE_THUMB_MAGIC.size) return null
    if (!storedBytes.copyOfRange(0, BACKUP_ARCHIVE_THUMB_MAGIC.size).contentEquals(BACKUP_ARCHIVE_THUMB_MAGIC)) {
        return null
    }
    val payload = storedBytes.copyOfRange(BACKUP_ARCHIVE_THUMB_MAGIC.size, storedBytes.size)
    return applyBackupThumbnailXor(code, payload)
}

private fun applyBackupThumbnailXor(code: Int, bytes: ByteArray): ByteArray {
    var state = BACKUP_ARCHIVE_OBFUSCATION_SEED xor code.toLong()
    return ByteArray(bytes.size) { index ->
        state = (state * 6364136223846793005L) + 1442695040888963407L + index.toLong()
        (bytes[index].toInt() xor ((state ushr 56).toInt() and 0xFF)).toByte()
    }
}

private fun deleteDocumentQuietly(context: Context, uri: Uri) {
    runCatching {
        DocumentsContract.deleteDocument(context.applicationContext.contentResolver, uri)
    }
}

private fun queryDocumentDisplayName(context: Context, document: BackupDocumentRef): String {
    return runCatching {
        context.applicationContext.contentResolver.query(
            document.documentUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val idx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                cursor.getString(idx).orEmpty()
            } else {
                ""
            }
        }.orEmpty()
    }.getOrDefault("")
}

private fun looksLikeBackupRootDirectory(context: Context, directory: BackupDocumentRef): Boolean {
    return runCatching {
        findChildDirectoryRef(context, directory, BACKUP_ARCHIVE_DIR_NAME) != null
    }.getOrDefault(false)
}

private fun extractBackupCodeString(displayName: String): String? {
    val trimmed = displayName.trim()
    if (trimmed.isBlank()) return null
    val digits = trimmed.takeWhile { it.isDigit() }
    return digits.takeIf { it.isNotBlank() && digits.toIntOrNull()?.let { code -> code > 0 } == true }
}

private fun extractBackupCode(displayName: String): Int? {
    return extractBackupCodeString(displayName)?.toIntOrNull()?.takeIf { it > 0 }
}

private fun backupDocumentPriority(displayName: String, preferArchivedThumb: Boolean): Int {
    val lower = displayName.trim().lowercase(Locale.US)
    return when {
        preferArchivedThumb && lower.endsWith(BACKUP_ARCHIVE_THUMB_EXTENSION) -> 0
        lower.endsWith(".txt") -> 0
        preferArchivedThumb && lower.endsWith(BACKUP_ARCHIVE_LEGACY_THUMB_EXTENSION) -> 1
        else -> 2
    }
}

private fun buildBackupDocumentMapByCode(
    children: List<BackupDocumentRef>,
    preferArchivedThumb: Boolean
): Map<Int, BackupDocumentRef> {
    val out = linkedMapOf<Int, BackupDocumentRef>()
    children.forEach { child ->
        val code = extractBackupCode(child.displayName) ?: return@forEach
        val existing = out[code]
        if (existing == null) {
            out[code] = child
        } else {
            val currentPriority = backupDocumentPriority(child.displayName, preferArchivedThumb)
            val existingPriority = backupDocumentPriority(existing.displayName, preferArchivedThumb)
            if (currentPriority < existingPriority) {
                out[code] = child
            }
        }
    }
    return out
}

private fun findBackupDocumentByCode(
    children: List<BackupDocumentRef>,
    code: Int,
    preferArchivedThumb: Boolean = false
): BackupDocumentRef? {
    if (code <= 0) return null
    return buildBackupDocumentMapByCode(children, preferArchivedThumb)[code]
}
