plugins {
    id("termosh.android.library")
    id("termosh.android.compose")
}

android {
    namespace = "app.termosh.core.ui"
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:common"))
}
