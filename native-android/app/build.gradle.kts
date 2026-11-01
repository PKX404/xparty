plugins {
    id("com.android.application")
}
android {
    namespace = "com.pkx404.xparty.nativepreview"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.pkx404.xparty.nativepreview"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "3.0.1-v24-native-preview"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation("androidx.media3:media3-exoplayer:1.6.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.6.1")
    implementation("androidx.media3:media3-ui:1.6.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
