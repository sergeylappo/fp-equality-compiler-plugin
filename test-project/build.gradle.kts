plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":compiler-plugin"))
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    kotlinCompilerPluginClasspath(project(":compiler-plugin"))
}

tasks.test {
    useJUnitPlatform()
} 