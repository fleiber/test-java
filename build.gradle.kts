import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.21"
    id("com.github.ben-manes.versions") version "0.46.0" // find dependency updates
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.google.guava:guava:31.1-jre")
    api("org.apache.commons:commons-csv:1.10.0")

    implementation("com.itextpdf:kernel:7.2.5")
    implementation("com.itextpdf:io:7.2.5")
    implementation("com.itextpdf:layout:7.2.5")
    implementation("com.github.luben:zstd-jni:1.5.5-2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        languageVersion = "1.8"
        apiVersion = "1.8"
        jvmTarget = "17"
        freeCompilerArgs = listOf("-progressive", "-Xjvm-default=all", "-Xlambdas=indy", "-Xenable-builder-inference", "-Xjsr305=strict", "-Xtype-enhancement-improvements-strict-mode")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    val unstableRegexps = arrayOf("alpha", "beta", "b", "m", "ea", "pr", "preview", "rc").map { Regex(".*[.-]$it[.\\d-+]*") }
    rejectVersionIf { val v = candidate.version.toLowerCase(); unstableRegexps.any { v.matches(it) } }
}
