import com.jfrog.bintray.gradle.BintrayExtension
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("com.android.library")
    kotlin("android")
    id("org.jetbrains.dokka")
    id("digital.wup.android-maven-publish")
    id("com.github.dcendents.android-maven")
    id("com.jfrog.bintray")
}

android {
    compileSdkVersion(Android.compileSdk)

    defaultConfig {
        minSdkVersion(Android.minSdk)
        targetSdkVersion(Android.targetSdk)

        versionCode = Android.versionCode
        versionName = "${project.version}"

        testInstrumentationRunner = Android.testInstrumentRunner
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
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
    implementation(Dependencies.Kotlin.stdlibJdk)
    implementation(Dependencies.Android.appCompat)
    implementation(Dependencies.Android.materialDesign)
}

val dokka by tasks.getting(DokkaTask::class) {
    moduleName = "bottomsheet"
    outputFormat = "html" // html, md, javadoc,
    outputDirectory = "$buildDir/dokka/html"
    sourceDirs = files("src/main/kotlin")
}

tasks {

    val androidSourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(android.sourceSets["main"].java.srcDirs)
    }

    val androidDokkaHtmlJar by creating(Jar::class) {
        archiveClassifier.set("kdoc-html")
        from("$buildDir/dokka/html")
        dependsOn(dokka)
    }

    artifacts {
        add("archives", androidSourcesJar)
        add("archives", androidDokkaHtmlJar)
    }
}

group = Android.groupId
version = "${project.version}"

val productionPublicName = "production"

bintray {
    user = findPropertyOrNull("bintray.user")
    key = findPropertyOrNull("bintray.apikey")
    publish = true
    setPublications(productionPublicName)
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "maven"
        name = "cz.eman.bottomsheet"
        userOrg = "emanprague"
        override = true
        websiteUrl = "https://www.emanprague.com/en/"
        githubRepo = "eManPrague/bottom-sheet"
        vcsUrl = "https://github.com/eManPrague/bottom-sheet"
        description = "Implementation of Google's Bottom Sheet which was modified to fully support two peek heights."
        setLabels(
            "kotlin",
            "android",
            "bottom-sheet",
            "bottomsheet",
            "widget"
        )
        setLicenses("MIT")
        desc = description
        publicDownloadNumbers = true
    })
}

publishing {
    publications {
        register(productionPublicName, MavenPublication::class) {
            from(components["android"])
            groupId = Android.groupId
            artifactId = Android.artifactId
            version = "${project.version}"
        }
    }

    repositories {
        maven(url = "http://dl.bintray.com/emanprague/maven")
    }
}