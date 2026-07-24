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
    implementation(libs.androidx.core.ktx)
}
