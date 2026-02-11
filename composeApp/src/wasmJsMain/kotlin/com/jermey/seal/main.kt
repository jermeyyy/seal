package com.jermey.seal

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.jermey.seal.demo.di.appModule
import com.jermey.seal.demo.di.containerModule
import kotlinx.browser.document
import org.koin.core.context.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    startKoin {
        modules(appModule, containerModule)
    }
    val body = document.body ?: return
    ComposeViewport(body) {
        App()
    }
}
