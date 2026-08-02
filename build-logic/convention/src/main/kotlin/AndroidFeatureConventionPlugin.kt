package pe.moneyflow.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * Convention for `:feature:*` modules: Android library + Compose + Hilt, plus the shared
 * core dependencies every feature needs (design system, domain, navigation, lifecycle).
 * Keeps each feature's own build file down to just its namespace.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("moneyflow.android.library")
                apply("moneyflow.android.library.compose")
                apply("moneyflow.android.hilt")
                apply("org.jetbrains.kotlin.plugin.serialization")
            }

            dependencies {
                add("implementation", project(":core:model"))
                add("implementation", project(":core:common"))
                add("implementation", project(":core:domain"))
                add("implementation", project(":core:designsystem"))
                add("implementation", project(":core:ui"))

                add("implementation", libs.findLibrary("androidx-core-ktx").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                add("implementation", libs.findLibrary("androidx-navigation-compose").get())
                add("implementation", libs.findLibrary("androidx-hilt-navigation-compose").get())
                add("implementation", libs.findLibrary("kotlinx-serialization-json").get())
                add("implementation", libs.findLibrary("androidx-compose-material3").get())
                add("implementation", libs.findLibrary("androidx-compose-foundation").get())
                add("implementation", libs.findLibrary("androidx-compose-material-icons-extended").get())

                add("testImplementation", libs.findLibrary("junit").get())
                add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
                add("testImplementation", libs.findLibrary("turbine").get())
                // Every feature ViewModel test needs MainDispatcherRule; wiring it here keeps that
                // from being re-declared (or forgotten) in each feature's build file.
                add("testImplementation", project(":core:testing"))

                // Compose UI tests. Feature modules are where composition-level defects live —
                // a section rendering when it should not, an action offered twice — and those are
                // invisible to a ViewModel test, which never composes anything.
                add("androidTestImplementation", libs.findLibrary("androidx-junit").get())
                add(
                    "androidTestImplementation",
                    libs.findLibrary("androidx-test-espresso-core").get(),
                )
                add("androidTestImplementation", libs.findLibrary("androidx-test-runner").get())
                add(
                    "androidTestImplementation",
                    libs.findLibrary("androidx-compose-ui-test-junit4").get(),
                )
                add(
                    "debugImplementation",
                    libs.findLibrary("androidx-compose-ui-test-manifest").get(),
                )
            }
        }
    }
}
