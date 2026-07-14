package io.duckling.contestpulse.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.duckling.contestpulse.R

enum class ContestPulseDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    Contests(
        route = "contests",
        labelRes = R.string.nav_contests,
        iconRes = R.drawable.ic_contests,
    ),
    Favorites(
        route = "favorites",
        labelRes = R.string.nav_favorites,
        iconRes = R.drawable.ic_favorites,
    ),
    Settings(
        route = "settings",
        labelRes = R.string.nav_settings,
        iconRes = R.drawable.ic_settings,
    ),
}
