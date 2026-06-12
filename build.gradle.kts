plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

group = "com.strangeparticle"
version = "0.1.0"

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
        nativeMain.dependencies {
            implementation(libs.clikt)
        }
        nativeTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
