import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.openrewrite.build.recipe-library") version "latest.release"
    kotlin("jvm") version "1.9.22"
}
group = "org.openrewrite"
description = "Rewrite Gradle Experimental"

val latest = if (project.hasProperty("releasing")) {
    "latest.release"
} else {
    "latest.integration"
}

val kotlinVersion = "1.9.22"

dependencies {
    implementation(platform("org.openrewrite:rewrite-bom:${latest}"))
    implementation("org.openrewrite:rewrite-gradle")
    implementation("org.openrewrite:rewrite-kotlin:${latest}")

    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-impl-embeddable:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-scripting-common:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-sam-with-receiver-compiler-plugin:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-assignment-compiler-plugin-embeddable:${kotlinVersion}")

    testImplementation("org.openrewrite:rewrite-test")
    testImplementation("org.junit.jupiter:junit-jupiter:latest.release")
    testRuntimeOnly("org.openrewrite:rewrite-java-17")
}

tasks {
    named<KotlinCompile>("compileKotlin") {
        kotlinOptions.jvmTarget = "1.8"
    }
    named<KotlinCompile>("compileTestKotlin") {
        kotlinOptions.jvmTarget = "17"
    }
}
