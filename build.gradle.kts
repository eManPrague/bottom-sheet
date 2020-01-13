buildscript {
    repositories {
        google()
        jcenter()
        mavenCentral()
    }

    dependencies {
        classpath(Dependencies.GradlePlugins.android)
        classpath(Dependencies.GradlePlugins.kotlin)
    }
}

allprojects {
    version = Dependencies.Versions.libVersion

    repositories {
        google()
        jcenter()
        mavenCentral()
    }
}

tasks{
    val clean by registering(Delete::class){
        delete(buildDir)
    }
}