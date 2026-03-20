plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.plugin.serialization)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "benchmark.BenchmarkRunnerKt"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logback.classic)
    implementation(libs.postgresql)

    testImplementation(libs.junit.jupiter)
}
