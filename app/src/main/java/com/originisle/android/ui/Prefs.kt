package com.originisle.android.ui

/** SharedPreferences file name shared by every tab and the onboarding screen. */
const val PREFS_NAME = "experimental_prefs"

/** Pref keys & contract values for Origin Island 2.0.0 customization and pop-up suppression. */
const val PREF_CAST_SUPPRESS_HEADS_UP = "cast_suppress_heads_up"
const val PREF_CAST_SUPPRESS_MODE = "cast_suppress_mode"
const val SUPPRESS_MODE_SNOOZE = "SNOOZE"
const val SUPPRESS_MODE_ISLAND_ONLY = "ISLAND_ONLY"

const val PREF_CAST_CUSTOM_LIGHT_COLOR = "cast_custom_light_color"
const val LIGHT_COLOR_AUTO = "AUTO"
const val LIGHT_COLOR_NONE = "NONE"
const val DEFAULT_CAST_CUSTOM_LIGHT_COLOR = LIGHT_COLOR_NONE
const val LIGHT_COLOR_EMERALD = "#00D084"
const val LIGHT_COLOR_BLUE = "#0A59F7"
const val LIGHT_COLOR_PURPLE = "#9D4EDD"
const val LIGHT_COLOR_AMBER = "#FFB020"

const val PREF_CAST_ISLAND_DURATION_SEC = "cast_island_duration_sec"
const val DEFAULT_ISLAND_DURATION_SEC = 5

const val PREF_CAST_TAP_EXPAND_CARD = "cast_tap_expand_card"

/** Pref keys & defaults for Samsung One UI 5.1+ overlay island positioning & scaling. */
const val PREF_ONEUI_OFFSET_X_PX = "oneui_island_offset_x_px"
const val PREF_ONEUI_OFFSET_Y_PX = "oneui_island_offset_y_px"
const val PREF_ONEUI_PILL_SCALE_PCT = "oneui_island_pill_scale_pct"
const val PREF_ONEUI_HIDE_LANDSCAPE = "oneui_island_hide_landscape"
const val DEFAULT_ONEUI_OFFSET_X_PX = 0
const val DEFAULT_ONEUI_OFFSET_Y_PX = 0
const val DEFAULT_ONEUI_PILL_SCALE_PCT = 100
const val DEFAULT_ONEUI_HIDE_LANDSCAPE = true
