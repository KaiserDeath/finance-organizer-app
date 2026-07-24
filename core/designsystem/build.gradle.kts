plugins {
    id("moneyflow.android.library")
    id("moneyflow.android.library.compose")
}

android {
    namespace = "pe.moneyflow.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.material.icons.extended)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.core.ktx)
}
