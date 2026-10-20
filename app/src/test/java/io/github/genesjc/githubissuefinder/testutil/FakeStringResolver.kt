package io.github.genesjc.githubissuefinder.testutil

import io.github.genesjc.githubissuefinder.ui.StringResolver

/**
 * A resolver for JVM tests, where string resources cannot be read: it echoes the resource id and
 * arguments, so a test builds its expected text with the same call and checks which resource and
 * arguments the code under test chose:
 *
 *     assertEquals(FakeStringResolver(R.string.error_http, 500), failure.toMessage(FakeStringResolver))
 */
object FakeStringResolver : StringResolver {
    override fun invoke(id: Int, vararg args: Any): String = "string:$id(${args.joinToString()})"

    override fun plural(id: Int, count: Int, vararg args: Any): String =
        "plural:$id#$count(${args.joinToString()})"
}
