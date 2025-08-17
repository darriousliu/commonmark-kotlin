plugins {
    id("multiplatform")
    id("publish")
}

kotlin {
    androidLibrary {
        namespace = "org.commonmark"

        enableCoreLibraryDesugaring = true
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                // Add KMP dependencies here

                implementation(libs.okio)
            }
        }

        androidMain {
            dependencies {

            }
        }

        iosMain {
            dependencies {

            }
        }
    }
}

mavenPublishing {
    coordinates(group.toString(), "commonmark", version.toString())
}

dependencies {
    coreLibraryDesugaring(libs.desugar)
}
