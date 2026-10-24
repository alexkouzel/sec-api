plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    `maven-publish`
}

// JitPack builds releases from git tags and serves them under this group.
group = "com.github.alexkouzel"

kotlin {
    jvmToolchain(21)
    explicitApi()
}

dependencies {
    implementation(libs.jackson.databind)
}

testing {
    suites {
        register<JvmTestSuite>("liveTest") {
            targets.all {
                testTask.configure {
                    description = "Runs the library against live EDGAR."
                    outputs.upToDateWhen { false }
                }
            }
        }
        withType<JvmTestSuite>().configureEach {
            useJUnitJupiter(libs.versions.junit)
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test-junit5")
            }
        }
    }
}

// The live tests may use internal declarations of main, as the unit tests do.
kotlin.target.compilations.run {
    getByName("liveTest").associateWith(getByName("main"))
}

tasks.withType<Test>().configureEach {
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

ktlint {
    version = libs.versions.ktlint.asProvider()
    outputToConsole = true
}

detekt {
    // Every source set, not only main and test.
    source.setFrom("src")
    buildUponDefaultConfig = true
    config.from("config/detekt.yml")
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
