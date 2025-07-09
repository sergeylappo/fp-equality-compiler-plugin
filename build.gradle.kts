plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("kapt") version "2.1.21"
    `maven-publish`
}

allprojects {
    group = "io.github.sergeylappo"
    version = "0.0.2"
    
    repositories {
        mavenCentral()
        google()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    
    kotlin {
        jvmToolchain(17)
    }
    
    dependencies {
        implementation(kotlin("stdlib"))
        testImplementation(kotlin("test"))
        testImplementation("junit:junit:4.13.2")
    }
} 