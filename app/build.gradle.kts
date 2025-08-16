plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "2.2.10"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
}

android {
    namespace = "com.jamal2367.arrcenter"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jamal2367.arrcenter"
        minSdk = 27
        targetSdk = 36
        versionCode = 10
        versionName = "1.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
		
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
	
    kotlin {
        jvmToolchain(21)
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
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.navigation:navigation-compose:2.9.3")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}
