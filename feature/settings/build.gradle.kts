plugins {
    id("termosh.android.feature")
}

android {
    namespace = "app.termosh.feature.settings"
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:datastore"))
    implementation(project(":core:licensing"))
    implementation(project(":core:terminal"))
    implementation(project(":core:service"))
    implementation(project(":core:ssh"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)

    testImplementation(libs.orgjson)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.orgjson)
}
