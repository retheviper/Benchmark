plugins {
    base
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.plugin.spring) apply false
    alias(libs.plugins.kotlin.plugin.serialization) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.ktor) apply false
}

allprojects {
    group = "io.youngbinkim.benchmark"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
