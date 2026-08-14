package com.roinur.saucetracker.data.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.IOException

internal const val PROCEDURAL_BACKUP_CURRENT_FILENAME = "procedural_backup.txt"
internal const val PROCEDURAL_BACKUP_PREVIOUS_1_FILENAME = "procedural_backup_previous_1.txt"
internal const val PROCEDURAL_BACKUP_PREVIOUS_2_FILENAME = "procedural_backup_previous_2.txt"
private const val PROCEDURAL_BACKUP_PENDING_FILENAME = "procedural_backup_pending.txt"

private data class ProceduralBackupDocument(
    val uri: Uri,
    val displayName: String
)

internal fun readCurrentProceduralBackupTextOrNull(context: Context, treeUri: Uri): String? {
    val rootUri = resolveOrCreateBackupContainerUri(context.applicationContext, treeUri)
    val current = listProceduralBackupDocuments(context, treeUri, rootUri)
        .firstOrNull { it.displayName == PROCEDURAL_BACKUP_CURRENT_FILENAME }
        ?: return null
    return runCatching { readProceduralBackupText(context, current.uri) }.getOrNull()
}

internal fun writeRollingProceduralBackup(
    context: Context,
    treeUri: Uri,
    text: String,
    validator: (String) -> Boolean
) {
    val appContext = context.applicationContext
    val rootUri = resolveOrCreateBackupContainerUri(appContext, treeUri)
    var documents = listProceduralBackupDocuments(appContext, treeUri, rootUri)
    documents.firstOrNull { it.displayName == PROCEDURAL_BACKUP_PENDING_FILENAME }?.let { stale ->
        DocumentsContract.deleteDocument(appContext.contentResolver, stale.uri)
    }

    val pendingUri = DocumentsContract.createDocument(
        appContext.contentResolver,
        rootUri,
        "text/plain",
        PROCEDURAL_BACKUP_PENDING_FILENAME
    ) ?: throw IOException("Could not create the pending procedural backup file.")

    try {
        writeProceduralBackupText(appContext, pendingUri, text)
        val verifiedText = readProceduralBackupText(appContext, pendingUri)
        if (verifiedText != text || !validator(verifiedText)) {
            throw IOException("The new procedural backup failed read-back validation.")
        }

        documents = listProceduralBackupDocuments(appContext, treeUri, rootUri)
        val current = documents.firstOrNull { it.displayName == PROCEDURAL_BACKUP_CURRENT_FILENAME }
        val previous1 = documents.firstOrNull { it.displayName == PROCEDURAL_BACKUP_PREVIOUS_1_FILENAME }
        val previous2 = documents.firstOrNull { it.displayName == PROCEDURAL_BACKUP_PREVIOUS_2_FILENAME }

        previous2?.let { old ->
            if (!DocumentsContract.deleteDocument(appContext.contentResolver, old.uri)) {
                throw IOException("Could not rotate Previous 2 procedural backup.")
            }
        }
        previous1?.let { old ->
            renameProceduralBackup(
                context = appContext,
                document = old,
                nextName = PROCEDURAL_BACKUP_PREVIOUS_2_FILENAME
            )
        }

        val rotatedCurrentUri = current?.let { old ->
            renameProceduralBackup(
                context = appContext,
                document = old,
                nextName = PROCEDURAL_BACKUP_PREVIOUS_1_FILENAME
            )
        }

        try {
            val installedCurrent = DocumentsContract.renameDocument(
                appContext.contentResolver,
                pendingUri,
                PROCEDURAL_BACKUP_CURRENT_FILENAME
            )
            if (installedCurrent == null) {
                throw IOException("Could not promote the validated procedural backup to Current.")
            }
        } catch (failure: Throwable) {
            if (rotatedCurrentUri != null) {
                runCatching {
                    DocumentsContract.renameDocument(
                        appContext.contentResolver,
                        rotatedCurrentUri,
                        PROCEDURAL_BACKUP_CURRENT_FILENAME
                    ) ?: throw IOException("Could not restore Current procedural backup after rotation failure.")
                }.getOrElse { restoreFailure ->
                    failure.addSuppressed(restoreFailure)
                }
            }
            throw failure
        }
    } catch (failure: Throwable) {
        runCatching { DocumentsContract.deleteDocument(appContext.contentResolver, pendingUri) }
        throw failure
    }
}

private fun renameProceduralBackup(
    context: Context,
    document: ProceduralBackupDocument,
    nextName: String
): Uri {
    return DocumentsContract.renameDocument(context.contentResolver, document.uri, nextName)
        ?: throw IOException("Could not rotate ${document.displayName} to $nextName.")
}

private fun listProceduralBackupDocuments(
    context: Context,
    treeUri: Uri,
    rootUri: Uri
): List<ProceduralBackupDocument> {
    val rootDocumentId = DocumentsContract.getDocumentId(rootUri)
    if (rootDocumentId.isBlank()) throw IOException("Invalid backup folder URI.")
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootDocumentId)
    val rows = mutableListOf<ProceduralBackupDocument>()
    context.contentResolver.query(
        childrenUri,
        arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        ),
        null,
        null,
        null
    )?.use { cursor ->
        val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        if (idIndex < 0 || nameIndex < 0) return@use
        while (cursor.moveToNext()) {
            val id = cursor.getString(idIndex).orEmpty()
            val name = cursor.getString(nameIndex).orEmpty()
            if (id.isNotBlank() && name.isNotBlank()) {
                rows += ProceduralBackupDocument(
                    uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id),
                    displayName = name
                )
            }
        }
    }
    return rows
}

private fun readProceduralBackupText(context: Context, uri: Uri): String {
    return context.contentResolver.openInputStream(uri)?.use { input ->
        input.readBytes().toString(Charsets.UTF_8)
    } ?: throw IOException("Could not read the procedural backup file.")
}

private fun writeProceduralBackupText(context: Context, uri: Uri, text: String) {
    val output = context.contentResolver.openOutputStream(uri, "wt")
        ?: context.contentResolver.openOutputStream(uri, "w")
        ?: throw IOException("Could not write the procedural backup file.")
    output.use { stream ->
        stream.write(text.toByteArray(Charsets.UTF_8))
        stream.flush()
    }
}
