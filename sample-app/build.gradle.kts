plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("sidekit.version")
}

val sidekitVersionName: String by extra

android {
    namespace = "hrln.interactive.sidekit.sample"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "hrln.interactive.sidekit.sample"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = sidekitVersionName
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // The smoke module depends on the SDK sources today. Once publishing is wired to a real
    // artifact host, switch this to hrln.interactive.sidekit:sidekit-sdk:<version> so the build
    // also catches missing POM dependencies.
    implementation(project(":sidekit-sdk"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.activity.compose)
}
