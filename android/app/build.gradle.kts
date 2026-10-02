plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Firma release: propiedades privadas en ~/.gradle/gradle.properties (nunca en el repo)
val rutaKeystore: String? = project.findProperty("CALENDARIO_KEYSTORE") as String?
val claveKeystore: String? = project.findProperty("CALENDARIO_STORE_PASSWORD") as String?
val aliasKeystore: String? = project.findProperty("CALENDARIO_KEY_ALIAS") as String?
val claveKey: String? = project.findProperty("CALENDARIO_KEY_PASSWORD") as String?

android {
    namespace = "com.jose.calendario"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.jose.calendario"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        if (rutaKeystore != null && claveKeystore != null) {
            create("release") {
                storeFile = file(rutaKeystore)
                storePassword = claveKeystore
                keyAlias = aliasKeystore
                keyPassword = claveKey ?: claveKeystore
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
}
