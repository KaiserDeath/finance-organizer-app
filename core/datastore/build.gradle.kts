plugins {
    id("moneyflow.android.library")
    id("moneyflow.android.hilt")
}

android {
    namespace = "pe.moneyflow.core.datastore"
}

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
}
