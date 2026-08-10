plugins {
    id("moneyflow.android.feature")
}

android {
    namespace = "pe.moneyflow.feature.addedit"
}

dependencies {
    // BackHandler for the unsaved-changes exit guard.
    implementation(libs.androidx.activity.compose)
}
