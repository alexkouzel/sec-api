plugins {
    id("sec.kotlin")
    application
}

dependencies {
    implementation(project(":lib"))
}

application {
    mainClass = "com.alexkouzel.sec.drift.MainKt"
}

tasks.register<JavaExec>("driftCheck") {
    description = "Checks live EDGAR and the SEC spec page against the library; writes build/drift/report.md."
    group = "verification"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = application.mainClass
    val dir = layout.buildDirectory.dir("drift")
    args(dir.get().asFile.path, rootProject.file("specs").path)
    doFirst { delete(dir) }
    outputs.upToDateWhen { false }
}
