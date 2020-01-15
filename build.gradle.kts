buildscript {
    repositories {
        google()
        jcenter()
        mavenCentral()
    }

    dependencies {
        classpath(Dependencies.GradlePlugins.android)
        classpath(Dependencies.GradlePlugins.kotlin)
        classpath(Dependencies.GradlePlugins.mavenPublish)
        classpath(Dependencies.GradlePlugins.androidMavenGradle)
        classpath(Dependencies.GradlePlugins.bintrayGradle)
        // Build Tool to generate Kotlin KDoc documentation
        classpath(Dependencies.GradlePlugins.dokka)
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        mavenCentral()
    }
}

tasks.create<Delete>("clean") {
    delete(rootProject.buildDir)
}