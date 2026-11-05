package com.originisle.android.download

import android.app.PendingIntent

data class DownloadInfo(
    val title: String,
    val statusText: String,
    val progress: Int, // 0..100
    val isIndeterminate: Boolean,
    val isFinished: Boolean,
    val speedText: String? = null,
    val contentIntent: PendingIntent? = null,
)

