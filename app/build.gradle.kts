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
//
// Build label (Project Conventions doc, section 6): "A.B" — A is the
// number of the pull request this work will land in; B is a running
// push counter that goes up by one on every push that changes this
// label and is NEVER reset when A changes (B alone orders any two
// builds; A just says which PR each belongs to). Bump this in the SAME
// commit as the work it labels, and read the value back before
// committing — a scripted edit can silently fail. Supersedes the old
// "cmVersion"/CM_VERSION stamp (last value "v0.48", already stale and
// never actually wired into any screen) with the scheme in that
// convention doc. Started fresh at 13.1 (PR #13, the open PR this
// convention was adopted on) — there is no prior A.B history to
// continue from.
//
// A went stale for several pushes (stayed at 13 through PR #14's merge
// and five commits into PR #15, which is what's actually open now) —
// a real lapse in the per-push bump this comment calls the ONLY
// required maintenance, caught directly by on-device observation
// ("I'm still seeing 13.1"). Corrected here to 15 (PR #15, the
// currently-open PR). B continues climbing from its last value (per
// above, it never resets on an A change) rather than restarting at 1.
val buildLabelA = 20
val buildLabelB = 14
val splashAnimation = "titleReveal" // "coin", "origami", "shapeStack", or "titleReveal"
val showDetailedDrawerMenu = false // true reveals the full animation-replay menu + version stamp (dev use); false shows just "Play App Start Animation"

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

        // Single source of truth for the on-device build label (see
        // ui/components/VersionLabel.kt and TopReposScreen's drawer
        // header). Distinct from versionName/versionCode above, which
        // track real Android app releases, not this project's own
        // per-push build identifier. Bumping buildLabelA/buildLabelB
        // above is the ONLY maintenance required to keep the on-device
        // stamp accurate — no other file needs to change.
        buildConfigField("String", "BUILD_LABEL", "\"$buildLabelA.$buildLabelB\"")

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

        // Whether Discover's hamburger menu shows its full animation-
        // replay toolkit (every individual splash, plus the on-device CM
        // version stamp — useful while actively iterating on splash/
        // animation work) or just a single "Play App Start Animation"
        // entry, what an actual end user would see. Defaults to false so
        // a fresh clone/run shows the clean, production-like menu rather
        // than this project's own dev tooling — see TopReposScreen.kt.
        // Deliberately a plain boolean val, same pattern as the field
        // above, not a Gradle product flavor: this is the only
        // thing it currently controls, so a flavor's build-variant
        // machinery (separate installable variants, an application-id
        // suffix, etc.) would be more than this needs.
        buildConfigField("Boolean", "SHOW_DETAILED_DRAWER_MENU", "$showDetailedDrawerMenu")
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
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.markwon.core)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
