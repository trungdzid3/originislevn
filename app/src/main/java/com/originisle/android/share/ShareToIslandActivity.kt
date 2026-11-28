package com.originisle.android.share

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.originisle.android.bank.BankTextParser
import com.originisle.android.bank.ParsedBankTransfer
import com.originisle.android.bank.TransferAssistOverlay
import com.originisle.android.bank.VietQrParser
import com.originisle.android.bank.VietnamBankDetector
import com.originisle.android.cards.ScoreCard
import com.originisle.android.sports.FlashscoreShare
import com.originisle.android.ui.PREFS_NAME
import com.originisle.android.ui.PREF_DEFAULT_BANK_PKG
import kotlin.math.max

/**
 * "Show on island" in the share sheet.
 * Handles:
 * - Image shares: decodes QR code (VietQR -> transfer assist overlay + launch bank app,
 *   or generic QR text/URL -> copy to clipboard).
 * - `text/plain` shares: Flashscore match shares (prioritized first) or bank transfer text
 *   (parsed via [BankTextParser] -> transfer assist overlay + launch bank app).
 */
class ShareToIslandActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val incoming = intent
        val mimeType = incoming?.type.orEmpty()
        val imageUri = extractImageUri(incoming)

        if (mimeType.startsWith("image/") || (mimeType.isEmpty() && imageUri != null)) {
            handleImageShare(imageUri)
        } else {
            handleTextShare(incoming)
        }

        finish()
    }

    private fun handleImageShare(imageUri: Uri?) {
        if (imageUri == null) {
            Toast.makeText(applicationContext, "Không đọc được ảnh được chia sẻ", Toast.LENGTH_SHORT).show()
            return
        }
        val bitmap = decodeScaledBitmap(imageUri, MAX_QR_IMAGE_PX)
        if (bitmap == null) {
            Toast.makeText(applicationContext, "Không thể giải mã ảnh QR", Toast.LENGTH_SHORT).show()
            return
        }

        val qrText = try {
            decodeQrFromBitmap(bitmap)
        } finally {
            bitmap.recycle()
        }

        if (qrText.isNullOrBlank()) {
            Toast.makeText(applicationContext, "Không tìm thấy mã QR trong ảnh", Toast.LENGTH_SHORT).show()
            return
        }

        val transfer = VietQrParser.parse(qrText)
        if (transfer != null) {
            launchTransferFlow(transfer, qrImageUri = imageUri)
        } else {
            copyToClipboard("QR", qrText)
            Toast.makeText(applicationContext, "Đã sao chép nội dung QR", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleTextShare(incoming: Intent?) {
        val text = listOfNotNull(
            incoming?.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString(),
            incoming?.getStringExtra(Intent.EXTRA_SUBJECT),
        ).joinToString("\n")

        val shared = FlashscoreShare.parse(text)
        if (shared != null) {
            ScoreCard.fromShare(applicationContext, shared)
            Toast.makeText(
                applicationContext,
                "${shared.home} - ${shared.away} is on the island. Follow it in Flashscore to get its updates.",
                Toast.LENGTH_LONG,
            ).show()
            return
        }

        val transfer = BankTextParser.parse(text)
        if (transfer != null) {
            launchTransferFlow(transfer, qrImageUri = null)
            return
        }

        Toast.makeText(
            applicationContext,
            "Chỉ hỗ trợ trận đấu Flashscore hoặc thông tin chuyển khoản / mã QR",
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun launchTransferFlow(transfer: ParsedBankTransfer, qrImageUri: Uri?) {
        if (transfer.accountNumber.isNotBlank()) {
            copyToClipboard("STK", transfer.accountNumber)
        }

        val targetBankPkg = resolveTargetBankPackage()
        if (!targetBankPkg.isNullOrBlank()) {
            runCatching {
                packageManager.getLaunchIntentForPackage(targetBankPkg)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }?.let { startActivity(it) }
            }
        }

        val shown = TransferAssistOverlay.show(
            context = applicationContext,
            transfer = transfer,
            qrImageUri = qrImageUri,
            defaultBankPkg = targetBankPkg,
        )
        if (!shown) {
            Toast.makeText(
                applicationContext,
                "Đã sao chép STK ${transfer.accountNumber} (${transfer.bankName}). Cấp quyền hiển thị trên ứng dụng khác để hiện bảng hỗ trợ.",
                Toast.LENGTH_LONG,
            ).show()
        } else {
            Toast.makeText(
                applicationContext,
                "Đã sao chép STK ${transfer.accountNumber} (${transfer.bankName})",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun resolveTargetBankPackage(): String? {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (prefs.contains(PREF_DEFAULT_BANK_PKG)) {
            val saved = prefs.getString(PREF_DEFAULT_BANK_PKG, "").orEmpty()
            if (saved.isBlank()) return null
            return if (packageManager.getLaunchIntentForPackage(saved) != null) saved else null
        }
        return VietnamBankDetector.BANK_PACKAGES.keys.firstOrNull { pkg ->
            packageManager.getLaunchIntentForPackage(pkg) != null
        }
    }

    private fun extractImageUri(incoming: Intent?): Uri? {
        if (incoming == null) return null
        val streamUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            incoming.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            incoming.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
        }
        if (streamUri != null) return streamUri
        val clip = incoming.clipData ?: return null
        if (clip.itemCount > 0) {
            return clip.getItemAt(0)?.uri
        }
        return null
    }

    private fun decodeScaledBitmap(uri: Uri, maxDimPx: Int): Bitmap? {
        return runCatching {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, boundsOpts)
            }
            val srcW = boundsOpts.outWidth
            val srcH = boundsOpts.outHeight
            if (srcW <= 0 || srcH <= 0) return@runCatching null

            var sampleSize = 1
            while (max(srcW / sampleSize, srcH / sampleSize) > maxDimPx * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOpts)
            } ?: return@runCatching null

            val maxSide = max(decoded.width, decoded.height)
            if (maxSide <= maxDimPx) {
                decoded
            } else {
                val scale = maxDimPx.toFloat() / maxSide.toFloat()
                val targetW = (decoded.width * scale).toInt().coerceAtLeast(1)
                val targetH = (decoded.height * scale).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(decoded, targetW, targetH, true)
                if (scaled !== decoded) decoded.recycle()
                scaled
            }
        }.getOrNull()
    }

    private fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val reader = QRCodeReader()
        val hints = mapOf(DecodeHintType.TRY_HARDER to true)

        val hybridResult = runCatching {
            reader.decode(BinaryBitmap(HybridBinarizer(source)), hints)?.text
        }.getOrNull()
        if (!hybridResult.isNullOrBlank()) return hybridResult

        reader.reset()
        return runCatching {
            reader.decode(BinaryBitmap(GlobalHistogramBinarizer(source)), hints)?.text
        }.getOrNull()
    }

    private fun copyToClipboard(label: String, text: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    private companion object {
        const val MAX_QR_IMAGE_PX = 1280
    }
}
