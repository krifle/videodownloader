package com.example.videodownloader.data.media

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import com.example.videodownloader.data.extract.ExtractError

class AndroidMediaStoreSaver(
    context: Context,
) : MediaSaver {
    private val contentResolver = context.applicationContext.contentResolver

    override fun saveVideo(
        request: SaveVideoRequest,
        writer: (java.io.OutputStream) -> WriteResult,
    ): MediaSaveResult {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, request.fileName)
            put(MediaStore.Video.Media.MIME_TYPE, request.mimeType)
            put(MediaStore.Video.Media.RELATIVE_PATH, request.relativePath)
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = contentResolver.insert(collection, values)
            ?: return MediaSaveResult.Failure(ExtractError.SaveFailed)

        return try {
            val writeResult = contentResolver.openOutputStream(uri)?.use { output ->
                writer(output)
            } ?: WriteResult.Failure(ExtractError.SaveFailed)

            when (writeResult) {
                is WriteResult.Success -> {
                    val completeValues = ContentValues().apply {
                        put(MediaStore.Video.Media.IS_PENDING, 0)
                    }
                    contentResolver.update(uri, completeValues, null, null)
                    MediaSaveResult.Success(
                        fileName = request.fileName,
                        bytesWritten = writeResult.bytesWritten,
                    )
                }
                is WriteResult.Failure -> {
                    contentResolver.delete(uri, null, null)
                    MediaSaveResult.Failure(writeResult.error)
                }
            }
        } catch (_: Exception) {
            contentResolver.delete(uri, null, null)
            MediaSaveResult.Failure(ExtractError.SaveFailed)
        }
    }
}
