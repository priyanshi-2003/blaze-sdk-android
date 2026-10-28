import java.util.Properties

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.jetbrains.kotlin.android)
  id("maven-publish")
}

val localProperties = Properties().apply {
  rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
  namespace = "in.breeze.blaze"
  compileSdk = 34

  defaultConfig {
    minSdk = 21

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildFeatures {
    buildConfig = true
  }

  buildTypes {
    debug {
      // Local Ostia for development: set blaze.devFrameUrl in local.properties
      val devFrameUrl = localProperties.getProperty("blaze.devFrameUrl")
      buildConfigField("String", "DEV_FRAME_URL", devFrameUrl?.let { "\"$it\"" } ?: "null")
    }
    release {
      buildConfigField("String", "DEV_FRAME_URL", "null")
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  kotlinOptions {
    jvmTarget = "1.8"
    languageVersion = "1.8"
  }
  publishing {
    singleVariant("release")
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  testImplementation(libs.junit)
  testImplementation(libs.json)
}


afterEvaluate {
  publishing {
    publications {
      create<MavenPublication>("maven") {
        groupId = "in.breeze"
        artifactId = "blaze"
        version = project.version.toString().takeIf { it != "unspecified" } ?: "LOCAL-SNAPSHOT"
        from(components["release"])
      }
    }
    repositories {
      maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/juspay/blaze-sdk-android")
        credentials {
          username = System.getenv("GITHUB_ACTOR")
          password = System.getenv("GITHUB_TOKEN")
        }
      }
    }
  }
}