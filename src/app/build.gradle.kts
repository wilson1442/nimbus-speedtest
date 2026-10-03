import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Build-time injected configuration (APK Build Standard §5 contract).
// A public LibreSpeed-compatible server is baked in as the default speed-test
// endpoint (verified 2026-10-03: empty.php ping/download/upload all 200,
// garbage.php streams at full line rate). -PAPI_BASE_URL=<url> overrides it
// at build time; the user can still change the server at runtime in Settings
// (DataStore), which always wins over the baked default.
val apiBaseUrl: String = (project.findProperty("API_BASE_URL") as String?)
    ?: "https://nyc.speedtest.clouvider.net/backend"

android {
    namespace = "cloud.g3h.nimbus"
    compileSdk = 35

    defaultConfig {
        applicationId = "cloud.g3h.nimbus"
        minSdk = 24
        targetSdk = 35
        versionCode = 5
        versionName = "1.1.1"
        // BuildConfig.API_BASE_URL: default speed-test server baked at build
        // time (NYC Clouvider LibreSpeed backend; user can override in Settings).
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"${apiBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        )
        // BuildConfig.IP_LOOKUP_URL: default WAN-IP lookup endpoint.
        buildConfigField(
            "String",
            "IP_LOOKUP_URL",
            "\"https://api.ipify.org\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signing is applied by the APK builder via its keystore reference;
            // a local assembleRelease is a diagnostic artifact only (Standard §9.3).
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += setOf(
            "META-INF/AL2.0",
            "META-INF/LGPL2.1",
            "META-INF/LICENSE",
            "META-INF/LICENSE.txt",
            "META-INF/NOTICE",
            "META-INF/NOTICE.txt"
        )
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    // Real org.json for JVM unit tests (Android's bundled copy is a stub
    // that throws at runtime under the plain JUnit runner).
    testImplementation("org.json:json:20240303")
}
