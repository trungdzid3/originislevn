package com.originisle.android.smart

import android.content.Context
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ScreenshotObserver(private val context: Context) : ContentObserver(Handler(Looper.getMainLooper())) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastProcessedUri: Uri? = null
    private var lastProcessedTime: Long = 0

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        checkNewScreenshot()
    }

    private fun checkNewScreenshot() {
        val prefs = context.getSharedPreferences("cast_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("cast_smart_screenshot", true)) return

        scope.launch {
            try {
                val projection = arrayOf(
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DATE_ADDED,
                    MediaStore.Images.Media.DISPLAY_NAME,
                )

                val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
                val cursor = context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder,
                )

                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val idIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                        val dataIndex = c.getColumnIndex(MediaStore.Images.Media.DATA)
                        val dateIndex = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                        val nameIndex = c.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)

                        val id = c.getLong(idIndex)
                        val path = if (dataIndex >= 0) c.getString(dataIndex).orEmpty() else ""
                        val name = if (nameIndex >= 0) c.getString(nameIndex).orEmpty() else ""
                        val dateAddedSec = c.getLong(dateIndex)
                        val nowSec = System.currentTimeMillis() / 1000

                        // Chỉ xử lý ảnh chụp màn hình được tạo trong vòng 10 giây qua
                        if (nowSec - dateAddedSec > 10) return@use

                        val lower = "$path $name".lowercase()
                        val isScreenshot = lower.contains("screenshot") || lower.contains("chụp màn hình") || lower.contains("screen_shot")
                        if (!isScreenshot) return@use

                        val imageUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                        if (imageUri == lastProcessedUri && (System.currentTimeMillis() - lastProcessedTime < 8000)) {
                            return@use
                        }

                        lastProcessedUri = imageUri
                        lastProcessedTime = System.currentTimeMillis()

                        Log.d("ScreenshotObserver", "Phát hiện ảnh chụp màn hình mới: $imageUri")
                        processScreenshot(imageUri)
                    }
                }
            } catch (e: Throwable) {
                Log.w("ScreenshotObserver", "Lỗi đọc ảnh chụp màn hình: ${e.message}")
            }
        }
    }

    private suspend fun processScreenshot(uri: Uri) {
        val bitmap = loadOptimizedBitmap(uri) ?: return
        val smartInfo = ScreenshotSmartExtractor.extract(bitmap, uri)
        if (smartInfo != null) {
            Log.i("ScreenshotObserver", "Bóc tách thành công thông tin thông minh: ${smartInfo.title}")
            ScreenshotCard.post(context, smartInfo)
        }
    }

    private fun loadOptimizedBitmap(uri: Uri): Bitmap? = try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }

        val maxDim = 1280
        var sampleSize = 1
        val maxSide = maxOf(options.outWidth, options.outHeight)
        if (maxSide > maxDim) {
            sampleSize = maxSide / maxDim
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        }
    } catch (_: Throwable) {
        null
    }
}
