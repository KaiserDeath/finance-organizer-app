plugins {
    id("moneyflow.android.application")
    id("moneyflow.android.application.compose")
    id("moneyflow.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "pe.moneyflow.app"

    defaultConfig {
        applicationId = "pe.moneyflow.app"
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    // Core
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))

    // Features
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:transactions"))
    implementation(project(":feature:addedit"))
    implementation(project(":feature:categories"))
    implementation(project(":feature:paymentmethods"))
    implementation(project(":feature:budgets"))
    implementation(project(":feature:upcoming"))
    implementation(project(":feature:recurring"))
    implementation(project(":feature:analytics"))
    implementation(project(":feature:accounts"))
    implementation(project(":feature:savings"))
    implementation(project(":feature:currency"))

    // Android / Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.serialization.json)

    // WorkManager + Hilt worker injection (Phase 2b: recurring generation & reminders)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Security: app lock (Phase 5) — biometric prompt + FragmentActivity host
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
