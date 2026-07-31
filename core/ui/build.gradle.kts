plugins {
    id("moneyflow.android.library")
    id("moneyflow.android.library.compose")
}

android {
    namespace = "pe.moneyflow.core.ui"
}

dependencies {
    api(project(":core:designsystem"))
    api(project(":core:model"))
    api(project(":core:common"))
    // For the shared InsightCard: Insight/InsightSeverity are plain data types that happen to live
    // in :core:domain's model package. :core:domain is a pure JVM library, so there is no cycle.
    api(project(":core:domain"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)

    testImplementation(libs.junit)

    // Instrumented UI tests: these exist specifically to hold the accessibility fixes in place
    // (48dp touch targets, amount reflow at large font scale, real clickable affordances).
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
