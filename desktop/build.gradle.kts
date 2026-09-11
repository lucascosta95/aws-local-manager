import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.security.MessageDigest

val appVersion = "1.3.0"
val githubOwner = "lucascosta95"
val githubRepo = "aws-local-manager"

val generateBuildConfig by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/buildconfig/kotlin")
    inputs.property("appVersion", appVersion)
    inputs.property("githubOwner", githubOwner)
    inputs.property("githubRepo", githubRepo)
    outputs.dir(outputDir)
    doLast {
        val dir = outputDir.get().asFile.resolve("dev/lucascosta/awslocalmanager")
        dir.mkdirs()
        dir.resolve("BuildConfig.kt").writeText(
            """
            package dev.lucascosta.awslocalmanager

            object BuildConfig {
                const val APP_VERSION = "$appVersion"
                const val GITHUB_OWNER = "$githubOwner"
                const val GITHUB_REPO = "$githubRepo"
            }
            """.trimIndent()
                .plus("\n"),
        )
    }
}

val skillsDir = rootProject.file("skills")

val verifySkillCatalog by tasks.registering {
    val catalogFile = File(skillsDir, "catalog.json")
    inputs.dir(skillsDir)
    doLast {
        Regex("\\{[^{}]*\\}").findAll(catalogFile.readText()).forEach { match ->
            val entry = match.value
            val path = Regex("\"path\"\\s*:\\s*\"([^\"]+)\"").find(entry)?.groupValues?.get(1)
            val expected = Regex("\"sha256\"\\s*:\\s*\"([^\"]+)\"").find(entry)?.groupValues?.get(1)
            if (path != null && expected != null) {
                val file = File(skillsDir, path)
                if (!file.isFile) {
                    error("Skill file listed in catalog.json is missing: $path")
                }
                val actual =
                    MessageDigest.getInstance("SHA-256")
                        .digest(file.readBytes())
                        .joinToString("") { byte -> "%02x".format(byte) }
                if (actual != expected) {
                    error("Checksum mismatch for $path. Set sha256 in skills/catalog.json to $actual")
                }
            }
        }
    }
}

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting {
            kotlin.srcDir(generateBuildConfig)
            resources.srcDir(skillsDir)
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.components.resources)

                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.json)

                implementation(libs.aws.sns)
                implementation(libs.aws.sqs)
                implementation(libs.aws.s3)
                implementation(libs.aws.dynamodb)

                implementation(libs.koin.core)
                implementation(libs.koin.compose)

                implementation(libs.kotlinx.coroutines)
                implementation(libs.kotlinx.serialization)
                implementation(libs.lettuce.core)
            }
        }
    }
}

tasks.matching { it.name.endsWith("ProcessResources") }.configureEach {
    dependsOn(verifySkillCatalog)
}

compose.resources {
    publicResClass = false
    generateResClass = always
}

val skikoNatives by configurations.creating

dependencies {
    skikoNatives("org.jetbrains.skiko:skiko-awt-runtime-macos-arm64:0.8.18")
    skikoNatives("org.jetbrains.skiko:skiko-awt-runtime-macos-x64:0.8.18")
    skikoNatives("org.jetbrains.skiko:skiko-awt-runtime-linux-x64:0.8.18")
    skikoNatives("org.jetbrains.skiko:skiko-awt-runtime-linux-arm64:0.8.18")
}

tasks.register<Jar>("fatJar") {
    dependsOn("desktopJar")
    archiveBaseName.set("aws-local-manager")
    archiveVersion.set(appVersion)
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "dev.lucascosta.awslocalmanager.AppKt"
        attributes["Implementation-Version"] = appVersion
    }

    from(
        configurations["desktopRuntimeClasspath"].map {
            if (it.isDirectory) it else zipTree(it)
        },
    )
    from(skikoNatives.map { zipTree(it) })
    with(tasks.getByName<Jar>("desktopJar"))
}

compose.desktop {
    application {
        mainClass = "dev.lucascosta.awslocalmanager.AppKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "aws-local-manager"
            packageVersion = appVersion
            description = "Desktop GUI for managing local AWS emulator services"
            copyright = "© 2026 AWS Local Manager"
            vendor = "Lucas Costa"
            licenseFile.set(project.file("../LICENSE"))

            jvmArgs +=
                listOf(
                    "--add-opens",
                    "java.base/sun.misc=ALL-UNNAMED",
                    "--add-opens",
                    "java.base/java.lang=ALL-UNNAMED",
                    "--add-opens",
                    "java.base/java.lang.reflect=ALL-UNNAMED",
                    "--add-opens",
                    "java.base/java.io=ALL-UNNAMED",
                    "--add-opens",
                    "java.base/java.nio=ALL-UNNAMED",
                    "--add-opens",
                    "java.base/java.util=ALL-UNNAMED",
                )

            linux {
                debMaintainer = "awslocalmanager@gmail.com"
                menuGroup = "Development"
                appCategory = "Development"
                iconFile.set(project.file("src/desktopMain/resources/icon.png"))
            }
            macOS {
                iconFile.set(project.file("src/desktopMain/resources/icon.png"))
                bundleID = "dev.lucascosta.awslocalmanager"
            }
        }

        buildTypes.release.proguard {
            isEnabled = false
        }
    }
}
