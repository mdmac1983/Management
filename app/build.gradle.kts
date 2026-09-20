import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// App version - bumped by +0.1 with every build per project convention.
// versionName is the single source of truth; versionCode derives from it.
val appVersionName = "1.6"
val appVersionCode = (appVersionName.replace(".", "").padEnd(3, '0')).toInt()

// Optional release signing - falls back to debug signing if keystore.properties is absent
// (set up real signing later by adding app/keystore.properties, see SETUP_INSTRUCTIONS.md)
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
val hasReleaseSigning = keystorePropertiesFile.exists()
if (hasReleaseSigning) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "app.orionmd.management"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.orionmd.management"
        // Optimized for Android Go edition devices (Android 10 / API 29), which are 32-bit ARM -
        // armeabi-v7a-only keeps the APK from bundling unused 64-bit/x86 native libs (SQLCipher
        // ships native .so files per ABI), matching the Portfolio app's device target.
        minSdk = 29
        targetSdk = 29
        versionCode = appVersionCode
        versionName = appVersionName

        ndk {
            abiFilters += "armeabi-v7a"
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // minSdk 24 needs desugaring for java.time (used throughout for dates/calendars)
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        // This app is sideloaded, never published to Google Play - Play's targetSdk-recency
        // policy doesn't apply, but AGP's release lint pass treats it as a hard build-breaking
        // error by default. Downgrade it to a warning so `assembleRelease` still works.
        disable += "ExpiredTargetSdkVersion"
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Room (local encrypted database)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // SQLCipher - encrypts the Room/SQLite database at rest
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")
    implementation("androidx.sqlite:sqlite:2.4.0")

    // Security - Keystore-backed storage for the encrypted DB passphrase
    implementation("androidx.security:security-crypto:1.1.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
