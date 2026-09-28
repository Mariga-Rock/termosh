import java.util.Properties

val keystoreProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

plugins {
    id("termosh.android.application")
    id("termosh.android.compose")
    id("termosh.android.hilt")
}

android {
    namespace = "app.termosh"

    defaultConfig {
        flavorDimensions += "edition"
        applicationId = "app.termosh"
        versionCode = 1
        versionName = "0.1.0"
    }

    productFlavors {
        create("public") {
            dimension = "edition"
        }
        create("personal") {
            dimension = "edition"
            applicationIdSuffix = ".personal"
            versionNameSuffix = "-personal"
        }
        create("beta") {
            dimension = "edition"
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-beta"
        }
    }

    signingConfigs {
        create("termoshRelease") {
            val storePath = keystoreProps.getProperty("termosh.storeFile")
                ?: System.getenv("TERMOSH_STORE_FILE")
                ?: "${System.getProperty("user.home")}/termosh-release.jks"
            storeFile = if (storePath.startsWith("/")) file(storePath)
                        else file("${System.getProperty("user.home")}/$storePath")
            storePassword = keystoreProps.getProperty("termosh.storePassword")
                ?: System.getenv("TERMOSH_STORE_PASSWORD")
                ?: ""
            keyAlias = keystoreProps.getProperty("termosh.keyAlias") ?: "termosh"
            keyPassword = keystoreProps.getProperty("termosh.keyPassword")
                ?: System.getenv("TERMOSH_KEY_PASSWORD")
                ?: ""
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("termoshRelease")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += setOf(
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module",
                "META-INF/INDEX.LIST",
                "META-INF/{AL2.0,LGPL2.1}",
            )
        }
    }
}

dependencies {
    implementation(project(":domain"))

    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:security"))
    implementation(project(":core:licensing"))
    implementation(libs.bouncycastle.bcprov)
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
