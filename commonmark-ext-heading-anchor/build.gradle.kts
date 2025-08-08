plugins {
    id("multiplatform")
    id("publish")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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

                implementation(compose.runtime)
                implementation(compose.components.resources)
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