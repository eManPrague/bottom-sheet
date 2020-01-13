plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-android-extensions")
}

android {
    compileSdkVersion(Dependencies.Versions.compileSdk)
    buildToolsVersion(Dependencies.Versions.buildTools)

    defaultConfig {
        applicationId = "cz.eman.android.bottomsheet.sample"
        minSdkVersion(Dependencies.Versions.minSdk)
        targetSdkVersion(Dependencies.Versions.targetSdk)
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        buildTypes {
            getByName("release") {
                isMinifyEnabled = false
                proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            }
        }

    }
}

dependencies {
    implementation(project(":sheet"))

    // Support Libraries
    implementation(Dependencies.SupportDependencies.appCompat)
    implementation(Dependencies.SupportDependencies.design)
    implementation(Dependencies.KotlinDependencies.kotlin)
}
