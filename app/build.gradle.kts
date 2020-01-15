plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-android-extensions")
}

android {

    compileSdkVersion(Android.compileSdk)

    defaultConfig {
        applicationId = Android.applicationId

        minSdkVersion(Android.minSdk)
        targetSdkVersion(Android.targetSdk)

        versionCode = getGitCommits()
        versionName = Android.versionName

        testInstrumentationRunner = Android.testInstrumentRunner
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }

        getByName("release") {
            isMinifyEnabled = false
        }
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
        getByName("test").java.srcDirs("src/test/kotlin")
    }

    compileOptions {
        sourceCompatibility = Android.sourceCompatibilityJava
        targetCompatibility = Android.targetCompatibilityJava
    }

    lintOptions {
        setLintConfig(rootProject.file("lint.xml"))
    }
}

dependencies {
    implementation(project(":bottomsheet"))

    implementation(Dependencies.Kotlin.stdlibJdk)
    // Support Libraries
    implementation(Dependencies.Android.appCompat)
    implementation(Dependencies.Android.materialDesign)
}
