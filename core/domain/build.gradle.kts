plugins {
    id("moneyflow.jvm.library")
}

dependencies {
    api(project(":core:model"))
    api(project(":core:common"))
    implementation(libs.javax.inject)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
