import java.io.FileInputStream
import java.util.Properties

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) FileInputStream(keystorePropsFile).use { load(it) }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.pyb110.onekey"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }
    buildToolsVersion = "37.0.0"
    defaultConfig {
        applicationId = "com.pyb110.onekey"
        minSdk = 33
        targetSdk = 37
        versionCode = 75
        versionName = "1.85"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    signingConfigs {
        create("release") {
            if (keystorePropsFile.exists()) {
                storeFile = rootProject.file(keystoreProps["storeFile"] as String)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}


dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("io.github.suqi8.coui.kmp:coui-ui-android:1.1.0")
    implementation("io.github.suqi8.coui.kmp:coui-icons-android:1.1.0")
    implementation("io.github.suqi8.coui.kmp:coui-preference-android:1.1.0")
    implementation("io.github.suqi8.coui.kmp:coui-blur-android:1.1.0")
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:aidl:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    implementation("com.github.roro2239:Stellar-API:1.0.3")
}
