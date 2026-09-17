import org.gradle.declarative.dsl.schema.FqName.Empty.packageName
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidMultiplatformLibrary)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.kotlinSerialization)
  alias(libs.plugins.sqldelight)
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

  android {
    namespace = "pl.llp.aircasting.shared"
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
    iosMain.dependencies {
      implementation(libs.ktor.client.darwin)
      implementation(libs.sqldelight.native.driver)
    }
    androidMain.dependencies {
      implementation(libs.compose.uiToolingPreview)
      implementation(libs.androidx.core.ktx)
      implementation(libs.androidx.activity.compose)
      implementation(libs.ktor.client.okhttp)
      implementation(libs.play.services.location)
      implementation(libs.sqldelight.android.driver)
    }
    commonMain.dependencies {
      implementation(libs.ktor.client.core)
      implementation(libs.ktor.client.content.negotiation)
      implementation(libs.ktor.serialization.kotlinx.json)
      implementation(libs.kotlinx.serialization.json)
      implementation(libs.kotlinx.datetime)
      implementation(libs.compose.runtime)
      implementation(libs.compose.foundation)
      implementation(libs.compose.material3)
      implementation(libs.compose.ui)
      // Common `BackHandler`: Android's system back on one side, iOS' back-swipe gesture on the
      // other. Ships as its own artifact, so `compose.ui` alone does not bring it in.
      implementation(libs.compose.ui.backhandler)
      implementation(libs.compose.components.resources)
      implementation(libs.compose.uiToolingPreview)
      implementation(libs.androidx.lifecycle.viewmodelCompose)
      implementation(libs.androidx.lifecycle.runtimeCompose)
      implementation(libs.androidx.navigation.compose)
      implementation(libs.kable.core)
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.koin.core)
      implementation(libs.koin.compose)
      implementation(libs.koin.compose.viewmodel)
      implementation(libs.kermit)
      implementation(libs.lyricist)
      implementation(libs.multiplatform.settings)
      implementation(libs.sqldelight.coroutines.extensions)
      implementation(libs.sqldelight.primitive.adapters)
    }
    commonTest.dependencies {
      implementation(libs.ktor.client.mock)
      implementation(libs.kotlin.test)
      implementation(libs.kotlinx.coroutines.test)
      implementation(libs.turbine)
      implementation(libs.multiplatform.settings.test)
    }
  }
}

dependencies {
  androidRuntimeClasspath(libs.compose.uiTooling)
}

sqldelight {
  databases {
    create("AircastingDatabase") {
      packageName.set("pl.llp.aircasting.data.local.db")
    }
  }
}