import org.jetbrains.changelog.Changelog

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.platform)
    alias(libs.plugins.changelog)
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create(
            providers.gradleProperty("platformType"),
            providers.gradleProperty("platformVersion"),
        )
        bundledPlugins("JavaScript", "NodeJS", "gherkin")

        pluginVerifier()
        zipSigner()
    }
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.vintage.engine)
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    instrumentCode = false

    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null }
        }

        val pluginDescriptionStart = "<!-- Plugin description -->"
        val pluginDescriptionEnd = "<!-- Plugin description end -->"
        description = providers
            .fileContents(layout.projectDirectory.file("README.md"))
            .asText
            .map { readme ->
                require(readme.contains(pluginDescriptionStart) && readme.contains(pluginDescriptionEnd)) {
                    "Plugin description markers not found in README.md"
                }
                readme
                    .substringAfter(pluginDescriptionStart)
                    .substringBefore(pluginDescriptionEnd)
                    .trim()
            }

        changeNotes = providers.gradleProperty("pluginVersion").map { version ->
            with(changelog) {
                renderItem(
                    (getOrNull(version) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
}

changelog {
    groups = emptyList()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

tasks.test {
    useJUnitPlatform()
}
