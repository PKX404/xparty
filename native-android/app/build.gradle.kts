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
        versionCode = 1
        versionName = "3.0.0-native-preview"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
