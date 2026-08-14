package com.roinur.saucetracker.core.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

internal class DocumentTreeStorage(
    context: Context,
    val treeUri: Uri
) {
    private val resolver = context.applicationContext.contentResolver

    fun persistReadWritePermission() {
        resolver.takePersistableUriPermission(
            treeUri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    fun childDocumentUri(documentId: String): Uri =
        DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)

    fun childrenUri(documentId: String): Uri =
        DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)

    fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): Uri? =
        DocumentsContract.createDocument(
            resolver,
            childDocumentUri(parentDocumentId),
            mimeType,
            displayName
        )

    fun delete(documentUri: Uri): Boolean = DocumentsContract.deleteDocument(resolver, documentUri)
}
