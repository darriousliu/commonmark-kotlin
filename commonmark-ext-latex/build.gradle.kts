plugins {
    id("multiplatform")
    id("publish")

}

kotlin {
    androidLibrary {
        namespace = "org.commonmark.ext.latex"
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
    coordinates(group.toString(), "commonmark-ext-latex", version.toString())
}