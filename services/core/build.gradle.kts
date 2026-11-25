plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":libs:contract"))

    implementation(platform(libs.spring.boot.bom))

    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.actuator)

    implementation(libs.spring.boot.starter.jdbc)
    runtimeOnly(libs.postgresql)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testRuntimeOnly(libs.junit.platform.launcher)

    implementation(libs.spring.boot.starter.flyway)
    runtimeOnly(libs.flyway.database.postgresql)

    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)

    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.jdbc.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.bootRun {
    workingDir = rootProject.projectDir
    args("--spring.profiles.active=local")
}