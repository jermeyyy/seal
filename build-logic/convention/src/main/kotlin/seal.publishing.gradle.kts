plugins {
    `maven-publish`
    signing
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set(project.name)
            description.set("Seal - Certificate Transparency verification library for Kotlin Multiplatform")
            url.set("https://github.com/jermeyyy/seal")

            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }

            developers {
                developer {
                    id.set("jermey")
                    name.set("Jermey")
                    url.set("https://github.com/jermeyyy")
                }
            }

            scm {
                url.set("https://github.com/jermeyyy/seal")
                connection.set("scm:git:git://github.com/jermeyyy/seal.git")
                developerConnection.set("scm:git:ssh://git@github.com/jermeyyy/seal.git")
            }
        }
    }

    repositories {
        maven {
            name = "sonatype"
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

            credentials {
                username = providers.environmentVariable("OSSRH_USERNAME").orNull
                password = providers.environmentVariable("OSSRH_PASSWORD").orNull
            }
        }
    }
}

signing {
    val signingKeyId = providers.environmentVariable("GPG_KEY_ID").orNull
    val signingKey = providers.environmentVariable("GPG_PRIVATE_KEY").orNull
    val signingPassword = providers.environmentVariable("GPG_PASSPHRASE").orNull

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        sign(publishing.publications)
    }
}
