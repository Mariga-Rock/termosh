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
    implementation(libs.sqlcipher.android)
    implementation(libs.kotlinx.coroutines.core)
}
