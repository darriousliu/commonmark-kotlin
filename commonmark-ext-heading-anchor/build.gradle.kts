plugins {
    id("multiplatform")
    id("publish")

}

kotlin {
    androidLibrary {
        namespace = "org.commonmark.ext.heading.anchor"
        enableCoreLibraryDesugaring = true
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":commonmark"))
                implementation(libs.kotlin.stdlib)
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

dependencies {
    coreLibraryDesugaring(libs.desugar)
}

mavenPublishing {
    coordinates(group.toString(), "commonmark-ext-heading-anchor", version.toString())
}