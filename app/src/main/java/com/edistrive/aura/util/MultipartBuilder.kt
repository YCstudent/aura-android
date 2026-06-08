package com.edistrive.aura.util

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

object MultipartBuilder {
    fun imagePartFromUri(
        context: Context,
        uri: Uri,
        fieldName: String = "image",
        fileName: String? = null
    ): MultipartBody.Part? {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/jpeg"
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        val name = fileName ?: defaultFileName(uri, mime)
        val body = bytes.toRequestBody(mime.toMediaTypeOrNull(), 0, bytes.size)
        return MultipartBody.Part.createFormData(fieldName, name, body)
    }

    private fun defaultFileName(uri: Uri, mime: String): String {
        val ext = when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            mime.contains("heic") -> "heic"
            else -> "jpg"
        }
        val raw = uri.lastPathSegment?.substringAfterLast('/') ?: "upload"
        val cleaned = raw.replace(Regex("[^A-Za-z0-9._-]"), "_").take(40)
        return "${cleaned}_${System.currentTimeMillis()}.$ext"
    }
}
