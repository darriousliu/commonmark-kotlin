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

    macosX64()
    macosArm64()

    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()
    watchosX64()

    tvosArm64()
    tvosSimulatorArm64()
    tvosX64()

    mingwX64()

    linuxX64()
    linuxArm64()

    js(IR) {
        nodejs()
        browser()
        binaries.executable()
    }

    @Suppress("OPT_IN_USAGE")
    wasmJs {
        nodejs()
        binaries.executable()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val webMain by creating {
            dependsOn(commonMain.get())
        }
        jsMain.get().dependsOn(webMain)
        wasmJsMain.get().dependsOn(webMain)
    }
}