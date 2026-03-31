plugins {
    //trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.buildkonfig) apply false
    alias(libs.plugins.sentry) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.sqldelight) apply false
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        android.set(true)
        filter {
            exclude { element -> element.file.path.contains("/build/") }
            exclude { element -> element.file.name.endsWith(".gradle.kts") }
        }
    }

    afterEvaluate {
        tasks.matching { it.name.startsWith("runKtlint") || it.name.startsWith("ktlint") }.configureEach {
            tasks.findByName("kspCommonMainKotlinMetadata")?.let { kspTask ->
                dependsOn(kspTask)
            }
        }
    }

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(rootProject.files("detekt.yml"))
        buildUponDefaultConfig = true
        source.setFrom(
            files(
                "src/commonMain/kotlin",
                "src/androidMain/kotlin",
                "src/iosMain/kotlin",
                "src/desktopMain/kotlin",
            ),
        )
    }
}
