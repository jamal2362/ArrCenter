import com.android.build.api.dsl.ApplicationExtension

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10"
}

configure<ApplicationExtension> {
    namespace = "com.jamal2367.arrcenter"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jamal2367.arrcenter"
        minSdk = 31
        targetSdk = 36
        versionCode = 70
        versionName = "7.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (System.getenv("KEYSTORE_FILE") != null) {
            create("release") {
                storeFile = file(System.getenv("KEYSTORE_FILE"))
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
	
    buildFeatures {
        compose = true
    }
	
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.08.00")

    implementation(composeBom)
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.2.0")
}
