plugins {
    id("termosh.android.library")
    id("termosh.android.hilt")
}

android {
    namespace = "app.termosh.core.licensing"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:security"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.bouncycastle.bcprov)
    implementation(libs.bouncycastle.bcpkix)

    testImplementation(libs.junit)
    testImplementation(libs.bouncycastle.bcprov)

    testImplementation(libs.orgjson)
}
