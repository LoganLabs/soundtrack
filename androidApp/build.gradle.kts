plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.navidrome.app"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        applicationId = "com.navidrome.app"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":shared"))
}
