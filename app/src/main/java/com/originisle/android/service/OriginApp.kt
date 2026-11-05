package com.originisle.android.service

import android.content.Context
import com.originisle.android.cards.RingerModeCard

/**
 * Cung cấp điểm khởi tạo và đăng ký các dịch vụ nền tảng cho Origin Isle.
 */
object OriginApp {

    fun init(context: Context) {
        RingerModeCard.register(context)
    }
}
