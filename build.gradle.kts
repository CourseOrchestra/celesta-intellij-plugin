import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.16.0"
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
}

group = "ru.curs.celesta.intellij"
version = "1.09"

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
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.3.0")

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
