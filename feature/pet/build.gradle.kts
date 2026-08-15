plugins {
    id("moneyflow.android.feature")
}

android {
    namespace = "pe.moneyflow.feature.pet"
}

dependencies {
    androidTestImplementation(project(":core:testing"))
}
