plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jlleitschuh.gradle.ktlint")
    id("dev.detekt")
}

val libs = versionCatalogs.named("libs")

kotlin {
    jvmToolchain(21)
}

ktlint {
    version = libs.findVersion("ktlint").get().requiredVersion
    outputToConsole = true
}

detekt {
    // Every source set, not only main and test.
    source.setFrom("src")
    buildUponDefaultConfig = true
    config.from(rootProject.file("config/detekt.yml"))
    file("detekt.yml").takeIf { it.exists() }?.let { config.from(it) }
}

dependencies {
    testImplementation(platform(libs.findLibrary("junit-bom").get()))
    testImplementation(libs.findLibrary("junit-jupiter").get())
    testImplementation(kotlin("test"))
    testRuntimeOnly(libs.findLibrary("junit-launcher").get())
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform {
        if (name != "knownBugTest") excludeTags("known-bug")
    }
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.register<Test>("knownBugTest") {
    description = "Runs tests that document known, unfixed bugs (docs/ROADMAP.md)."
    group = "verification"
    val test = tasks.test.get()
    testClassesDirs = test.testClassesDirs
    classpath = test.classpath
    useJUnitPlatform {
        includeTags("known-bug")
    }
    outputs.upToDateWhen { false }
}
