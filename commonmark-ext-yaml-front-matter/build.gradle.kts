plugins {
    id("multiplatform")
    id("publish")

}

kotlin {
    androidLibrary {
        namespace = "org.commonmark.ext.yaml.front.matter"
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
    coordinates(group.toString(), "commonmark-ext-yaml-front-matter", version.toString())
}