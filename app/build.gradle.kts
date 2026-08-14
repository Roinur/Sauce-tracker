import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.roinur.saucetracker"
    compileSdk = 34

    val signingProperties = Properties().apply {
        val signingFile = rootProject.file("signing.properties")
        if (signingFile.isFile) {
            signingFile.inputStream().use { load(it) }
        }
    }
    fun signingValue(name: String): String? =
        (findProperty(name) as String?)
            ?.takeIf(String::isNotBlank)
            ?: signingProperties.getProperty(name)?.takeIf(String::isNotBlank)
            ?: System.getenv(name)?.takeIf(String::isNotBlank)

    val releaseStoreFile = signingValue("SAUCE_RELEASE_STORE_FILE")
    val releaseStorePassword = signingValue("SAUCE_RELEASE_STORE_PASSWORD")
    val releaseKeyAlias = signingValue("SAUCE_RELEASE_KEY_ALIAS")
    val releaseKeyPassword = signingValue("SAUCE_RELEASE_KEY_PASSWORD")
    val hasReleaseSigning = listOf(
        releaseStoreFile,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword
    ).all { !it.isNullOrBlank() }

    defaultConfig {
        applicationId = "com.roinur.saucetracker"
        minSdk = 26
        targetSdk = 34
        versionCode = 12
        versionName = "1.8"
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(requireNotNull(releaseStoreFile))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".rewrite"
            versionNameSuffix = "-rewrite"
            resValue("string", "app_name", "Sauce Tracker Rewrite")
        }
        release {
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                null
            }
            // Keep release stable while features iterate; avoid R8/resource shrink regressions.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("profile") {
            initWith(getByName("release"))
            applicationIdSuffix = ".rewrite"
            versionNameSuffix = "-rewrite-profile"
            resValue("string", "app_name", "Sauce Tracker Rewrite")
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            matchingFallbacks += listOf("release")
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    testImplementation("junit:junit:4.13.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
