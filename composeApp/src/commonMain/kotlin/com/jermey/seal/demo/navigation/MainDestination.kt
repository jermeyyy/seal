package com.jermey.seal.demo.navigation

import com.jermey.quo.vadis.annotations.Argument
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

@Stack(name = "main", startDestination = MainDestination.Home::class)
sealed class MainDestination : NavDestination {
    @Destination(route = "main/home")
    data object Home : MainDestination()

    @Destination(route = "main/details/{url}/{resultJson}")
    data class Details(
        @Argument val url: String,
        @Argument val resultJson: String,
    ) : MainDestination()
}
