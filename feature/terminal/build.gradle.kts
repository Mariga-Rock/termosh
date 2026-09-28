plugins {
    id("termosh.android.feature")
}

android {
    namespace = "app.termosh.feature.terminal"
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:security"))
    implementation(project(":core:ssh"))
    implementation(project(":core:terminal"))
    implementation(project(":core:mosh"))
    implementation(project(":core:service"))
    implementation(project(":core:datastore"))
    implementation(project(":core:database"))
    implementation(project(":core:licensing"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.coroutines.core)
}
