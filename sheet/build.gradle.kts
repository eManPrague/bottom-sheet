plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlin-android-extensions")
    id("maven-publish")
}

android {
    compileSdkVersion(Dependencies.Versions.compileSdk)
    buildToolsVersion(Dependencies.Versions.buildTools)

    defaultConfig {
        minSdkVersion(Dependencies.Versions.minSdk)
        targetSdkVersion(Dependencies.Versions.targetSdk)
        versionCode = 1
        versionName = "1.0"
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
    implementation(Dependencies.SupportDependencies.appCompat)
    implementation(Dependencies.SupportDependencies.design)
    implementation(Dependencies.KotlinDependencies.kotlin)
}