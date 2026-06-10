plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.perf)
}

import java.util.Properties

// Load local.properties (kept out of version control) so secrets like AdMob unit IDs and the
// release keystore never live in tracked source. Order of precedence for each value:
//   1. local.properties   2. environment variable (used by CI)   3. a safe fallback.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun secret(key: String, fallback: String): String =
    (localProps.getProperty(key) ?: System.getenv(key) ?: fallback)

// Google's official AdMob *test* IDs — used whenever real IDs aren't configured, so debug/PR
// builds never serve (or accidentally click) real ads. See https://developers.google.com/admob/android/test-ads
val testAdAppId = "ca-app-pub-3940256099942544~3347511713"
val testBannerId = "ca-app-pub-3940256099942544/9214589741"
val testInterstitialId = "ca-app-pub-3940256099942544/1033173712"
val testRewardedId = "ca-app-pub-3940256099942544/5224354917"

val admobAppId = secret("ADMOB_APP_ID", testAdAppId)
val admobBannerId = secret("ADMOB_BANNER_ID", testBannerId)
val admobInterstitialId = secret("ADMOB_INTERSTITIAL_ID", testInterstitialId)
val admobRewardedId = secret("ADMOB_REWARDED_ID", testRewardedId)

val githubApiToken = localProps.getProperty("github.api.token") ?: System.getenv("GH_API_TOKEN") ?: ""
val githubRepoOwner = localProps.getProperty("github.repo.owner") ?: System.getenv("GH_REPO_OWNER") ?: ""
val githubRepoName = localProps.getProperty("github.repo.name") ?: System.getenv("GH_REPO_NAME") ?: ""

android {
    namespace = "com.charles.skypulse.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.charles.skypulse.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 15
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // AdMob unit IDs — never hardcoded; sourced from local.properties / CI secrets (see top of file).
        buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobBannerId\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$admobInterstitialId\"")
        buildConfigField("String", "ADMOB_REWARDED_ID", "\"$admobRewardedId\"")
        // AdMob application id is required as a manifest <meta-data> entry.
        manifestPlaceholders["admobAppId"] = admobAppId

        // GitHub In-App Feedback config injected into BuildConfig
        buildConfigField("String", "GITHUB_API_TOKEN", "\"$githubApiToken\"")
        buildConfigField("String", "GITHUB_REPO_OWNER", "\"$githubRepoOwner\"")
        buildConfigField("String", "GITHUB_REPO_NAME", "\"$githubRepoName\"")
    }

    signingConfigs {
        create("release") {
            val keystorePath = secret("KEYSTORE_FILE", "")
            val ksFile = if (keystorePath.isNotBlank()) rootProject.file(keystorePath) else null
            val ksPassword = secret("KEYSTORE_PASSWORD", "")
            // Only wire up signing when a real, non-empty keystore + password are present, so that
            // missing/empty CI secrets fall back to an unsigned release instead of failing the build.
            if (ksFile != null && ksFile.exists() && ksFile.length() > 0L && ksPassword.isNotBlank()) {
                storeFile = ksFile
                storePassword = ksPassword
                keyAlias = secret("KEY_ALIAS", "")
                keyPassword = secret("KEY_PASSWORD", "")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            // Crashlytics mapping uploads automatically via the gradle plugin.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Sign with the release keystore only when one is configured (CI / local secrets);
            // otherwise the release build stays unsigned so PR/local `assembleRelease` still works.
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // Emit SARIF so CI can upload Lint findings to GitHub code scanning.
        sarifReport = true
        // Don't fail the build on lint findings; they surface in the Security tab instead.
        abortOnError = false
        // "Update available" checks are owned by Dependabot, not lint — silence the nags.
        disable.add("GradleDependency")
        disable.add("AndroidGradlePluginVersion")
        disable.add("NewerVersionAvailable")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // WorkManager + location + map
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.play.services.location)
    implementation(libs.osmdroid.android)
    implementation(libs.accompanist.permissions)

    // Firebase (Spark tier)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.perf)

    // Ads (AdMob) + Google UMP consent
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    // Tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-metadata-jvm:2.3.21")
    }
}
