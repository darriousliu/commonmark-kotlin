@file:Suppress("UnstableApiUsage")

rootProject.name = "commonmark-kotlin"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-convention")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")

include(":commonmark")
include(":commonmark-ext-autolink")
include(":commonmark-ext-footnotes")
include(":commonmark-ext-gfm-strikethrough")
include(":commonmark-ext-gfm-tables")
include(":commonmark-ext-heading-anchor")
include(":commonmark-ext-image-attributes")
include(":commonmark-ext-ins")
include(":commonmark-ext-task-list-items")
include(":commonmark-ext-latex")

