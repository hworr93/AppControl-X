import org.gradle.api.GradleException

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.appcontrolx"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.appcontrolx"
        minSdk = 29
        targetSdk = 34
        versionCode = 3
        versionName = "4.0.0"
    }

    val releaseSigningEnvVars = listOf(
        "KEYSTORE_FILE",
        "KEYSTORE_PASSWORD",
        "KEY_ALIAS",
        "KEY_PASSWORD"
    )
    val isReleaseBuildRequested = gradle.startParameter.taskNames.any { taskName ->
        taskName.contains("Release", ignoreCase = true)
    }

    signingConfigs {
        create("release") {
            val signingValues = releaseSigningEnvVars.associateWith { envName ->
                System.getenv(envName)?.trim().orEmpty().ifEmpty { null }
            }
            val missingVars = signingValues.filterValues { it == null }.keys
            val hasAnySigningInput = signingValues.values.any { it != null }

            if (hasAnySigningInput && missingVars.isNotEmpty()) {
                throw GradleException(
                    "Incomplete release signing configuration. Missing: ${missingVars.joinToString(", ")}. " +
                        "Provide all required variables: ${releaseSigningEnvVars.joinToString(", ")}."
                )
            }

            if (isReleaseBuildRequested && missingVars.isNotEmpty()) {
                throw GradleException(
                    "Release signing is required but missing environment variables: ${missingVars.joinToString(", ")}. " +
                        "Set ${releaseSigningEnvVars.joinToString(", ")} before building release artifacts."
                )
            }

            if (missingVars.isEmpty()) {
                val keystorePath = signingValues.getValue("KEYSTORE_FILE")!!
                val resolvedKeystore = file(keystorePath)
                if (isReleaseBuildRequested && !resolvedKeystore.exists()) {
                    throw GradleException("KEYSTORE_FILE does not exist: $keystorePath")
                }

                storeFile = resolvedKeystore
                storePassword = signingValues.getValue("KEYSTORE_PASSWORD")
                keyAlias = signingValues.getValue("KEY_ALIAS")
                keyPassword = signingValues.getValue("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            isDebuggable = true
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
        buildConfig = true
        aidl = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        disable += "QueryAllPackagesPermission"
    }

}

dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")

    // Shizuku
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    // libsu (Root)
    implementation("com.github.topjohnwu.libsu:core:5.2.2")
    implementation("com.github.topjohnwu.libsu:service:5.2.2")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
}
