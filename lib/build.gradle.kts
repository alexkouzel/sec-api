plugins {
    id("sec.kotlin")
    `java-test-fixtures`
}

kotlin {
    explicitApi()
}

dependencies {
    api(libs.jackson.databind)
    implementation(libs.jackson.xml)
    implementation(libs.jackson.kotlin)
}

testing {
    suites {
        register<JvmTestSuite>("contractTest")
        register<JvmTestSuite>("liveTest") {
            targets.all {
                testTask.configure {
                    description = "Runs smoke tests against live EDGAR."
                    outputs.upToDateWhen { false }
                }
            }
        }
        withType<JvmTestSuite>().configureEach {
            useJUnitJupiter(libs.versions.junit)
            dependencies {
                implementation(testFixtures(project()))
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
        }
    }
}

// Test code may use internal declarations of main, and tests those of testFixtures.
kotlin.target.compilations.run {
    val main = getByName("main")
    val testFixtures = getByName("testFixtures")
    listOf("testFixtures", "contractTest", "liveTest").forEach { getByName(it).associateWith(main) }
    listOf("test", "contractTest", "liveTest").forEach { getByName(it).associateWith(testFixtures) }
}

tasks.check {
    dependsOn(testing.suites.named("contractTest"))
}
