plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.20"
}

android {
    namespace = "com.example.healthchatbot"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.healthchatbot"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }

    packagingOptions {
        resources {
            pickFirsts += "/META-INF/{AL2.0,LGPL2.1}"
            pickFirsts += "**/META-INF/DEPENDENCIES"
            pickFirsts += "**/META-INF/LICENSE"
            pickFirsts += "**/META-INF/LICENSE.txt"
            pickFirsts += "**/META-INF/license.txt"
            pickFirsts += "**/META-INF/NOTICE"
            pickFirsts += "**/META-INF/NOTICE.txt"
            pickFirsts += "**/META-INF/notice.txt"
            pickFirsts += "**/META-INF/ASL2.0"
            pickFirsts += "**/META-INF/*.kotlin_module"
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

configurations.all {
    resolutionStrategy {
        // Force all Ktor dependencies to use SDK-compatible version 3.0.1 and organize them properly
        force("io.ktor:ktor-client-core:3.0.1")
        force("io.ktor:ktor-client-android:3.0.1")
        force("io.ktor:ktor-client-okhttp:3.0.1")
        force("io.ktor:ktor-utils:3.0.1")
        force("io.ktor:ktor-io:3.0.1")
        force("io.ktor:ktor-http:3.0.1")
        force("io.ktor:ktor-client-json:3.0.1")
        force("io.ktor:ktor-client-serialization:3.0.1")
        force("io.ktor:ktor-client-logging:3.0.1")

        force("io.ktor:ktor-client-content-negotiation:3.0.1")
        force("io.ktor:ktor-serialization-kotlinx-json:3.0.1")
        force("io.ktor:ktor-network:3.0.1")
        force("io.ktor:ktor-client-auth:3.0.1")
        force("io.ktor:ktor-client-encoding:3.0.1")
        force("io.ktor:ktor-client-websockets:3.0.1")

        // Force JVM compatibility versions to 3.0.1
        force("io.ktor:ktor-client-core-jvm:3.0.1")
        force("io.ktor:ktor-client-content-negotiation-jvm:3.0.1")
        force("io.ktor:ktor-client-logging-jvm:3.0.1")
        force("io.ktor:ktor-client-auth-jvm:3.0.1")
        force("io.ktor:ktor-client-encoding-jvm:3.0.1")
        force("io.ktor:ktor-client-websockets-jvm:3.0.1")
        force("io.ktor:ktor-http-jvm:3.0.1")
        force("io.ktor:ktor-utils-jvm:3.0.1")
        force("io.ktor:ktor-io-jvm:3.0.1")
        force("io.ktor:ktor-network-jvm:3.0.1")

        // Force plugin-specific dependencies that might have the missing methods
        // These may not exist in 3.0.1, removing for now

        // Force exact kotlinx-serialization version as requested
        force("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
        force("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3")

        // Force AndroidX Security version
        force("androidx.security:security-crypto:1.1.0-alpha06")

        // Fix BouncyCastle version conflicts
        force("org.bouncycastle:bcprov-jdk15to18:1.72")
        force("org.bouncycastle:bcpkix-jdk15to18:1.72")
        force("org.bouncycastle:bcutil-jdk15to18:1.72")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // RunAnywhere SDK
    implementation(files("libs/RunAnywhereKotlinSDK-release-v0.1.3.aar"))
    implementation(files("libs/runanywhere-llm-llamacpp-release-v0.1.3.aar"))

    // CRITICAL FIX: AndroidX Security library for MasterKey$Builder (required by RunAnywhere SDK)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Explicit BouncyCastle dependencies to prevent version conflicts
    implementation("org.bouncycastle:bcprov-jdk15to18:1.72")
    implementation("org.bouncycastle:bcpkix-jdk15to18:1.72")
    implementation("org.bouncycastle:bcutil-jdk15to18:1.72")

    // CRITICAL FIX: Use Ktor 3.0.1 - Latest stable version that might have the exact getHttpTimeout API
    val ktorVersion = "3.0.1"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-android:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-utils:$ktorVersion")
    implementation("io.ktor:ktor-io:$ktorVersion")
    implementation("io.ktor:ktor-http:$ktorVersion")

    // COMPREHENSIVE JVM COMPATIBILITY LAYER for RunAnywhere SDK
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-http-jvm:$ktorVersion")
    implementation("io.ktor:ktor-utils-jvm:$ktorVersion")
    implementation("io.ktor:ktor-io-jvm:$ktorVersion")
    implementation("io.ktor:ktor-network-jvm:$ktorVersion")

    // COMPLETE PLUGIN ECOSYSTEM with timeout support
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-encoding:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    // JVM versions of all plugins
    implementation("io.ktor:ktor-client-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-encoding-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets-jvm:$ktorVersion")

    // Additional modules that might be needed
    implementation("io.ktor:ktor-client-json:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization:$ktorVersion")
    implementation("io.ktor:ktor-network:$ktorVersion")

    // EXACT KOTLINX-SERIALIZATION VERSION AS REQUESTED (compatible with Ktor 3.0.1)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Other project dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}