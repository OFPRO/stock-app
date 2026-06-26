package com.app2.core.data.network

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.ResponseBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun saveAndGetUri(pdfBytes: ByteArray, filename: String): Uri {
        val dir = File(context.cacheDir, "pdfs")
        dir.mkdirs()
        val file = File(dir, filename)
        file.writeBytes(pdfBytes)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    suspend fun saveResponseAndGetUri(response: ResponseBody, filename: String): Uri {
        val bytes = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            response.bytes()
        }
        return saveAndGetUri(bytes, filename)
    }
}
