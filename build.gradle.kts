import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.20"
    id("com.github.ben-manes.versions") version "0.52.0" // find dependency updates
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.google.guava:guava:33.4.8-jre")
    api("org.apache.commons:commons-csv:1.14.0")

    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("com.github.luben:zstd-jni:1.5.7-2")

    implementation("com.itextpdf:kernel:9.1.0")
    implementation("com.itextpdf:io:9.1.0")
    implementation("com.itextpdf:layout:9.1.0")
    implementation("com.itextpdf:bouncy-castle-adapter:9.1.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.12.2")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        languageVersion = KOTLIN_2_1
        apiVersion = KOTLIN_2_1
        progressiveMode = true
        jvmTarget = JVM_21
        extraWarnings = true
        freeCompilerArgs.addAll("-Xjvm-default=all", "-Xjsr305=strict", "-Xtype-enhancement-improvements-strict-mode", "-Xassertions=jvm")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    val unstableRegexps = arrayOf("alpha", "beta", "b", "m", "ea", "pr", "preview", "rc").let { toReject -> Array(toReject.size) { Regex(""".*[.-]${toReject[it]}[.\d-+]*""") } }
    rejectVersionIf { val v = candidate.version.lowercase(); unstableRegexps.any { v.matches(it) } }
}
