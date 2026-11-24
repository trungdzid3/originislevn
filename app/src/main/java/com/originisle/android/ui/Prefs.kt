package com.originisle.android.ui

/** SharedPreferences file name shared by every tab and the onboarding screen. */
const val PREFS_NAME = "experimental_prefs"

/** Pref keys & contract values for Origin Island 2.0.0 customization and pop-up suppression. */
const val PREF_CAST_SUPPRESS_HEADS_UP = "cast_suppress_heads_up"
const val PREF_CAST_SUPPRESS_MODE = "cast_suppress_mode"
const val SUPPRESS_MODE_SNOOZE = "SNOOZE"
const val SUPPRESS_MODE_ISLAND_ONLY = "ISLAND_ONLY"

const val PREF_CAST_ISLAND_DURATION_SEC = "cast_island_duration_sec"
const val DEFAULT_ISLAND_DURATION_SEC = 5

const val PREF_CAST_TAP_EXPAND_CARD = "cast_tap_expand_card"

/** Quick Reply floating overlay & Shizuku background keep-alive preferences (v2.3.0). */
const val PREF_QUICK_REPLY_OVERLAY = "cast_quick_reply_overlay"
const val PREF_SHIZUKU_AUTO_KEEPALIVE = "shizuku_auto_keepalive"
const val PREF_SHIZUKU_LAST_SUMMARY = "shizuku_last_summary"
const val PREF_SHIZUKU_LAST_APPLIED_MS = "shizuku_last_applied_ms"

/** Bluetooth connection popup overlay preference (v2.4.0, default false). */
const val PREF_BT_CONNECT_POPUP = "cast_bt_connect_popup"

/** App theme & Material Design 3 color theme preferences (v2.7.1). */
const val PREF_APP_THEME = "app_theme"
const val PREF_APP_THEME_COLOR = "app_theme_color"
