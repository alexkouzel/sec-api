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
