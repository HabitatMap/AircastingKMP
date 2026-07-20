import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidMultiplatformLibrary)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.kotlinSerialization)
}

kotlin {
  listOf(
    iosArm64(),
    iosSimulatorArm64()
  ).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "Shared"
      isStatic = true
    }
  }

  androidLibrary {
    namespace = "com.lunarlogic.aircasting.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    minSdk = libs.versions.android.minSdk.get().toInt()

    compilerOptions {
      jvmTarget = JvmTarget.JVM_11
    }
    androidResources {
      enable = true
    }
    withHostTest {
      isIncludeAndroidResources = true
    }
  }

  sourceSets {
    androidMain.dependencies {
      implementation(libs.compose.uiToolingPreview)
      implementation(libs.androidx.core.ktx)
    }
    commonMain.dependencies {
      implementation(libs.kotlinx.serialization.json)
      implementation(libs.kotlinx.datetime)
      implementation(libs.compose.runtime)
      implementation(libs.compose.foundation)
      implementation(libs.compose.material3)
      implementation(libs.compose.ui)
      implementation(libs.compose.components.resources)
      implementation(libs.compose.uiToolingPreview)
      implementation(libs.androidx.lifecycle.viewmodelCompose)
      implementation(libs.androidx.lifecycle.runtimeCompose)
      implementation(libs.kable.core)
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.koin.core)
      implementation(libs.koin.compose)
      implementation(libs.koin.compose.viewmodel)
      implementation(libs.kermit)
    }
    commonTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.kotlin.test)
      implementation(libs.kotlinx.coroutines.test)
      implementation(libs.turbine)
    }
  }
}

dependencies {
  androidRuntimeClasspath(libs.compose.uiTooling)
}