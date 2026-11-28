package com.originisle.android.bank

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.originisle.android.R

/**
 * Fallback heads-up / notification bar helper for bank transfers.
 * Ensures users can 1-tap copy STK, amount, and transfer note even when
 * overlay permission is missing or the banking app blocks floating windows.
 */
object TransferNotificationHelper {

    private const val CHANNEL_ID = "transfer_assist"
    private const val CHANNEL_NAME = "Hỗ trợ chuyển khoản"
    private const val NOTIFICATION_ID = 32030

    fun show(context: Context, transfer: ParsedBankTransfer) {
        val appContext = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        ensureChannel(nm)

        val title = buildString {
            append(transfer.bankName.ifBlank { "Chuyển khoản" })
            if (transfer.accountNumber.isNotBlank()) {
                append(" - ")
                append(transfer.accountNumber)
            }
        }
        val parts = buildList {
            if (transfer.accountNumber.isNotBlank()) add("Đã copy STK")
            if (transfer.amount.isNotBlank()) add("Số tiền: ${transfer.amount}đ")
            if (transfer.note.isNotBlank()) add("ND: ${transfer.note}")
        }
        val body = if (parts.isEmpty()) "Chạm nút bên dưới để sao chép nhanh" else parts.joinToString(" | ")

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)

        var reqCode = 1
        if (transfer.accountNumber.isNotBlank()) {
            builder.addAction(
                0,
                "Copy STK",
                buildCopyPendingIntent(appContext, reqCode++, "STK", transfer.accountNumber),
            )
        }
        if (transfer.amount.isNotBlank()) {
            builder.addAction(
                0,
                "Copy tiền",
                buildCopyPendingIntent(appContext, reqCode++, "Số tiền", transfer.amount),
            )
        }
        if (transfer.note.isNotBlank()) {
            builder.addAction(
                0,
                "Copy ND",
                buildCopyPendingIntent(appContext, reqCode, "Nội dung", transfer.note),
            )
        }

        runCatching { nm.notify(NOTIFICATION_ID, builder.build()) }
    }

    private fun ensureChannel(nm: NotificationManager) {
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Hiển thị nút sao chép nhanh STK, số tiền và nội dung chuyển khoản"
            enableVibration(false)
            setSound(null, null)
        }
        nm.createNotificationChannel(channel)
    }

    private fun buildCopyPendingIntent(
        context: Context,
        requestCode: Int,
        label: String,
        value: String,
    ): PendingIntent {
        val intent = Intent(context, TransferCopyReceiver::class.java).apply {
            action = TransferCopyReceiver.ACTION_COPY_FIELD
            putExtra(TransferCopyReceiver.EXTRA_LABEL, label)
            putExtra(TransferCopyReceiver.EXTRA_VALUE, value)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

class TransferCopyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_COPY_FIELD) return
        val label = intent.getStringExtra(EXTRA_LABEL).orEmpty().ifBlank { "Thông tin" }
        val value = intent.getStringExtra(EXTRA_VALUE).orEmpty()
        if (value.isBlank()) return

        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        cm.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(context, "Đã sao chép $label: $value", Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val ACTION_COPY_FIELD = "com.originisle.android.ACTION_COPY_TRANSFER_FIELD"
        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_VALUE = "extra_value"
    }
}
