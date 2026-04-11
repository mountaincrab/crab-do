plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Room KMP runtime
            implementation(libs.room.runtime)
            // kotlinx.serialization (RecurrenceRule JSON)
            implementation(libs.kotlinx.serialization.json)
            // Koin core (KMP)
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            // Core Android
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.activity.compose)
            // Compose (Android)
            implementation(project.dependencies.platform(libs.compose.bom))
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.graphics)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons)
            implementation(libs.navigation.compose)
            // SQLite driver for Room on Android
            implementation(libs.sqlite.bundled)
            // Koin Android + Compose
            implementation(libs.koin.android)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.workmanager)
            // WorkManager
            implementation(libs.workmanager.ktx)
            // Firebase
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.auth)
            implementation(libs.firebase.firestore)
            // Credential Manager for Google Sign-In
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)
            // DataStore
            implementation(libs.datastore.preferences)
            // Reorderable (drag-and-drop)
            implementation(libs.reorderable)
            // Glance (home screen widgets)
            implementation(libs.glance.appwidget)
        }
    }
}

android {
    namespace = "com.mountaincrab.crabdo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mountaincrab.crabdo"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        debug {
            buildConfigField("Boolean", "USE_EMULATOR", "false")
        }
        release {
            buildConfigField("Boolean", "USE_EMULATOR", "false")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// KSP Room compiler + Android-specific debug deps
dependencies {
    add("kspAndroid", libs.room.compiler)
    add("debugImplementation", libs.compose.ui.tooling)
}
