plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.aminsambung.videoeditor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aminsambung.videoeditor"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // --- Bawaan Android Studio ---
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // --- TAMBAHKAN LIBRARY VIDEO EDITOR DI SINI ---
    
    // 1. Library untuk Filter Real-time (Kontras, Eksposur)
    implementation("jp.co.cyberagent.android:gpuimage:2.1.0")

    // 2. Library untuk Export/Encode Video (FFmpeg)
    // Catatan: Jika library ini error saat sync, kita bisa ganti dengan Media3 Transformer dari Google nanti.
    implementation("com.arthenica:ffmpeg-kit-min-gpl:6.0-2")
    
    // 3. (Opsional) ExoPlayer untuk memutar video di layar
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
}
