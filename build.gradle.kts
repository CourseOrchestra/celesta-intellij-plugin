import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings
import org.jetbrains.intellij.tasks.RunPluginVerifierTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java
    id("org.jetbrains.intellij") version "1.17.4"
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
}

group = "ru.curs.celesta.intellij"
version = "1.08"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.1.0")
}

idea {
    project {
        settings {
            runConfigurations {

                create("Run Plugin", Gradle::class) {
                    projectPath = projectDir.absolutePath
                    taskNames = setOf(":runIde")
                }
                create("Build Plugin", Gradle::class) {
                    projectPath = projectDir.absolutePath
                    taskNames = setOf(":buildPlugin")
                }
            }
        }
    }
}

intellij {
    version.set("2023.3.7")
    type.set("IU")
    plugins.set(listOf("DatabaseTools", "JPA", "java", "maven", "maven-model"))
}

tasks {
    runPluginVerifier {
        ideVersions.set(listOf("IU-2023.3.4"))
        failureLevel.set(
            setOf(
                RunPluginVerifierTask.FailureLevel.INVALID_PLUGIN,
                RunPluginVerifierTask.FailureLevel.COMPATIBILITY_PROBLEMS
            )
        )
    }


    compileKotlin {
        compilerOptions{
            jvmTarget = JvmTarget.JVM_17
        }
    }

    compileTestKotlin {
        compilerOptions{
            jvmTarget = JvmTarget.JVM_17
        }
    }

    patchPluginXml {
        changeNotes.set(File("changelog.html").readText(Charsets.UTF_8))
        sinceBuild.set("233")
        untilBuild.set("241.*")
    }
}