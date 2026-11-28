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
import com.originisle.android.bank.TransferNotificationHelper
import com.originisle.android.bank.VietQrDeeplink
import com.originisle.android.bank.VietQrParser
import com.originisle.android.bank.VietnamBankDetector
import com.originisle.android.cards.ScoreCard
import com.originisle.android.sports.FlashscoreShare
import com.originisle.android.ui.PREFS_NAME
import com.originisle.android.ui.PREF_DEFAULT_BANK_PKG
import kotlin.math.max

/**
 * Receives shares and OriginOS Super Drag & Drop payloads:
 * - Image shares: decodes QR code (VietQR -> transfer assist overlay + launch bank app,
 *   including `ACTION_SEND`, `ACTION_SEND_MULTIPLE`, `ACTION_VIEW`).
 * - `text/plain` shares: Flashscore match shares (prioritized first) or bank transfer text
 *   (parsed via [BankTextParser] -> transfer assist overlay + launch bank app).
 */
class ShareToIslandActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ShareToIslandHandler.handleIntent(this, intent)
        finish()
    }
}

object ShareToIslandHandler {

    private const val MAX_QR_IMAGE_PX = 1280

    fun isIncomingShareIntent(intent: Intent?): Boolean {
        val action = intent?.action ?: return false
        return when (action) {
            Intent.ACTION_SEND,
            Intent.ACTION_SEND_MULTIPLE -> true
            Intent.ACTION_VIEW -> {
                val type = intent.type.orEmpty()
                type.startsWith("image/") ||
                    intent.data != null ||
                    extractImageUri(intent) != null ||
                    !extractText(null, intent).isNullOrBlank()
            }
            else -> false
        }
    }

    fun handleIntent(activity: Activity, incoming: Intent?): Boolean {
        if (!isIncomingShareIntent(incoming)) return false

        val action = incoming?.action
        val type = incoming?.type.orEmpty()
        val imageUri = extractImageUri(incoming)
        val text = extractText(activity, incoming)

        val handled = when {
            type.startsWith("image/") -> {
                handleImageShare(activity, imageUri)
                true
            }
            imageUri != null && (text.isNullOrBlank() || action == Intent.ACTION_VIEW) -> {
                handleImageShare(activity, imageUri)
                true
            }
            !text.isNullOrBlank() -> {
                handleTextShare(activity, text)
                true
            }
            imageUri != null -> {
                handleImageShare(activity, imageUri)
                true
            }
            else -> {
                Toast.makeText(
                    activity,
                    "Chỉ hỗ trợ trận đấu Flashscore hoặc thông tin chuyển khoản / mã QR",
                    Toast.LENGTH_SHORT,
                ).show()
                false
            }
        }

        // Prevent re-handling on Activity recreation if routed through MainActivity.
        incoming?.action = Intent.ACTION_MAIN
        return handled
    }

    fun handleClipData(activity: Activity, clip: ClipData?): Boolean {
        if (clip == null || clip.itemCount == 0) return false

        for (i in 0 until clip.itemCount) {
            val item = clip.getItemAt(i) ?: continue
            val uri = item.uri
            if (uri != null) {
                val mime = runCatching { activity.contentResolver.getType(uri) }.getOrNull().orEmpty()
                if (mime.startsWith("image/") || mime.isBlank()) {
                    if (tryHandleImageUri(activity, uri)) {
                        return true
                    }
                }
            }
            val text = item.text?.toString()
                ?: runCatching { item.coerceToText(activity)?.toString() }.getOrNull()
            if (!text.isNullOrBlank()) {
                handleTextShare(activity, text)
                return true
            }
        }

        Toast.makeText(
            activity,
            "Không nhận diện được mã QR hoặc thông tin chuyển khoản",
            Toast.LENGTH_SHORT,
        ).show()
        return false
    }

    private fun tryHandleImageUri(context: Context, imageUri: Uri): Boolean {
        val bitmap = decodeScaledBitmap(context, imageUri, MAX_QR_IMAGE_PX) ?: return false
        val qrText = try {
            decodeQrFromBitmap(bitmap)
        } finally {
            bitmap.recycle()
        }
        if (qrText.isNullOrBlank()) {
            Toast.makeText(context, "Không tìm thấy mã QR trong ảnh", Toast.LENGTH_SHORT).show()
            return true
        }
        val transfer = VietQrParser.parse(qrText)
        if (transfer == null) {
            Toast.makeText(context, "Mã QR không phải VietQR chuyển khoản hợp lệ", Toast.LENGTH_SHORT).show()
            return true
        }
        launchTransferFlow(context, transfer, imageUri)
        return true
    }

    private fun handleImageShare(context: Context, imageUri: Uri?) {
        if (imageUri == null) {
            Toast.makeText(context, "Không đọc được ảnh mã QR", Toast.LENGTH_SHORT).show()
            return
        }
        val bitmap = decodeScaledBitmap(context, imageUri, MAX_QR_IMAGE_PX)
        if (bitmap == null) {
            Toast.makeText(context, "Không thể giải mã ảnh được chia sẻ", Toast.LENGTH_SHORT).show()
            return
        }
        val qrText = try {
            decodeQrFromBitmap(bitmap)
        } finally {
            bitmap.recycle()
        }
        if (qrText.isNullOrBlank()) {
            Toast.makeText(context, "Không tìm thấy mã QR trong ảnh", Toast.LENGTH_SHORT).show()
            return
        }
        val transfer = VietQrParser.parse(qrText)
        if (transfer == null) {
            Toast.makeText(context, "Mã QR không phải VietQR chuyển khoản hợp lệ", Toast.LENGTH_SHORT).show()
            return
        }
        launchTransferFlow(context, transfer, imageUri)
    }

    private fun handleTextShare(context: Context, rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty()) {
            Toast.makeText(
                context,
                "Chỉ hỗ trợ trận đấu Flashscore hoặc thông tin chuyển khoản / mã QR",
                Toast.LENGTH_SHORT,
            ).show()
            return
        }

        val shared = FlashscoreShare.parse(text)
        if (shared != null) {
            ScoreCard.fromShare(context, shared)
            Toast.makeText(
                context,
                "${shared.home} - ${shared.away} is on the island. Follow it in Flashscore to get its updates.",
                Toast.LENGTH_LONG,
            ).show()
            return
        }

        val transfer = BankTextParser.parse(text)
        if (transfer != null) {
            launchTransferFlow(context, transfer, qrImageUri = null)
            return
        }

        Toast.makeText(
            context,
            "Chỉ hỗ trợ trận đấu Flashscore hoặc thông tin chuyển khoản / mã QR",
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun launchTransferFlow(context: Context, transfer: ParsedBankTransfer, qrImageUri: Uri?) {
        if (transfer.accountNumber.isNotBlank()) {
            copyToClipboard(context, "STK", transfer.accountNumber)
        }
        val targetPkg = resolveTargetBankPackage(context)
        if (targetPkg != null) {
            val openedDeeplink = tryOpenVietQrDeeplink(context, transfer, targetPkg)
            val forwardedQr = !openedDeeplink && qrImageUri != null && trySendQrToBankApp(context, qrImageUri, targetPkg)
            if (!openedDeeplink && !forwardedQr) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(targetPkg)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (launchIntent != null) {
                    runCatching { context.startActivity(launchIntent) }
                }
            }
        } else {
            Toast.makeText(
                context,
                "Đã sao chép STK ${transfer.accountNumber}. Chưa tìm thấy app ngân hàng mặc định.",
                Toast.LENGTH_SHORT,
            ).show()
        }

        val shown = TransferAssistOverlay.show(
            context = context,
            transfer = transfer,
            qrImageUri = qrImageUri,
            defaultBankPkg = targetPkg,
        )
        TransferNotificationHelper.show(context, transfer)
        if (!shown && targetPkg != null && transfer.accountNumber.isNotBlank()) {
            Toast.makeText(
                context,
                "Đã sao chép STK ${transfer.accountNumber}",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun tryOpenVietQrDeeplink(
        context: Context,
        transfer: ParsedBankTransfer,
        targetPkg: String,
    ): Boolean {
        val payUri = VietQrDeeplink.buildPayUri(transfer, targetPkg) ?: return false
        val viewIntent = Intent(Intent.ACTION_VIEW, payUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(viewIntent)
            true
        }.getOrDefault(false)
    }

    private fun trySendQrToBankApp(context: Context, qrImageUri: Uri, targetPkg: String): Boolean {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, qrImageUri)
            clipData = ClipData.newUri(context.contentResolver, "QR", qrImageUri)
            setPackage(targetPkg)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (context.packageManager.resolveActivity(sendIntent, 0) == null) {
            return false
        }
        runCatching {
            context.grantUriPermission(targetPkg, qrImageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching {
            context.startActivity(sendIntent)
            true
        }.getOrDefault(false)
    }

    private fun resolveTargetBankPackage(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.contains(PREF_DEFAULT_BANK_PKG)) {
            val saved = prefs.getString(PREF_DEFAULT_BANK_PKG, "").orEmpty()
            if (saved.isNotBlank() && context.packageManager.getLaunchIntentForPackage(saved) != null) {
                return saved
            }
        }
        return VietnamBankDetector.BANK_PACKAGES.keys.firstOrNull { pkg ->
            context.packageManager.getLaunchIntentForPackage(pkg) != null
        }
    }

    private fun extractText(context: Context?, incoming: Intent?): String? {
        if (incoming == null) return null
        val extraText = incoming.getStringExtra(Intent.EXTRA_TEXT)
            ?: incoming.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        if (!extraText.isNullOrBlank()) return extraText

        val htmlText = incoming.getStringExtra(Intent.EXTRA_HTML_TEXT)
        if (!htmlText.isNullOrBlank()) return htmlText

        val clip = incoming.clipData
        if (clip != null && clip.itemCount > 0) {
            for (i in 0 until clip.itemCount) {
                val item = clip.getItemAt(i) ?: continue
                val direct = item.text?.toString()
                if (!direct.isNullOrBlank()) return direct
                if (context != null) {
                    val coerced = runCatching { item.coerceToText(context)?.toString() }.getOrNull()
                    if (!coerced.isNullOrBlank()) return coerced
                }
            }
        }
        return null
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
        val streamList: ArrayList<Uri>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            incoming.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            incoming.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
        }
        val firstInList = streamList?.firstOrNull()
        if (firstInList != null) return firstInList
        if (incoming.data != null) return incoming.data
        val clip = incoming.clipData ?: return null
        if (clip.itemCount > 0) {
            return clip.getItemAt(0)?.uri
        }
        return null
    }

    private fun decodeScaledBitmap(context: Context, uri: Uri, maxDimPx: Int): Bitmap? {
        return runCatching {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { input ->
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
            val decoded = context.contentResolver.openInputStream(uri)?.use { input ->
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

    private fun copyToClipboard(context: Context, label: String, text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}
