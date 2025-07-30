plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.maven.gradlePlugin)
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
}

