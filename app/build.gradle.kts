plugins {
//    id("com.android.application")
//    id("org.jetbrains.kotlin.android")
//    id("com.google.devtools.ksp")
//    id("com.google.gms.google-services")
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
//    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.example.audioandvideoeditor"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.audioandvideoeditor"
        minSdk = 24
        targetSdk = 36
        versionCode = 10
        versionName = "1.0.10"
        ndkVersion="25.1.8937393"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
//        ndk{
//            abiFilters.addAll(arrayOf("arm64-v8a", "x86"))
//        }
        splits {
            abi {
                // 启用 ABI 分割
                isEnable = true
                // 清除默认包含的 ABI（如果有的话）
                reset()
                // 指定你想要包含的 ABI。
                // 建议包含 arm64-v8a 和 armeabi-v7a，因为它们覆盖了绝大多数设备。
                // 如果你的应用在模拟器上运行，或者需要支持一些较老的Intel设备，可以加上 x86 和 x86_64。"armeabi-v7a", "arm64-v8a", "x86", "x86_64"
                include("arm64-v8a","x86_64")
                // 设置为 false，表示生成多个 ABI 特定的 APK，而不是一个包含所有 ABI 的通用 APK
                isUniversalApk = false
            }
        }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        aidl=true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    externalNativeBuild {
        cmake {
            path =file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {

//    implementation("androidx.core:core-ktx:1.13.1")
//    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
//    implementation("androidx.activity:activity-compose:1.9.1")
//    implementation(platform("androidx.compose:compose-bom:2023.08.00"))

    // Specify the Compose BOM with a version definition
//    val composeBom = platform("androidx.compose:compose-bom:2025.10.00")

//    implementation("androidx.compose.ui:ui")
//    implementation("androidx.compose.ui:ui-graphics")
//    implementation("androidx.compose.ui:ui-tooling-preview")
//    implementation("androidx.compose.material3:material3")
//    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.documentfile:documentfile:1.1.0")
    //implementation("com.android.volley:volley:1.2.1")
//    testImplementation("junit:junit:4.13.2")
//    androidTestImplementation("androidx.test.ext:junit:1.2.1")
//    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
//    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
//    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
//    debugImplementation("androidx.compose.ui:ui-tooling")
//    debugImplementation("androidx.compose.ui:ui-test-manifest")


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)


    implementation ("androidx.navigation:navigation-compose:2.5.3")
    // optional - Jetpack Compose integration
    implementation("androidx.paging:paging-compose:3.3.2")

    val room_version = "2.6.1"

    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")
    // To use Kotlin Symbol Processing (KSP)
    ksp("androidx.room:room-compiler:$room_version")
    // 🔥 核心：启用 Coroutine 特性（suspend, Flow, withTransaction）
    implementation("androidx.room:room-ktx:$room_version")


    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
//      implementation("androidx.media3:media3-exoplayer:1.8.0")
//      implementation("androidx.media3:media3-exoplayer-dash:1.8.0")
//      implementation("androidx.media3:media3-ui:1.8.0")

    //implementation("io.coil-kt.coil3:coil-compose:3.1.0")
    //implementation(platform("com.google.firebase:firebase-bom:34.0.0"))
//    implementation(platform("com.google.firebase:firebase-bom:32.2.3"))
//    implementation("com.google.firebase:firebase-analytics")
//    implementation("com.google.firebase:firebase-config")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
//    implementation("androidx.fragment:fragment-ktx:1.6.2")
}