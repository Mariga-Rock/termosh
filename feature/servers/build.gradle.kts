plugins {
    id("termosh.android.feature")
}

android {
    namespace = "app.termosh.feature.servers"
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation(project(":domain"))
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:database"))
    implementation(project(":core:licensing"))
    implementation(project(":core:ssh"))
    implementation(project(":core:security"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.sh.calvin.reorderable)
    implementation(libs.androidx.compose.material.icons.extended)
}
