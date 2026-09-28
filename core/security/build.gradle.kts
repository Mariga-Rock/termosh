plugins {
    id("termosh.android.library")
    id("termosh.android.hilt")
}

android {
    namespace = "app.termosh.core.security"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":domain"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.bouncycastle.bcprov)
    implementation(libs.bouncycastle.bcpkix)

    testImplementation(libs.junit)
}
