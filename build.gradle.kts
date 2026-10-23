plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    `java-test-fixtures`
    `maven-publish`
}

group = "com.alexkouzel"

kotlin {
    jvmToolchain(21)
    explicitApi()
}

dependencies {
    api(libs.jackson.databind)
    implementation(libs.jackson.xml)
    implementation(libs.jackson.kotlin)
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
                implementation(testFixtures(project()))
                implementation("org.jetbrains.kotlin:kotlin-test-junit5")
            }
        }
    }
}

// Test code may use internal declarations of main and of testFixtures.
kotlin.target.compilations.run {
    val main = getByName("main")
    val testFixtures = getByName("testFixtures")
    val liveTest = getByName("liveTest")
    testFixtures.associateWith(main)
    liveTest.associateWith(main)
    getByName("test").associateWith(testFixtures)
    liveTest.associateWith(testFixtures)
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

// Published to GitHub Packages; test fixtures stay unpublished.
val java = components["java"] as AdhocComponentWithVariants
listOf("testFixturesApiElements", "testFixturesRuntimeElements").forEach {
    java.withVariantsFromConfiguration(configurations[it]) { skip() }
}

// Without -Pversion the release would be called "unspecified".
val releaseVersion = version.toString()
tasks.withType<PublishToMavenRepository>().configureEach {
    doFirst { check(releaseVersion != "unspecified") { "Set the version: -Pversion=<version>" } }
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            artifactId = "sec-api"
            from(java)
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/alexkouzel/sec-api")
            credentials {
                username = findProperty("gpr.username") as String? ?: System.getenv("GPR_USERNAME")
                password = findProperty("gpr.token") as String? ?: System.getenv("GPR_TOKEN")
            }
        }
    }
}
