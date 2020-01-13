object Dependencies {

    object Versions {
        // build
        const val buildTools = "28.0.3"
        const val libVersion = "1.0.0"
        const val isSnapshot = false

        const val compileSdk = 28
        const val minSdk = 21
        const val targetSdk = 28

        // gradle
        const val gradleBuildTools = "3.5.1"
        const val gradle = "5.6.2"

        const val kotlin = "1.3.50"
        const val androidx = "1.0.0"

        const val espresso = "3.0.1"
        const val junit = "4.12"
        const val testRunner = "1.0.1"
    }

    // constants
    const val testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    object GradlePlugins {
        const val android = "com.android.tools.build:gradle:${Versions.gradleBuildTools}"
        const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    }

    object SupportDependencies {
        const val appCompat = "androidx.appcompat:appcompat:${Versions.androidx}"
        const val design = "com.google.android.material:material:${Versions.androidx}"
    }

    object KotlinDependencies {
        const val kotlin = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"
    }

    object TestDependencies {
        const val junit = "junit:junit:${Versions.junit}"
    }
}