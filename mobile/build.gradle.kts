plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// LibreHU fork: LibreHU Apps updates when versionCode grows, and upstream's stays the same between two builds of the
// fork. versionCode = upstream's + number of commits: grows with every commit, upstream bumps included. providers.exec,
// not ProcessBuilder: the configuration cache is on.
val librehuCommitCount: Int =
    providers
        .exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            isIgnoreExitValue = true
        }.standardOutput.asText
        .get()
        .trim()
        .toIntOrNull() ?: 0

android {
    namespace = "com.shilapi.xcertplay"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // LibreHU fork: its own package (installs beside xcertplay), signed with the LibreHU key.
        applicationId = "org.librehu.carplay"
        minSdk = 28
        targetSdk = 37
        versionCode = 1400 + librehuCommitCount
        versionName = "1.4.0"
        testInstrumentationRunner = "com.shilapi.xcertplay.CarPlayMediaServiceLifecycleTest"
    }

    signingConfigs {
        create("release") {
            storeFile = file(
                providers.environmentVariable("ANDROID_KEYSTORE_PATH")
                    .getOrElse(rootProject.file("keystore/librehu.jks").path),
            )
            storePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").getOrElse("librehu")
            keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").getOrElse("librehu")
            keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").getOrElse("librehu")
        }
    }

    buildTypes {
        debug {
            if (providers.environmentVariable("ANDROID_KEYSTORE_PATH").isPresent || rootProject.file("keystore/librehu.jks").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            optimization {
                enable = false
            }
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(project(":common"))
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.app.projected)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
