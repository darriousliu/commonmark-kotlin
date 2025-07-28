plugins {
    `kotlin-multiplatform`
    com.android.kotlin.multiplatform.library
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidLibrary {
        compileSdk = 36
        minSdk = 23
    }

    jvm()

    iosX64()
    iosArm64()
    iosSimulatorArm64()
}