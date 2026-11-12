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
