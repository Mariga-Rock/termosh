plugins {
    id("termosh.android.library")
    id("termosh.android.room")
    id("termosh.android.hilt")
}

android {
    namespace = "app.termosh.core.database"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:security"))
    implementation(project(":domain"))
    implementation(libs.sqlcipher.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.orgjson)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.core)
}
