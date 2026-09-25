plugins {
    id("termosh.android.library")
    id("termosh.android.hilt")
}

android {
    namespace = "app.termosh.core.mosh"
    ndkVersion = "26.1.10909125"
    defaultConfig {
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
            }
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:ssh"))
    implementation(libs.kotlinx.coroutines.core)
}
