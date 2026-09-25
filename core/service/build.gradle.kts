plugins {
    id("termosh.android.library")
    id("termosh.android.hilt")
}

android {
    namespace = "app.termosh.core.service"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:ssh"))
    implementation(project(":core:mosh"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}
