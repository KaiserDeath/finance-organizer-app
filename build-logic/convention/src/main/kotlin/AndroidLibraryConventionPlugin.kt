package pe.moneyflow.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
            }

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
            }

            dependencies {
                // `configureKotlinAndroid` declares AndroidJUnitRunner for every module here, so
                // the runner has to actually be on the classpath of every module here. It was not:
                // only :core:ui and the feature plugin added it, which left core:data,
                // core:datastore and core:database declaring a runner they did not ship.
                //
                // The symptom was not a skipped test. Their `connectedDebugAndroidTest` crashed
                // with ClassNotFoundException on AndroidJUnitRunner, so a full instrumented run
                // failed on modules that have no instrumented tests at all — which trains you to
                // read a red build as normal, and that is how a real failure gets through.
                //
                // Paired with the declaration rather than left to each module for the same reason
                // the declaration itself is shared: a module that opts out by forgetting looks
                // exactly like a module that has nothing to run.
                add("androidTestImplementation", libs.findLibrary("androidx-junit").get())
                add(
                    "androidTestImplementation",
                    libs.findLibrary("androidx-test-espresso-core").get(),
                )
                add("androidTestImplementation", libs.findLibrary("androidx-test-runner").get())
            }
        }
    }
}
