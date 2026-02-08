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
        namespace = "com.jermey.seal.android"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }

        withHostTest {}
    }

    sourceSets {
        androidMain.dependencies {
            api(project(":seal-core"))
            implementation(libs.conscrypt.android)
            implementation(libs.okhttp)
            implementation(libs.kotlinx.coroutines.android)
        }

        val androidHostTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlin.testJunit)
                implementation(libs.okhttp.mockwebserver)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

group = "io.github.jermeyyy.seal"
version = "0.1.0"

dokka {
    moduleName.set("Seal - Android")
    moduleVersion.set("0.1.0")

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
