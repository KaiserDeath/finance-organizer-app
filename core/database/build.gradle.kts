plugins {
    id("moneyflow.android.library")
    id("moneyflow.android.hilt")
    id("moneyflow.android.room")
}

android {
    namespace = "pe.moneyflow.core.database"

    // MigrationTestHelper reads the exported schemas off the test APK's assets, so the directory
    // KSP writes to has to be packaged into it. Without this the helper fails at construction with
    // "Cannot find the schema file", not with a useful message about the missing asset.
    sourceSets.getByName("androidTest") {
        assets.srcDir("$projectDir/schemas")
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    // Migration tests run against a real SQLite file on the device: an ALTER TABLE that is wrong
    // only fails where it actually executes.
    androidTestImplementation(libs.room.testing)
}
