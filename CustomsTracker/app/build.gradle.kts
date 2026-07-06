import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// Release signing credentials are kept out of source control.
// See keystore.properties.example for the expected format.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.coroutines.core)
        }

        androidMain.dependencies {
            // Core AndroidX
            implementation(libs.core.ktx)
            implementation(libs.lifecycle.runtime.ktx)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.activity.compose)
            implementation(libs.coroutines.android)
            implementation(libs.datastore)

            // Compose UI (Android-specific interop/tooling)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.material.icons.ext)
            implementation(libs.compose.ui.text.google.fonts)
            implementation(libs.navigation.compose)

            // Room
            implementation(libs.room.runtime)
            implementation(libs.room.ktx)

            // Hilt
            implementation(libs.hilt.android)
            implementation(libs.hilt.navigation.compose)
            implementation(libs.hilt.work)

            // Network (ready for Phase 9)
            implementation(libs.retrofit)
            implementation(libs.retrofit.kotlinx.serialization)
            implementation(libs.okhttp)
            implementation(libs.okhttp.logging)
            implementation(libs.kotlinx.serialization.json)

            // Auth / Security
            implementation(libs.security.crypto)
            implementation(libs.lifecycle.runtime.compose)

            // WorkManager (SLA alerts Phase 6 + sync Phase 9)
            implementation(libs.workmanager)

            // CameraX (Phase 5)
            implementation(libs.camerax.core)
            implementation(libs.camerax.camera2)
            implementation(libs.camerax.lifecycle)
            implementation(libs.camerax.view)
        }

        androidUnitTest.dependencies {
            implementation(libs.junit)
            implementation(libs.coroutines.test)
        }

        getByName("androidInstrumentedTest").dependencies {
            implementation(libs.androidx.test.ext)
            implementation(libs.room.testing)
            implementation(libs.coroutines.test)
            implementation(libs.androidx.test.runner)
            implementation(libs.hilt.android.testing)
        }

        iosMain.dependencies {
        }
    }
}

android {
    namespace   = "com.rms.customs"
    compileSdk  = 36

    defaultConfig {
        applicationId   = "com.rms.customs"
        minSdk          = 26
        targetSdk       = 36
        versionCode     = 1
        versionName     = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile     = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias      = keystoreProperties.getProperty("keyAlias")
                keyPassword   = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    add("androidMainImplementation", platform(libs.compose.bom))

    // Debug
    add("debugImplementation", libs.compose.ui.tooling)

    // KSP (Android-only annotation processing)
    add("kspAndroid", libs.room.compiler)
    add("kspAndroid", libs.hilt.compiler)
    add("kspAndroid", libs.hilt.work.compiler)
    add("kspAndroidTest", libs.hilt.compiler)
}
