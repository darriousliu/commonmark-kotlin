plugins {
    id("multiplatform")
    id("publish")

}

kotlin {
    androidLibrary {
        namespace = "org.commonmark.ext.gfm.tables"
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
    coordinates(group.toString(), "commonmark-ext-gfm-tables", version.toString())
}