plugins {
    id("termosh.android.library")
    id("termosh.android.hilt")
}

android {
    ndkVersion = "26.1.10909125"
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    namespace = "app.termosh.core.mosh"

    packaging {
        jniLibs {
            // ВАЖНО: useLegacyPackaging=true распаковывает .so в /data/app/.../lib/
            // Без этого mosh-client нельзя запустить как бинарник (W^X)
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:ssh"))
    implementation(project(":domain"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
}
