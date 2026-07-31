plugins {
    id("moneyflow.android.library")
    id("moneyflow.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "pe.moneyflow.core.datastore"
}

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
