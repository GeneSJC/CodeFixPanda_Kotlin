import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Reads GITHUB_API_TOKEN from local.properties (gitignored by default, never
// committed) and exposes it as a BuildConfig field. If the key is absent,
// this resolves to an empty string — GitHub's API still works unauthenticated,
// just at a much lower rate limit.
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}

// Actual config values live here, as plain vals — the buildConfigField calls
// below just reference these via string interpolation, instead of each
// value being buried inline inside an escaped-quote string. Edit a value
// here; nothing else needs to change to pick it up.
val cmVersion = "v0.27"
val splashAnimation = "coin" // "coin" or "origami"
val landingExperience = "repoFirst" // "classic" or "repoFirst"

android {
    namespace = "com.example.githubissuefinder"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.githubissuefinder"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "GITHUB_API_TOKEN",
            "\"${localProperties.getProperty("GITHUB_API_TOKEN", "")}\"",
        )

        // Single source of truth for the on-device Commit Milestone version
        // stamp (see ui/components/VersionLabel.kt). Distinct from
        // versionName/versionCode above, which track real Android app
        // releases, not this project's internal CM numbering. Bumping the
        // cmVersion val above is the ONLY per-CM maintenance required to
        // keep the on-device stamp accurate — no other file needs to change.
        buildConfigField("String", "CM_VERSION", "\"$cmVersion\"")

        // Which splash animation plays on launch. A build-time config
        // value, same pattern as the two fields below — not a runtime
        // user-facing setting. See MainActivity.kt for how this is read.
        buildConfigField("String", "SPLASH_ANIMATION", "\"$splashAnimation\"")

        // Which post-splash experience shows: "classic" (today's existing
        // tabs flow — completely untouched by this) or "repoFirst" (the
        // in-progress repo-first redesign, CM 1 as of this version). Same
        // build-time "branch by abstraction" pattern as the two fields
        // above — see MainActivity.kt.
        buildConfigField("String", "LANDING_EXPERIENCE", "\"$landingExperience\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.drafter)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
