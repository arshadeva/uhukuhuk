plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "id.oscarmandala.otacix"
    compileSdk = 35

    defaultConfig {
        applicationId = "id.oscarmandala.otacix"
        minSdk = 24            // Android 7.0 -> HP lama / spek rendah tetap bisa
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            // Ubah ke true + isShrinkResources = true bila ingin APK lebih kecil
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true   // dibutuhkan NewPipeExtractor di Android < 13
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.media3.common.util.UnstableApi",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
        )
    }
    buildFeatures { compose = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    // Pemutar + layanan latar belakang (sama seperti Metrolist: Media3 ExoPlayer + MediaSession)
    val media3 = "1.4.1"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-session:$media3")
    implementation("androidx.media3:media3-datasource:$media3")
    implementation("androidx.media3:media3-database:$media3")
    implementation("com.google.guava:guava:33.3.1-android")

    // Pengambil URL audio YouTube yang dirawat aktif (dipakai NewPipe; sudah punya solusi SABR/signature)
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.5")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.0.4")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.palette:palette-ktx:1.0.0")
}
