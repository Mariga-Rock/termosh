plugins {
    id("termosh.android.library")
    id("termosh.android.compose")
    id("termosh.android.hilt")
}

android {
    namespace = "app.termosh.core.terminal"
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
}
