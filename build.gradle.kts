import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.20"
}

repositories {
    jcenter()
}

dependencies {
    implementation(kotlin("reflect"))

    api("com.google.guava:guava:30.0-jre")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
    kotlinOptions.freeCompilerArgs = listOf("-progressive", "-Xjsr305=strict", "-Xjvm-default=all", "-Xstring-concat=indy")
}

tasks.test {
    useJUnitPlatform()
}
