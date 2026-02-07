import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    id("seal.publishing")
}

kotlin {
    explicitApi()

    androidLibrary {
        namespace = "com.jermey.seal.ktor"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":seal-core"))
            implementation(libs.ktor.client.core)
        }
        androidMain.dependencies {
            implementation(project(":seal-android"))
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(project(":seal-ios"))
            implementation(libs.ktor.client.darwin)
        }
    }
}

group = "io.github.jermey.seal"
version = "0.1.0"
