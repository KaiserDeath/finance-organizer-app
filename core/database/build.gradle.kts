plugins {
    id("moneyflow.android.library")
    id("moneyflow.android.hilt")
    id("moneyflow.android.room")
}

android {
    namespace = "pe.moneyflow.core.database"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
