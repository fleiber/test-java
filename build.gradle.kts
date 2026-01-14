import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_3

plugins {
    kotlin("jvm") version "2.3.0"
    id("com.github.ben-manes.versions") version "0.53.0" // find dependency updates
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.google.guava:guava:33.5.0-jre")
    api("org.apache.commons:commons-csv:1.14.1")

    implementation("ch.qos.logback:logback-classic:1.5.24")
    implementation("com.github.luben:zstd-jni:1.5.7-6")

    implementation("com.itextpdf:kernel:9.5.0")
    implementation("com.itextpdf:io:9.5.0")
    implementation("com.itextpdf:layout:9.5.0")
    implementation("com.itextpdf:bouncy-castle-adapter:9.5.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.2")
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        languageVersion = KOTLIN_2_3
        apiVersion = KOTLIN_2_3
        progressiveMode = true
        extraWarnings = true
        jvmDefault = JvmDefaultMode.NO_COMPATIBILITY
        freeCompilerArgs.addAll("-Xwhen-expressions=indy", "-Xjsr305=strict", "-Xtype-enhancement-improvements-strict-mode", "-Xassertions=jvm")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    val unstableRegexps = arrayOf("alpha", "beta", "b", "m", "ea", "pr", "preview", "rc").let { toReject -> Array(toReject.size) { Regex(""".*[.-]${toReject[it]}[.\d-+]*""") } }
    rejectVersionIf { val v = candidate.version.lowercase(); unstableRegexps.any { v.matches(it) } }
}
