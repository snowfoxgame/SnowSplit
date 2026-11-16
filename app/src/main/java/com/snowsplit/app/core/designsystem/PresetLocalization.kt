package com.snowsplit.app.core.designsystem

import androidx.annotation.StringRes
import com.snowsplit.app.R

@StringRes
fun ThemePreset.titleRes(): Int = when (id) {
    "standard_m3" -> R.string.preset_standard_m3_title
    "winter" -> R.string.preset_winter_title
    "halloween" -> R.string.preset_halloween_title
    "christmas" -> R.string.preset_christmas_title
    "summer" -> R.string.preset_summer_title
    "autumn" -> R.string.preset_autumn_title
    "spring" -> R.string.preset_spring_title
    "easter" -> R.string.preset_easter_title
    "new_year" -> R.string.preset_new_year_title
    "gaming" -> R.string.preset_gaming_title
    "8bit_cute" -> R.string.preset_8bit_cute_title
    "interactive_bubbles" -> R.string.preset_interactive_bubbles_title
    "mini_city" -> R.string.preset_mini_city_title
    "weather_time" -> R.string.preset_weather_time_title
    "8bit_cat" -> R.string.preset_8bit_cat_title
    "8bit_fox" -> R.string.preset_8bit_fox_title
    "8bit_bunny" -> R.string.preset_8bit_bunny_title
    "8bit_snake" -> R.string.preset_8bit_snake_title
    else -> R.string.preset_standard_m3_title
}

@StringRes
fun ThemePreset.subtitleRes(): Int = when (id) {
    "standard_m3" -> R.string.preset_standard_m3_subtitle
    "winter" -> R.string.preset_winter_subtitle
    "halloween" -> R.string.preset_halloween_subtitle
    "christmas" -> R.string.preset_christmas_subtitle
    "summer" -> R.string.preset_summer_subtitle
    "autumn" -> R.string.preset_autumn_subtitle
    "spring" -> R.string.preset_spring_subtitle
    "easter" -> R.string.preset_easter_subtitle
    "new_year" -> R.string.preset_new_year_subtitle
    "gaming" -> R.string.preset_gaming_subtitle
    "8bit_cute" -> R.string.preset_8bit_cute_subtitle
    "interactive_bubbles" -> R.string.preset_interactive_bubbles_subtitle
    "mini_city" -> R.string.preset_mini_city_subtitle
    "weather_time" -> R.string.preset_weather_time_subtitle
    "8bit_cat" -> R.string.preset_8bit_cat_subtitle
    "8bit_fox" -> R.string.preset_8bit_fox_subtitle
    "8bit_bunny" -> R.string.preset_8bit_bunny_subtitle
    "8bit_snake" -> R.string.preset_8bit_snake_subtitle
    else -> R.string.preset_standard_m3_subtitle
}

@StringRes
fun ThemeCategory.titleRes(): Int = when (this) {
    ThemeCategory.NORMAL -> R.string.appearance_category_normal
    ThemeCategory.EVENTS -> R.string.appearance_category_events
    ThemeCategory.PLAYFUL -> R.string.appearance_category_playful
    ThemeCategory.INTERACTIVE -> R.string.appearance_category_interactive
}
