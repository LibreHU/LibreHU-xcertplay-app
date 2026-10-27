plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// LibreHU fork: LibreHU Apps updates when versionCode grows, and upstream's stays the same between two builds of the
// fork. versionCode = upstream's + number of commits: grows with every commit, upstream bumps included. providers.exec,
// not ProcessBuilder: the configuration cache is on.
// "-<branch>" at the end of versionName: the branch the APK was built from (GITHUB_REF_NAME in the CI).
val librehuBranch: String =
    System.getenv("GITHUB_REF_NAME")?.takeIf { it.isNotBlank() } ?: providers
        .exec {
            commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
            isIgnoreExitValue = true
        }.standardOutput.asText
        .get()
        .trim()
        .ifEmpty { "local" }

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
        versionCode = 1402 + librehuCommitCount
        versionName = "1.4.2-$librehuBranch"

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
            // LibreHU: optimised by R8 like mobile (without renaming: src/main/keepRules/librehu.keep).
            optimization {
                enable = true
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
    implementation(project(":common"))
    implementation(project(":shared"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.app.automotive)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
