package com.snowsplit.app.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalPizza
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector

data class BubbleIconOption(
    val key: String,
    val icon: ImageVector
)

object BubbleIconCatalog {
    val options: List<BubbleIconOption> = listOf(
        BubbleIconOption("person", Icons.Outlined.Person),
        BubbleIconOption("coffee", Icons.Outlined.Coffee),
        BubbleIconOption("pizza", Icons.Outlined.LocalPizza),
        BubbleIconOption("gamepad", Icons.Outlined.SportsEsports),
        BubbleIconOption("car", Icons.Outlined.DirectionsCar),
        BubbleIconOption("heart", Icons.Outlined.FavoriteBorder),
        BubbleIconOption("flight", Icons.Outlined.Flight),
        BubbleIconOption("home", Icons.Outlined.Home)
    )

    fun resolve(key: String): ImageVector =
        options.firstOrNull { it.key == key }?.icon ?: Icons.Outlined.Person
}
