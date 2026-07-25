import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "pe.moneyflow.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

// Register each convention plugin so modules can apply them by id.
gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "moneyflow.android.application"
            implementationClass = "pe.moneyflow.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id = "moneyflow.android.application.compose"
            implementationClass = "pe.moneyflow.buildlogic.AndroidApplicationComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = "moneyflow.android.library"
            implementationClass = "pe.moneyflow.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "moneyflow.android.library.compose"
            implementationClass = "pe.moneyflow.buildlogic.AndroidLibraryComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "moneyflow.android.feature"
            implementationClass = "pe.moneyflow.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("androidHilt") {
            id = "moneyflow.android.hilt"
            implementationClass = "pe.moneyflow.buildlogic.AndroidHiltConventionPlugin"
        }
        register("androidRoom") {
            id = "moneyflow.android.room"
            implementationClass = "pe.moneyflow.buildlogic.AndroidRoomConventionPlugin"
        }
        register("jvmLibrary") {
            id = "moneyflow.jvm.library"
            implementationClass = "pe.moneyflow.buildlogic.JvmLibraryConventionPlugin"
        }
    }
}
