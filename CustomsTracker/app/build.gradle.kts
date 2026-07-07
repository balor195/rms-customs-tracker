import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
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

    // iosX64 (Intel simulator) deliberately omitted — androidx.sqlite:sqlite-bundled no longer
    // publishes for it, and every dev/CI machine here is Apple Silicon (iosSimulatorArm64) anyway.
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
        // CommonCrypto has no Apple-published module map (unlike Foundation/Security), so PBKDF2
        // (PasswordHasher.ios.kt) needs a hand-written cinterop def to bind CCKeyDerivationPBKDF.
        iosTarget.compilations.getByName("main") {
            cinterops {
                create("commonCrypto") {
                    defFile(project.file("src/nativeInterop/cinterop/commonCrypto.def"))
                }
            }
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
            implementation(libs.kotlinx.datetime)

            // Room — room-ktx is deliberately not included: it's Android-only (pulls in
            // kotlinx-coroutines-android transitively, which has no iOS variant) and unused —
            // Room's own Flow support in room-runtime already covers every DAO in this project.
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)

            // Persistence
            implementation(libs.datastore)

            // Network
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        androidMain.dependencies {
            // Core AndroidX
            implementation(libs.core.ktx)
            implementation(libs.lifecycle.runtime.ktx)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.activity.compose)
            implementation(libs.coroutines.android)

            // Compose UI (Android-specific interop/tooling)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.compose.material.icons.ext)
            implementation(libs.compose.ui.text.google.fonts)
            implementation(libs.navigation.compose)

            // Koin (BOM applied via androidMainImplementation below)
            implementation(libs.koin.core)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
            implementation(libs.koin.androidx.workmanager)

            // Network (Ktor engine)
            implementation(libs.ktor.client.okhttp)

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
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
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
    add("androidMainImplementation", platform(libs.koin.bom))

    // Debug
    add("debugImplementation", libs.compose.ui.tooling)

    // KSP — Room's @Database/@Dao/@Entity processing runs per-target since Kotlin/Native
    // doesn't share one compiled artifact across iosArm64/iosSimulatorArm64 like JVM does.
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}
