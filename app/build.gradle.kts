import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Reads GITHUB_API_TOKEN from local.properties (gitignored, never committed), or from a Gradle property of the
// same name (which scripts/check-release-has-no-token.sh uses), and exposes it as a BuildConfig field in
// DEBUG builds only, so a developer can run without typing a token. Release builds always get an
// empty string: a published APK can be unpacked by anyone, and every user would otherwise share that token.
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
// Build label "A.B": A counts feature sets and goes up by one when a new feature set starts; B counts the
// pushes that change this label within the feature set and restarts at 1 with each new A. Bump it in the
// SAME commit as the work it labels, and read the value back before committing: a scripted edit can
// silently fail.
val buildLabelA = 45
val buildLabelB = 28
// Ads are parked: the ads SDK and all code that depends on it were removed from this build (see
// docs/ads-restore.md, and the tag ads-parked-2026-10-27 that holds the working implementation).

/** A private property: a Gradle property (-P or the user-level gradle.properties), else local.properties. */
fun privateProperty(name: String): String? =
    providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty(name)?.takeIf { it.isNotBlank() }

val splashAnimation = "titleReveal" // "coin", "origami", "shapeStack", or "titleReveal"
val showDetailedDrawerMenu = false // true reveals the full animation-replay menu + version stamp (dev use); false shows just "Play App Start Animation"

android {
    namespace = "io.github.genesjc.opensourcepanda"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

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
        // above, not a product flavor: this is a developer aid, and a
        // flavor would be more than it needs. The flavors below exist
        // for a different reason (what each published build contains).
        buildConfigField("Boolean", "SHOW_DETAILED_DRAWER_MENU", "$showDetailedDrawerMenu")
    }

    // The project is published two ways from one codebase: foss is the
    // open-source build posted to GitHub, commercial is the build for
    // Google Play. A flavor (and not a runtime flag) is what keeps code
    // that only the commercial build may contain out of the open one at
    // compile time. The two ids are written out in full, not derived, because
    // a published Play id can never change. Both can be installed on one device.
    flavorDimensions += "distribution"
    productFlavors {
        create("foss") {
            dimension = "distribution"
            isDefault = true // a fresh checkout builds and runs the open build
            applicationId = "io.github.genesjc.opensourcepanda"
            versionNameSuffix = "-foss"
        }
        create("commercial") {
            dimension = "distribution"
            applicationId = "io.github.genesjc.opensourcepanda.commercial"
            versionNameSuffix = "-commercial"
        }
    }

    buildTypes {
        debug {
            // A developer's own token from local.properties, for debug builds only (see the note at the top).
            buildConfigField("String", "GITHUB_API_TOKEN", "\"${privateProperty("GITHUB_API_TOKEN") ?: ""}\"")
        }
        release {
            // Never the developer's token: a release build is published, and anything in it can be read by anyone.
            buildConfigField("String", "GITHUB_API_TOKEN", "\"\"")
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
    implementation(libs.koin.android)
    // The ads SDK is parked (docs/ads-restore.md): no build contains it. scripts/check-no-ads-sdk.sh checks that.
    testImplementation(libs.junit)
    // Lets ViewModel unit tests swap Dispatchers.Main (viewModelScope runs on it) for a test dispatcher.
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

// Prints the build label ("Build A.B", the same text the app shows in its
// drawer) as the last output of a build or test run, so the version can be
// read straight off the console without opening the app. A finalizer
// task, so it also prints when the build fails. Gradle's own "BUILD
// SUCCESSFUL" summary line comes after it; this is the last task output.
//
// The label is copied into a local String first: the task action must not
// reach back into this script's properties, which the configuration cache
// (enabled in gradle.properties) cannot store.
val printBuildLabel = tasks.register("printBuildLabel") {
    val label = "Build $buildLabelA.$buildLabelB"
    doLast {
        println()
        println(label)
    }
}

tasks.matching { it.name in setOf("assembleDebug", "assembleRelease", "installDebug", "testDebugUnitTest") }
    .configureEach { finalizedBy(printBuildLabel) }

// Android Studio's Run and Build use the selected variant's own tasks (for example
// installCommercialDebug), not the aggregate ones above, so each variant gets its own line naming
// the variant, e.g. "Build 34.2 (commercialDebug)". That is how to confirm which variant just ran.
for (flavor in listOf("Foss", "Commercial")) {
    for (type in listOf("Debug", "Release")) {
        val variant = "${flavor.replaceFirstChar { it.lowercase() }}$type"
        val line = "Build $buildLabelA.$buildLabelB ($variant)"
        val printVariantLabel = tasks.register("print${flavor}${type}Label") {
            doLast {
                println()
                println(line)
            }
        }
        val variantTasks = setOf(
            "assemble$flavor$type", "install$flavor$type", "bundle$flavor$type", "test${flavor}${type}UnitTest",
            // The task Android Studio's plain Build runs for the selected variant.
            "create${flavor}${type}ApkListingFile",
        )
        tasks.matching { it.name in variantTasks }.configureEach { finalizedBy(printVariantLabel) }
    }
}
