package com.jermey.seal

import androidx.compose.ui.window.ComposeUIViewController
import com.jermey.seal.demo.di.appModule
import com.jermey.seal.demo.di.containerModule
import org.koin.core.context.startKoin

private var koinInitialized = false

fun MainViewController() = ComposeUIViewController {
    if (!koinInitialized) {
        startKoin {
            modules(appModule, containerModule)
        }
        koinInitialized = true
    }
    App()
}