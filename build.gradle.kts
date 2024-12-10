plugins {
    id("java")
}

group = "io.github.thegatesdev"
version = "0.3"
description = "Kill player when timer expires"
java.sourceCompatibility = JavaVersion.VERSION_17

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.3-R0.1-SNAPSHOT")
}

tasks {
    processResources {
        filteringCharset = Charsets.UTF_8.name()
        val props = mapOf(
            "name" to project.name,
            "version" to project.version,
            "description" to project.description,
            "apiVersion" to "1.21"
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(17)
    }
}