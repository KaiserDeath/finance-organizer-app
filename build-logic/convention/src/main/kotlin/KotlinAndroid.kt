package pe.moneyflow.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** Shared Android + Kotlin configuration applied to every Android module. */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        compileSdk = 35

        defaultConfig {
            minSdk = 26
            // Without this, library instrumented tests compile, package, and run *nothing* —
            // `connectedAndroidTest` reports BUILD SUCCESSFUL with tests="0". Only :app declared a
            // runner, so core:ui's accessibility suite had never executed despite being written as
            // a regression guard.
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // Some IDEs and generic Gradle integrations ask Android projects for the Java-style
    // `unitTestClasses` lifecycle task. AGP names the actual variant task
    // `compileDebugUnitTestSources`, so provide a stable compatibility alias instead of forcing
    // callers to know the Android-specific name.
    tasks.register("unitTestClasses") {
        group = "verification"
        description = "Compiles the debug unit-test classes."
        dependsOn("compileDebugUnitTestSources")
    }

    // Instrumented-test equivalent of the compatibility alias above. AGP exposes variant-specific
    // compilation tasks but some IDE launchers still request the Java-style lifecycle name.
    tasks.register("androidTestClasses") {
        group = "verification"
        description = "Compiles the debug Android-test classes."
        dependsOn("compileDebugAndroidTestSources")
    }
}

/** Shared configuration for pure Kotlin/JVM (domain) modules. */
internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    extensions.configure<KotlinJvmProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
}
