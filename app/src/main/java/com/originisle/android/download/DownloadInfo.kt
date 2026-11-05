package com.originisle.android.download

import android.app.Notification
import android.app.PendingIntent

data class DownloadInfo(
    val title: String,
    val statusText: String,
    val progress: Int, // 0..100
    val isIndeterminate: Boolean,
    val isFinished: Boolean,
    val isCanceled: Boolean = false,
    val speedText: String? = null,
    val contentIntent: PendingIntent? = null,
    val actions: List<Notification.Action> = emptyList(),
)
