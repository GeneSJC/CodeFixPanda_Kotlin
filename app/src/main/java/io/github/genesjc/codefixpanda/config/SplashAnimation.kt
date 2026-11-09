package io.github.genesjc.codefixpanda.config

/** The start animations a build can choose (build.gradle.kts, SPLASH_ANIMATION). */
enum class SplashAnimation(val configName: String) {
    COIN("coin"),
    ORIGAMI("origami"),
    SHAPE_STACK("shapeStack"),
    TITLE_REVEAL("titleReveal"),
    ;

    companion object {
        /** The animation the build setting [name] stands for. An unknown name fails at start-up, so a typo is found at once. */
        fun fromConfigName(name: String): SplashAnimation =
            entries.firstOrNull { it.configName == name }
                ?: error("Unknown splash animation \"$name\" in the build settings; expected one of ${entries.joinToString { it.configName }}")
    }
}
