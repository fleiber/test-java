import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.31"
    id("com.github.ben-manes.versions") version "0.39.0" // find dependency updates
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.google.guava:guava:31.0.1-jre")
    api("org.apache.commons:commons-csv:1.9.0")

    implementation("com.github.luben:zstd-jni:1.5.0-4")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        languageVersion = "1.5"
        apiVersion = "1.5"
        jvmTarget = "16"
        freeCompilerArgs = listOf("-progressive", "-Xjsr305=strict", "-Xjvm-default=all", "-Xstring-concat=indy", "-Xlambdas=indy", "-Xself-upper-bound-inference", "-Xunrestricted-builder-inference", "-Xtype-enhancement-improvements-strict-mode")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    val unstableRegexps = arrayOf("alpha", "beta", "b", "m", "ea", "pr", "preview", "rc").map { Regex(".*[.-]$it[.\\d-+]*") }
    rejectVersionIf { val v = candidate.version.toLowerCase(); unstableRegexps.any { v.matches(it) } }
}
