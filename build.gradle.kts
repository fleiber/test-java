import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
    id("com.github.ben-manes.versions") version "0.47.0" // find dependency updates
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.google.guava:guava:32.1.1-jre")
    api("org.apache.commons:commons-csv:1.10.0")

    implementation("ch.qos.logback:logback-classic:1.4.8")
    implementation("com.github.luben:zstd-jni:1.5.5-5")

    implementation("com.itextpdf:kernel:8.0.0")
    implementation("com.itextpdf:io:8.0.0")
    implementation("com.itextpdf:layout:8.0.0")
    implementation("com.itextpdf:bouncy-castle-adapter:8.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        languageVersion.set(KOTLIN_1_9)
        apiVersion.set(KOTLIN_1_9)
        progressiveMode.set(true)
        jvmTarget.set(JVM_17)
        freeCompilerArgs.addAll("-Xjvm-default=all", "-Xlambdas=indy", "-Xjsr305=strict", "-Xtype-enhancement-improvements-strict-mode", "-Xassertions=jvm")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    val unstableRegexps = arrayOf("alpha", "beta", "b", "m", "ea", "pr", "preview", "rc").map { Regex(".*[.-]$it[.\\d-+]*") }
    rejectVersionIf { val v = candidate.version.lowercase(); unstableRegexps.any { v.matches(it) } }
}
