package com.jermey.seal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.compose.NavigationHost
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeNavigator
import com.jermey.seal.demo.SealTheme
import com.jermey.seal.demo.navigation.ComposeAppNavigationConfig
import com.jermey.seal.demo.navigation.MainDestination

@OptIn(InternalQuoVadisApi::class)
@Composable
fun App() {
    SealTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
                val config = ComposeAppNavigationConfig
                val initialState = remember {
                    config.buildNavNode(
                        destinationClass = MainDestination::class,
                        parentKey = null,
                    )!!
                }
                val navigator = remember {
                    TreeNavigator(
                        config = config,
                        initialState = initialState,
                    )
                }
                NavigationHost(navigator = navigator)
            }
        }
    }
}