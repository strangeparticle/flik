plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "com.strangeparticle"

// The version is declared in exactly one place: flikVersion in gradle.properties.
// It feeds both the Gradle project version (for build/packaging tooling) and the
// generated runtime constant below.
val flikVersion: String = providers.gradleProperty("flikVersion").get()
version = flikVersion

// Regenerates the runtime version constant from flikVersion before each compile.
// The output lives under build/ (git-ignored), so the value is never duplicated in
// tracked source and cannot drift from gradle.properties.
val generatedVersionDir = layout.buildDirectory.dir("generated/flikVersion")

val generateKotlinVersionFile = tasks.register("GenerateKotlinVersionFile") {
    val outputDir = generatedVersionDir
    val versionValue = flikVersion
    inputs.property("flikVersion", versionValue)
    outputs.dir(outputDir)
    doLast {
        val packageDir = outputDir.get().dir("com/strangeparticle/flik").asFile
        packageDir.mkdirs()
        packageDir.resolve("FlikVersion.kt").writeText(
            """
            package com.strangeparticle.flik

            object FlikVersion {
                const val VERSION = "$versionValue"
            }
            """.trimIndent() + "\n",
        )
    }
}

kotlin {
    val nativeTargets = listOf(
        macosArm64(),
        macosX64(),
        linuxX64(),
        mingwX64(),
    )

    nativeTargets.forEach { target ->
        target.binaries {
            executable {
                entryPoint = "com.strangeparticle.flik.main"
                baseName = "flik"
            }
        }
    }

    sourceSets {
        nativeMain {
            kotlin.srcDir(generatedVersionDir)
            dependencies {
                implementation(libs.clikt)
            }
        }
        nativeTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// Ensure the generated constant exists before any native (main or test) compile.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile>().configureEach {
    dependsOn(generateKotlinVersionFile)
}
