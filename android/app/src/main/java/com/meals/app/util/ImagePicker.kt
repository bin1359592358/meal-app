package com.meals.app.util

import android.content.Context
import android.net.Uri
import com.meals.app.data.remote.ApiClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Image upload utility for picking and uploading images to the server.
 *
 * Supports gallery selection via ActivityResultContracts.GetContent.
 * Uploads to the backend /api/upload/image endpoint.
 */
object ImagePicker {

    val supportedMimeTypes = listOf(
        "image/jpeg",
        "image/png",
        "image/webp"
    )

    const val MAX_FILE_SIZE = 5 * 1024 * 1024L
    const val MAX_DIMENSION = 2048

    /**
     * Upload an image from a content URI to the server.
     *
     * @param context Android context for resolving the URI
     * @param uri The content URI of the selected image
     * @return Result containing the image URL string on success, or an exception on failure
     */
    suspend fun uploadImage(context: Context, uri: Uri): Result<String> {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
            val inputStream = contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("无法读取图片文件"))

            val bytes = inputStream.use { it.readBytes() }

            if (bytes.size > MAX_FILE_SIZE) {
                return Result.failure(Exception("图片大小超过限制（最大5MB）"))
            }

            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val fileName = "upload_${System.currentTimeMillis()}.${getFileExtension(mimeType)}"
            val part = MultipartBody.Part.createFormData("file", fileName, requestBody)

            val api = ApiClient.getApiService()
            val response = api.uploadImage(part)
            val body = response.body()

            if (response.isSuccessful && body?.code == 0 && body.data != null) {
                val url = body.data["url"]
                if (url != null) {
                    Result.success(url)
                } else {
                    Result.failure(Exception("服务器未返回图片地址"))
                }
            } else {
                Result.failure(Exception(body?.message ?: "上传失败"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("上传失败: ${e.message}"))
        }
    }

    private fun getFileExtension(mimeType: String): String {
        return when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
    }
}
