import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    id("com.github.ben-manes.versions") version "0.43.0" // find dependency updates
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.google.guava:guava:31.1-jre")
    api("org.apache.commons:commons-csv:1.9.0")

    implementation("com.itextpdf:kernel:7.2.4")
    implementation("com.itextpdf:io:7.2.4")
    implementation("com.itextpdf:layout:7.2.4")
    implementation("com.github.luben:zstd-jni:1.5.2-5")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        languageVersion = "1.7"
        apiVersion = "1.7"
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
