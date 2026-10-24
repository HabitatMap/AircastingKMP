import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
}

kotlin {
  compilerOptions {
    jvmTarget = JvmTarget.JVM_17
  }
}
dependencies {
  implementation(projects.shared)
  implementation(libs.koin.android)
  implementation(libs.androidx.activity.compose)
  implementation(libs.compose.uiToolingPreview)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.lifecycle.process)

  debugImplementation(libs.compose.uiTooling)
}

android {
  namespace = "pl.llp.aircasting"
  compileSdk = libs.versions.android.compileSdk.get()
    .toInt()

  defaultConfig {
    applicationId = "pl.llp.aircasting"
    minSdk = libs.versions.android.minSdk.get()
      .toInt()
    targetSdk = libs.versions.android.targetSdk.get()
      .toInt()
    versionCode = 1
    versionName = "1.0"
    val localProperties = Properties().apply {
      rootProject.file("local.properties")
        .takeIf { it.exists() }
        ?.inputStream()
        ?.use { load(it) }
    }
    manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY", "")
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}