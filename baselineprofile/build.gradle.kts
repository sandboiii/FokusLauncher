plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.lu4p.fokuslauncher.baselineprofile"
    compileSdk { version = release(36) }

    defaultConfig {
        // Baseline profiles require API 28+; rootless collection requires API 33+.
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    targetProjectPath = ":app"
}

baselineProfile {
    // Collect on whatever device/emulator is attached; a Gradle-managed device can be
    // added later if CI generation is ever wanted (profiles are committed, so it isn't).
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
