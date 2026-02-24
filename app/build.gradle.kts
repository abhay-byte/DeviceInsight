// F-Droid reproducible builds: disable baseline profiles using Groovy script
apply(from = "fix-baseline-profiles.gradle")

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("kapt")
}

android {
    namespace = "com.ivarna.deviceinsight"
    compileSdk = 36

    buildFeatures {
        compose = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.ivarna.deviceinsight"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        resourceConfigurations += listOf("en")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Disable dependency metadata block for F-Droid
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Disable baseline profiles for F-Droid reproducible builds
            packaging {
                resources.excludes.add("META-INF/**")
                resources.excludes.add("**.prof")
                resources.excludes.add("assets/dexopt/baseline.prof")
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }



    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // ViewModel & Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")

    // Charts/Graphs
    implementation("com.patrykandpatrick.vico:compose:2.0.0-alpha.28")
    implementation("com.patrykandpatrick.vico:compose-m3:2.0.0-alpha.28")
    implementation("com.patrykandpatrick.vico:core:2.0.0-alpha.28")

    // Jetpack Glance (Widgets)
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")

    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Haze (Glassmorphism)
    implementation("dev.chrisbanes.haze:haze:0.7.3")

    // Shizuku (System API access)
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

kapt {
    correctErrorTypes = true
}
