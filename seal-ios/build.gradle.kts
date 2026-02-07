plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("seal.publishing")
}

kotlin {
    explicitApi()

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":seal-core"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

group = "io.github.jermey.seal"
version = "0.1.0"
