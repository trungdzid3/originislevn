package com.originisle.android.smart

import android.net.Uri

enum class SmartCardType {
    FLIGHT,    // Chuyến bay (Vietnam Airlines, Vietjet, Bamboo, v.v.)
    TICKET,    // Vé tàu xe, vé xem phim, sự kiện
    DELIVERY,  // Mã đơn hàng, lấy đồ ăn (Grab/ShopeeFood), mã bưu kiện (SPX, GHTK, ViettelPost)
}

data class SmartInfo(
    val type: SmartCardType,
    val title: String,
    val code: String,
    val detail: String,
    val subtitle: String,
    val dateString: String? = null,
    val timeString: String? = null,
    val sourceUri: Uri? = null,
)
