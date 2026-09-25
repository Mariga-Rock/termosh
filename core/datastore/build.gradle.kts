plugins {
    id("termosh.android.library")
    id("termosh.android.hilt")
}

android {
    namespace = "app.termosh.core.datastore"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
}
