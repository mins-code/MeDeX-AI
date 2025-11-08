plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
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
        // Force all Ktor dependencies to use version 2.3.7 consistently
        force("io.ktor:ktor-client-core:2.3.7")
        force("io.ktor:ktor-client-android:2.3.7")
        force("io.ktor:ktor-client-okhttp:2.3.7")
        force("io.ktor:ktor-client-cio:2.3.7")
        force("io.ktor:ktor-http:2.3.7")
        force("io.ktor:ktor-utils:2.3.7")
        force("io.ktor:ktor-io:2.3.7")
        force("io.ktor:ktor-network:2.3.7")
        force("io.ktor:ktor-client-content-negotiation:2.3.7")
        force("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
        force("io.ktor:ktor-client-auth:2.3.7")
        force("io.ktor:ktor-client-logging:2.3.7")
        force("io.ktor:ktor-client-websockets:2.3.7")
        force("io.ktor:ktor-client-encoding:2.3.7")
        force("io.ktor:ktor-client-apache:2.3.7")
        force("io.ktor:ktor-client-java:2.3.7")
        force("kotlinx.serialization:kotlinx-serialization-json:1.6.0")
        force("kotlinx.serialization:kotlinx-serialization-core:1.6.0")

        // Prevent any conflicting versions
        exclude(group = "io.ktor", module = "ktor-client-jvm")
        exclude(group = "io.ktor", module = "ktor-http-jvm")
        exclude(group = "io.ktor", module = "ktor-utils-jvm")
        exclude(group = "io.ktor", module = "ktor-io-jvm")
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

    // DEFINITIVE FIX: Manually include the transitive dependencies required by the RunAnywhere SDK.
    // 1. AndroidX Security for secure storage (solves MasterKey$Builder crash).
    implementation("androidx.security:security-crypto:1.0.0")

    // COMPREHENSIVE KTOR FIX - Multiple version strategy to find compatibility

    // Try the exact version RunAnywhere SDK was likely compiled against
    val ktorVersion = "2.3.7" // Version mentioned in error logs

    // Core Ktor client with all required implementations
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-android:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    // HTTP and utilities - the problematic classes
    implementation("io.ktor:ktor-http:$ktorVersion")
    implementation("io.ktor:ktor-utils:$ktorVersion")
    implementation("io.ktor:ktor-io:$ktorVersion")

    // Content negotiation and serialization
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Network and connection handling
    implementation("io.ktor:ktor-network:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    // Additional modules that might contain missing classes
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("io.ktor:ktor-client-encoding:$ktorVersion")

    // JVM-specific implementations (might be needed despite Android)
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-java:$ktorVersion")

    // Serialization dependencies compatible with this Ktor version
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.0")

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