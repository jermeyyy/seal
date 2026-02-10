package com.jermey.seal.demo.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.compose.scope.LocalNavigator
import com.jermey.quo.vadis.core.navigation.config.NavigationConfig
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.registry.ContainerRegistry
import com.jermey.quo.vadis.core.registry.DeepLinkRegistry
import com.jermey.quo.vadis.core.registry.ScopeRegistry
import com.jermey.quo.vadis.core.registry.ScreenRegistry
import com.jermey.quo.vadis.core.registry.TransitionRegistry
import com.jermey.seal.demo.ui.details.DetailsScreen
import com.jermey.seal.demo.ui.home.HomeScreen
import kotlin.reflect.KClass
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Hand-written navigation config equivalent to what the Quo Vadis KSP processor
 * would generate. KSP cannot be used because the `com.google.devtools.ksp` Gradle
 * plugin is incompatible with the `com.android.kotlin.multiplatform.library` AGP
 * plugin (ClassCastException on KotlinMultiplatformAndroidCompilationImpl).
 *
 * When KSP compatibility is resolved, this file can be deleted and replaced by
 * applying the `io.github.jermeyyy.quo-vadis` Gradle plugin with KSP.
 */
@OptIn(InternalQuoVadisApi::class, ExperimentalUuidApi::class)
object ComposeAppNavigationConfig : NavigationConfig {

    override val screenRegistry: ScreenRegistry = object : ScreenRegistry {
        @Composable
        override fun Content(
            destination: NavDestination,
            sharedTransitionScope: SharedTransitionScope?,
            animatedVisibilityScope: AnimatedVisibilityScope?,
        ) {
            val navigator = LocalNavigator.current ?: return
            when (destination) {
                is MainDestination.Home -> HomeScreen(navigator)
                is MainDestination.Details -> DetailsScreen(navigator)
            }
        }

        override fun hasContent(destination: NavDestination): Boolean =
            destination is MainDestination
    }

    override val scopeRegistry: ScopeRegistry = object : ScopeRegistry {
        override fun isInScope(scopeKey: String, destination: NavDestination): Boolean =
            scopeKey == SCOPE_KEY && destination is MainDestination

        override fun getScopeKey(destination: NavDestination): String? =
            if (destination is MainDestination) SCOPE_KEY else null
    }

    override val transitionRegistry: TransitionRegistry = TransitionRegistry.Empty

    override val containerRegistry: ContainerRegistry = ContainerRegistry.Empty

    override val deepLinkRegistry: DeepLinkRegistry = DeepLinkRegistry.Empty

    override fun buildNavNode(
        destinationClass: KClass<out NavDestination>,
        key: String?,
        parentKey: String?,
    ): NavNode? {
        if (destinationClass != MainDestination::class) return null
        val stackKey = key ?: Uuid.random().toString()
        val screenKey = Uuid.random().toString()
        return StackNode(
            key = stackKey,
            parentKey = parentKey,
            children = listOf(
                ScreenNode(
                    key = screenKey,
                    parentKey = stackKey,
                    destination = MainDestination.Home,
                ),
            ),
            scopeKey = SCOPE_KEY,
        )
    }

    override fun plus(other: NavigationConfig): NavigationConfig = other

    private const val SCOPE_KEY = "MainDestination"
}
