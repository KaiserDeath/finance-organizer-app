plugins {
    id("moneyflow.android.library")
    id("moneyflow.android.hilt")
}

android {
    namespace = "pe.moneyflow.core.data"
}

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
