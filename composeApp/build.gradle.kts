import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.serialization)
    alias(libs.plugins.mokkery)
    alias(libs.plugins.ksp)
    alias(libs.plugins.buildkonfig)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi")
        freeCompilerArgs.add("-Xopt-in=kotlin.uuid.ExperimentalUuidApi")
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
        freeCompilerArgs.add("-Xopt-in=kotlin.time.ExperimentalTime")
    }
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.android)
            implementation(libs.koin.android)
            implementation(libs.play.services.location)
            implementation(libs.androidx.browser)
        }
        commonMain.dependencies {
            implementation(project(":console-logger"))
            implementation(project(":logger"))
            implementation(project(":sentry-logger"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.compose.foundation)
            implementation(libs.compose.runtime)
            implementation(libs.compose.map)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.kotlinx.datetime)
            implementation(libs.uuid)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            api(libs.koin.annotations)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.datastore.preferences)
            implementation(libs.multiplatform.settings.datastore)
            implementation(libs.multiplatform.settings.coroutines)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.android)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.turbine)
        }
    }
}

android {
    namespace = "com.jordankurtz.piawaremobile"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.jordankurtz.piawaremobile"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
    add("kspCommonMainMetadata", libs.koin.compiler)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

kotlin.sourceSets.commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}

compose.desktop {
    application {
        mainClass = "com.jordankurtz.piawaremobile.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.jordankurtz.piawaremobile"
            packageVersion = "1.0.0"
        }
    }
}

buildkonfig {
    packageName = "com.jordankurtz.piawaremobile"
    objectName = "BuildConfig"

    defaultConfigs {
        buildConfigField(STRING, "SENTRY_DSN", "")

    }

    targetConfigs {
        create("android") {
            buildConfigField(STRING, "SENTRY_DSN", providers.gradleProperty("sentry.dsn.android").getOrElse(""))
        }
        create("iosX64") {
            buildConfigField(STRING, "SENTRY_DSN", providers.gradleProperty("sentry.dsn.ios").getOrElse("fdsafs"))
        }
        create("iosArm64") {
            buildConfigField(STRING, "SENTRY_DSN", providers.gradleProperty("sentry.dsn.ios").getOrElse("fdsafs"))
        }
        create("iosSimulatorArm64") {
            buildConfigField(STRING, "SENTRY_DSN", providers.gradleProperty("sentry.dsn.ios").getOrElse("fdsafs"))
        }
        create("desktop") {
            buildConfigField(STRING, "SENTRY_DSN", providers.gradleProperty("sentry.dsn.desktop").getOrElse(""))
        }
    }
}
