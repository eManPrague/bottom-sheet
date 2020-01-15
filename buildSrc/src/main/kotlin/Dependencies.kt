import org.gradle.api.JavaVersion

object Versions {
    // build
    const val libVersion = "1.0.0"
    const val isSnapshot = false

    const val compileSdk = 28
    const val minSdk = 21
    const val targetSdk = 28

    // gradle
    const val gradleBuildTools = "3.5.2"
    const val gradle = "5.6.4"

    const val kotlin = "1.3.61"
    const val appcompat = "1.1.0"
    const val dokka = "0.9.17"
    const val material = "1.1.0-alpha05"

    const val espresso = "3.0.1"
    const val junit = "4.12"
    const val testRunner = "1.0.1"

    const val mavenPublish = "3.6.2"
    const val mavenGradleGithub = "1.5"
    const val bintrayGradle = "1.8.4"
}

object Android {
    const val minSdk = 21
    const val targetSdk = 29
    const val compileSdk = 29

    const val versionCode = 1
    const val versionName = "1"

    const val applicationId = "cz.eman.bottomsheet.sample"
    const val groupId = "cz.eman.bottomsheet"
    const val artifactId = "bottomsheet"

    const val testInstrumentRunner = "androidx.test.runner.AndroidJUnitRunner"
    val sourceCompatibilityJava = JavaVersion.VERSION_1_8
    val targetCompatibilityJava = JavaVersion.VERSION_1_8
}

object Dependencies {

    object GradlePlugins {
        const val encoding = "UTF-8"
        const val gradle = Versions.gradle
        const val android = "com.android.tools.build:gradle:${Versions.gradleBuildTools}"
        const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
        const val dokka = "org.jetbrains.dokka:dokka-gradle-plugin:${Versions.dokka}"
        const val mavenPublish = "digital.wup:android-maven-publish:${Versions.mavenPublish}"
        const val androidMavenGradle = "com.github.dcendents:android-maven-gradle-plugin:${Versions.mavenGradleGithub}"
        const val bintrayGradle = "com.jfrog.bintray.gradle:gradle-bintray-plugin:${Versions.bintrayGradle}"
    }

    object Android {
        const val appCompat = "androidx.appcompat:appcompat:${Versions.appcompat}"
        const val materialDesign = "com.google.android.material:material:${Versions.material}"
    }

    object Kotlin {
        const val stdlibJdk = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"
    }

    object Test {
        const val junit = "junit:junit:${Versions.junit}"
    }
}