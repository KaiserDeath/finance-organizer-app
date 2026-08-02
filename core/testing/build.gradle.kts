plugins {
    id("moneyflow.jvm.library")
}

dependencies {
    api(project(":core:domain"))
    api(project(":core:model"))

    // `api`, not `testImplementation`: this module's *production* source is other modules' test
    // support, so consumers need JUnit and the coroutines test dispatcher on their test classpath.
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
