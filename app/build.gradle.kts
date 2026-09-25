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
val cmVersion = "v0.48"
val splashAnimation = "titleReveal" // "coin", "origami", "shapeStack", or "titleReveal"
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

        // Which splash animation plays on launch — "titleReveal" (the
        // kinetic word-by-word title reveal, see
        // TitleRevealSplashScreen.kt) is the default for now while it's
        // the active focus of iteration; "origami" (the diamond-fold
        // sequence), "shapeStack" (a stop-motion layered shapes
        // animation, v0.33), and "coin" are all fully available
        // alternatives, not variants of one another. A build-time config
        // value, same pattern as the two fields below — not a runtime
        // user-facing setting. See MainActivity.kt for how this is read.
        buildConfigField("String", "SPLASH_ANIMATION", "\"$splashAnimation\"")

        // Which post-splash experience shows: "repoFirst" (the Discover
        // landing screen — now the default as of v0.28) or "classic" (the
        // original tabs flow, still fully intact and reachable by setting
        // this back). Same
        // build-time "branch by abstraction" pattern as the two fields
        // above — see MainActivity.kt.
        buildConfigField("String", "LANDING_EXPERIENCE", "\"$landingExperience\"")
    }

    // "mode" flavor dimension: "developer" keeps the Discover hamburger
    // menu's full animation-replay toolkit and on-device CM version
    // stamp (useful for exactly the kind of iteration this project does
    // — see the earlier splash/animation CMs); "production" strips
    // those down to a single "Play App Start Animation" entry, what an
    // actual end user would see on a real release. Read at runtime via
    // the auto-generated BuildConfig.FLAVOR — see TopReposScreen.kt.
    // applicationIdSuffix on "developer" lets both variants install
    // side by side on the same device.
    flavorDimensions += "mode"
    productFlavors {
        create("developer") {
            dimension = "mode"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
        create("production") {
            dimension = "mode"
        }
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
