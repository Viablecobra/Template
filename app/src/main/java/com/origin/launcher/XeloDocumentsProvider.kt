package com.origin.launcher

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Point
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileNotFoundException

class XeloDocumentsProvider : DocumentsProvider() {
    
    override fun attachInfo(context: Context, info: DocumentsContract.ProviderInfo) {
        super.attachInfo(context, info)
    }

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val cursor = MatrixCursor(
            arrayOf(
                "root_id", "document_id", "flags", "icon", "title", 
                "summary", "available_bytes"
            )
        )
        cursor.addRow(arrayOf(
            "xelo_root", "xelo_root", 0,
            android.R.drawable.ic_menu_gallery,
            "Xelo Client", "Internal Storage",
            8000000000L
        ))
        return cursor
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        queryArgs: Bundle?
    ): Cursor {
        val dir = context?.filesDir ?: return MatrixCursor(emptyArray())
        val cursor = MatrixCursor(projection ?: emptyArray())
        dir.listFiles()?.forEach { file ->
            includeFile(cursor, file.name, file)
        }
        return cursor
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String
    ): Cursor {
        return queryChildDocuments(parentDocumentId, projection, null)
    }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val file = File(context?.filesDir, documentId)
        val cursor = MatrixCursor(projection ?: emptyArray())
        includeFile(cursor, documentId, file)
        return cursor
    }

    private fun includeFile(cursor: MatrixCursor, docId: String, file: File) {
        cursor.newRow()
            .add(docId)
            .add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, file.name)
            .add(DocumentsContract.Document.COLUMN_MIME_TYPE, 
                if (file.isDirectory) DocumentsContract.Document.MIME_TYPE_DIR 
                else MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    MimeTypeMap.getFileExtensionFromUrl(file.name)
                ) ?: "*/*"
            )
            .add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, file.lastModified())
            .add(DocumentsContract.Document.COLUMN_SIZE, file.length())
            .add(DocumentsContract.Document.COLUMN_FLAGS, 0)
    }

    override fun getDocumentType(documentId: String): String {
        return DocumentsContract.Document.MIME_TYPE_DIR
    }

    override fun openDocumentThumbnail(
        documentId: String,
        sizeHint: Point,
        signal: CancellationSignal
    ): AssetFileDescriptor {
        throw FileNotFoundException("Thumbnails not supported")
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        val file = File(context?.filesDir, documentId)
        val accessMode = ParcelFileDescriptor.parseMode(mode)
        return ParcelFileDescriptor.open(file, accessMode)
    }
}