package com.github.kr328.clash.service

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import com.github.kr328.clash.core.Global
import com.github.kr328.clash.service.data.ClashDatabase

class ProfileDocumentProvider : DocumentsProvider() {
    companion object {
        private const val DEFAULT_ROOT_ID = 0
    }

    val database = ClashDatabase.getInstance(Global.application)

    override fun openDocument(
        documentId: String?,
        mode: String?,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        TODO("Not yet implemented")
    }

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        TODO("Not yet implemented")
    }

    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor {
        TODO("Not yet implemented")
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        return MatrixCursor(projection).apply {
            newRow().apply {
                add(DocumentsContract.Root.COLUMN_ROOT_ID, DEFAULT_ROOT_ID)

            }
        }
    }
}