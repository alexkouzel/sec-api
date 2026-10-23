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

// Test code may use internal declarations of main.
kotlin.target.compilations.run {
    val main = getByName("main")
    listOf("testFixtures", "contractTest", "liveTest").forEach { getByName(it).associateWith(main) }
}

tasks.check {
    dependsOn(testing.suites.named("contractTest"))
}
