package com.originisle.android.ride

enum class RideType {
    TAXI,
    DELIVERY
}

data class RideInfo(
    val appName: String,
    val packageName: String,
    val title: String,
    val driverName: String? = null,
    val licensePlate: String? = null,
    val etaMinutes: Int? = null,
    val statusText: String,
    val type: RideType = RideType.TAXI
)
