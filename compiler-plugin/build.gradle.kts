plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish") version "0.33.0"
}

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
        optIn.add("org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI")
    }
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
}

dependencies {
    compileOnly(kotlin("compiler"))
}

mavenPublishing  {
    signAllPublications()
    publishToMavenCentral()

    coordinates(
        "io.github.sergeylappo", "fp-equality-compiler-plugin", "0.0.2"
    )
    pom {
        name = "fp-equality-compiler-plugin"
        description = "Kotlin compiler plugin for fp-equality library"
        inceptionYear = "2025"
        url = "https://github.com/sergeylappo/fp-equality-compiler-plugin"
        licenses {
            license {
                name = "MIT License"
                url = "https://github.com/sergeylappo/fp-equality-compiler-plugin/blob/main/LICENSE"
            }
        }
        developers {
            developer {
                id = "sergeylappo"
                name = "Sergey Lappo"
                email = "SergeyLappo@gmail.com"
            }
        }
        scm {
            url = "https://github.com/sergeylappo/fp-equality-compiler-plugin"
            connection = "scm:git:git://github.com/sergeylappo/fp-equality-compiler-plugin.git"
            developerConnection = "scm:git:ssh://git@github.com/sergeylappo/fp-equality-compiler-plugin.git"
        }
    }
}