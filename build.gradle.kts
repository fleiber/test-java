import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.31"
    id("com.github.ben-manes.versions") version "0.36.0" // find dependency updates
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.google.guava:guava:30.1-jre")
    api("org.apache.commons:commons-csv:1.8")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        useIR = true
        jvmTarget = "11"
        freeCompilerArgs = listOf("-progressive", "-Xjsr305=strict", "-Xjvm-default=all", "-Xstring-concat=indy")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    val unstableRegexps = arrayOf("alpha", "beta", "b", "m", "ea", "pr", "preview", "rc").map { Regex(".*[.-]$it[.\\d-+]*") }
    rejectVersionIf { val v = candidate.version.toLowerCase(); unstableRegexps.any { v.matches(it) } }
}
