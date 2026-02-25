plugins {
    // Use plugin IDs without explicit versions here because the Android Gradle Plugin
    // is already on the classpath (configured at the root level).
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ai.assistance.showerclient"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        targetSdk = 35
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        // Shower client does not use Compose directly; only Binder/IPC and coroutines.
        compose = false
        aidl = true
        buildConfig = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
}
