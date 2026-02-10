package com.jermey.seal

import android.app.Application
import com.jermey.seal.android.ConscryptInitializer
import com.jermey.seal.demo.di.appModule
import com.jermey.seal.demo.di.containerModule
import org.koin.core.context.startKoin

class SealApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ConscryptInitializer.initialize()
        startKoin {
            modules(appModule, containerModule)
        }
    }
}
