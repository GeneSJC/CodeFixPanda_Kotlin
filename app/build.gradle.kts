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
val buildLabelA = 34
val buildLabelB = 28
// ---- Advertising ids, and the production switch ----
// Safe by default: every build, release included, uses Google's published sample ids, so it serves only
// test ads and can never earn or be penalized. Live ads need a deliberate production build:
//     ./gradlew bundleCommercialRelease -PadmobProduction=true
// with all three real ids supplied as private properties (admobAppId, admobBannerUnitId, admobRewardedUnitId):
// in local.properties, in ~/.gradle/gradle.properties, or as -P options (CI secrets). Never in the repository's
// own gradle.properties, which is committed. A production build refuses to configure without all three, so
// test ads cannot be shipped as live ones by mistake. Debug builds ignore all of this and always use the sample ids. The foss build has no ads.
val admobSampleAppId = "ca-app-pub-3940256099942544~3347511713"
val admobSampleBannerUnitId = "ca-app-pub-3940256099942544/9214589741"
val admobSampleRewardedUnitId = "ca-app-pub-3940256099942544/5224354917"
val admobProduction = providers.gradleProperty("admobProduction").orNull == "true"

/** A private property: a Gradle property (-P or the user-level gradle.properties), else local.properties. */
fun privateProperty(name: String): String? =
    providers.gradleProperty(name).orNull?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty(name)?.takeIf { it.isNotBlank() }

if (admobProduction) {
    val missing = listOf("admobAppId", "admobBannerUnitId", "admobRewardedUnitId").filter { privateProperty(it) == null }
    if (missing.isNotEmpty()) {
        throw GradleException(
            "-PadmobProduction=true needs all three real AdMob ids as private properties (local.properties or Gradle); missing: $missing. " +
                "Leave the flag off to build with Google's test ids.",
        )
    }
}
val releaseAdmobAppId = if (admobProduction) privateProperty("admobAppId")!! else admobSampleAppId
val releaseAdmobBannerUnitId =
    if (admobProduction) privateProperty("admobBannerUnitId")!! else admobSampleBannerUnitId
val releaseAdmobRewardedUnitId =
    if (admobProduction) privateProperty("admobRewardedUnitId")!! else admobSampleRewardedUnitId

val splashAnimation = "titleReveal" // "coin", "origami", "shapeStack", or "titleReveal"
val showDetailedDrawerMenu = false // true reveals the full animation-replay menu + version stamp (dev use); false shows just "Play App Start Animation"

android {
    namespace = "io.github.genesjc.githubissuefinder"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
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
            applicationId = "io.github.genesjc.githubissuefinder"
            versionNameSuffix = "-foss"
        }
        create("commercial") {
            dimension = "distribution"
            applicationId = "io.github.genesjc.githubissuefinder.commercial"
            versionNameSuffix = "-commercial"
            // The banner ad unit for release builds: Google's sample unit unless this is a production build
            // (see the advertising block at the top). Debug builds ignore it and always use the sample unit.
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"$releaseAdmobBannerUnitId\"")
            // The same for the rewarded video behind the ad-free offer.
            buildConfigField("String", "ADMOB_REWARDED_UNIT_ID", "\"$releaseAdmobRewardedUnitId\"")
            // Your device's test id, which the ads SDK prints in the log, set as ADMOB_TEST_DEVICE_ID in
            // local.properties. In every build that is not a production build it is registered as a test device,
            // and in a debug build it also lets the consent form be seen outside the EU. Empty by default.
            buildConfigField(
                "String",
                "ADMOB_TEST_DEVICE_ID",
                "\"${localProperties.getProperty("ADMOB_TEST_DEVICE_ID", "")}\"",
            )
        }
    }

    // The AdMob app id the commercial build's manifest needs (src/commercial/AndroidManifest.xml): the sample
    // id except in a production build (see the advertising block at the top). The foss build ignores it: it has
    // no ads manifest entries.
    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = admobSampleAppId
        }
        release {
            manifestPlaceholders["admobAppId"] = releaseAdmobAppId
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
    // Only the commercial build contains the ads software; the foss build never does (see
    // scripts/check-foss-clean.sh). The string form is used because the typed accessor for a flavor's
    // configuration is not generated when the flavors are declared in this same script.
    add("commercialImplementation", libs.play.services.ads)
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
