plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.dokka)
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

dokka {
    moduleName.set("Seal - iOS")
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
