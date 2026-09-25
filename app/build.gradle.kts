plugins {
    id("termosh.android.application")
    id("termosh.android.compose")
    id("termosh.android.hilt")
}

android {
    namespace = "app.termosh"
    defaultConfig {
        applicationId = "app.termosh"
        versionCode = 1
        versionName = "0.1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":domain"))

    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:security"))
    implementation(project(":core:ssh"))
    implementation(project(":core:mosh"))
    implementation(project(":core:terminal"))
    implementation(project(":core:service"))

    implementation(project(":feature:servers"))
    implementation(project(":feature:terminal"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:license"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)
}
