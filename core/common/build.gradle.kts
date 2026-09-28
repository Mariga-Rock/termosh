plugins {
    id("termosh.android.library")
    id("termosh.android.hilt")
}

android {
    namespace = "app.termosh.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.orgjson)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.core)
}
