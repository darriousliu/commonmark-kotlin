plugins {
    id("multiplatform")
    id("publish")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidLibrary {
        namespace = "org.commonmark.ext.gfm.strikethrough"
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

publishing {
    publications {
        getByName<MavenPublication>("kotlinMultiplatform") {
            artifactId = "commonmark-ext-gfm-strikethrough"
        }
    }
}