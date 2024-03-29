import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.23"
    id("com.github.ben-manes.versions") version "0.51.0" // find dependency updates
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.google.guava:guava:33.1.0-jre")
    api("org.apache.commons:commons-csv:1.10.0")

    implementation("ch.qos.logback:logback-classic:1.5.3")
    implementation("com.github.luben:zstd-jni:1.5.5-11")

    implementation("com.itextpdf:kernel:8.0.3")
    implementation("com.itextpdf:io:8.0.3")
    implementation("com.itextpdf:layout:8.0.3")
    implementation("com.itextpdf:bouncy-castle-adapter:8.0.3")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        languageVersion = KOTLIN_1_9
        apiVersion = KOTLIN_1_9
        progressiveMode = true
        jvmTarget = JVM_21
        freeCompilerArgs.addAll("-Xjvm-default=all", "-Xlambdas=indy", "-Xjsr305=strict", "-Xtype-enhancement-improvements-strict-mode", "-Xassertions=jvm")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    val unstableRegexps = arrayOf("alpha", "beta", "b", "m", "ea", "pr", "preview", "rc").let { toReject -> Array(toReject.size) { Regex(""".*[.-]${toReject[it]}[.\d-+]*""") } }
    rejectVersionIf { val v = candidate.version.lowercase(); unstableRegexps.any { v.matches(it) } }
}
