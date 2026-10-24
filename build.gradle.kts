plugins {
    kotlin("jvm") version "2.4.21"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    `maven-publish`
}

// JitPack builds releases from git tags and serves them under this group.
group = "com.github.alexkouzel"

kotlin {
    jvmToolchain(21)
    explicitApi()
}

// Sources let IDEs show the KDoc and code of the library.
java {
    withSourcesJar()
}

dependencies {
    implementation("tools.jackson.core:jackson-databind:3.2.4")
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
            useJUnitJupiter("6.1.3")
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
    version = "1.8.0"
    outputToConsole = true
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
