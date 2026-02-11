import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.dokka)
    id("seal.publishing")
}

kotlin {
    explicitApi()

    androidLibrary {
        namespace = "com.jermey.seal.core"
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

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.io.core)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        // jvmSharedMain: shared between androidMain and jvmMain (Conscrypt, OkHttp, TrustManager)
        val jvmSharedMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.okhttp)
            }
        }
        val jvmSharedTest by creating {
            dependsOn(commonTest.get())
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlin.testJunit)
                implementation(libs.okhttp.mockwebserver)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        androidMain.get().dependsOn(jvmSharedMain)
        androidMain.dependencies {
            implementation(libs.conscrypt.android)
        }

        jvmMain.get().dependsOn(jvmSharedMain)
        jvmMain.dependencies {
            implementation(libs.conscrypt.openjdk)
        }

        val jvmTest by getting {
            dependsOn(jvmSharedTest)
        }
    }
}

dokka {
    moduleName.set("Seal - Core")
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
