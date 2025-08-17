plugins {
    id("multiplatform")
    id("publish")
}

kotlin {
    androidLibrary {
        namespace = "com.commonmark.ext.autolink"
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

mavenPublishing {
    coordinates(group.toString(), "commonmark-ext-autolink", version.toString())
}