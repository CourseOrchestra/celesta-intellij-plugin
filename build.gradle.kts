import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.18.1"
    id("org.jetbrains.kotlin.jvm") version "2.4.10"
    id("dev.detekt") version "2.0.0-alpha.5"
}

group = "ru.curs.celesta.intellij"
version = "1.10"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.4.10")

    // CelestaSQL parser used to validate score files in the editor. Bundled into the plugin's lib/.
    // slf4j-api is already provided by the IntelliJ Platform, so exclude it to avoid a duplicate copy.
    implementation("ru.curs:celesta-core:8.2.1") {
        exclude(group = "org.slf4j")          // already provided by the IntelliJ Platform
        exclude(group = "com.h2database")     // runtime DB driver, not needed for score parsing
    }

    // CursorGenerator: generates Celesta cursor classes in-process (instead of running a Maven goal).
    // Its Maven dependencies are "provided" scope, so consuming it pulls only celesta-core + javapoet.
    implementation("ru.curs:celesta-maven-plugin:8.2.1") {
        exclude(group = "org.slf4j")
        exclude(group = "com.h2database")
    }

    intellijPlatform {
        create("IU", "2026.1.3")

        bundledPlugins(
            "com.intellij.database",
            "com.intellij.java",
            "org.jetbrains.idea.maven"
        )

        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }

    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        version = project.version.toString()
        changeNotes = file("changelog.html").readText(Charsets.UTF_8)

        ideaVersion {
            sinceBuild = "261"
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        // The plugin ID "ru.curs.celesta.intellij" is already published; renaming it would
        // break updates for existing users, so mute the marketplace naming-convention check.
        freeArgs = listOf("-mute", "TemplateWordInPluginId")

        ides {
            recommended()
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("config/detekt/detekt.yml"))
    basePath.set(projectDir)
}

tasks {
    compileKotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
            // Rely on the platform interfaces' real JVM default methods instead of emitting
            // compatibility delegates. This avoids generating a delegate for the deprecated
            // MavenProjectsTree.Listener.projectResolved(Pair, NativeMavenProjectHolder),
            // which is removed in newer IDE builds (2026.2+).
            freeCompilerArgs.add("-Xjvm-default=all")
        }
    }

    compileTestKotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
            freeCompilerArgs.add("-Xjvm-default=all")
        }
    }
}
