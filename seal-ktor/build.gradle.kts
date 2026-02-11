import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.dokka)
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

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    iosArm64()
    iosSimulatorArm64()

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":seal-core"))
            implementation(libs.ktor.client.core)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        val wasmJsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }
    }
}

dokka {
    moduleName.set("Seal - Ktor")
    moduleVersion.set("0.2.0")

    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
        suppressObviousFunctions.set(true)
        suppressInheritedMembers.set(false)
    }

    dokkaSourceSets.configureEach {
        sourceLink {
            localDirectory.set(projectDir.resolve("src"))
            remoteUrl("https://github.com/jermeyyy/seal/tree/main/${project.name}/src")
            remoteLineSuffix.set("#L")
        }
    }
}
