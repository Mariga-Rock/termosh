plugins {
    id("termosh.android.library")
    id("termosh.android.hilt")
}

android {
    namespace = "app.termosh.core.security"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)
}
