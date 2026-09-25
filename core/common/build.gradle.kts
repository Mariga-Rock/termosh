plugins {
    id("termosh.android.library")
}

android {
    namespace = "app.termosh.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}
