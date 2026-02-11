package com.jermey.seal

import androidx.compose.ui.window.singleWindowApplication
import com.jermey.seal.demo.di.appModule
import com.jermey.seal.demo.di.containerModule
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(appModule, containerModule)
    }
    singleWindowApplication(title = "Seal CT Demo") {
        App()
    }
}
