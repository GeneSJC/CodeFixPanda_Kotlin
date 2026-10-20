package io.github.genesjc.githubissuefinder.data

import io.github.genesjc.githubissuefinder.network.dto.ReadmeDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadmeRepositoryTest {

    private fun body(content: String? = "encoded", encoding: String? = "base64") =
        ReadmeDto(name = "README.md", content = content, encoding = encoding, htmlUrl = null)

    /** A fake API that returns one canned response (or throws) and counts calls. */
    private class FakeApi(private val answer: () -> ReadmeApiResponse) : ReadmeApi {
        var calls = 0
        override suspend fun getReadme(repoOwner: String, repoName: String): ReadmeApiResponse {
            calls++
            return answer()
        }
    }

    private fun repository(
        api: ReadmeApi,
        decode: (String?, String?) -> String? = { content, _ -> content?.let { "decoded:$it" } },
    ) = ReadmeRepository(api, decode)

    private fun fetch(repository: ReadmeRepository) = runBlocking { repository.fetch("octo", "repo") }

    @Test
    fun aSuccessfulResponseIsDecodedIntoFound() {
        val result = fetch(repository(FakeApi { ReadmeApiResponse(200, true, body("abc")) }))
        assertEquals(ReadmeResult.Found("decoded:abc"), result)
    }

    @Test
    fun aFourOhFourIsNotFoundNotAnError() {
        val result = fetch(repository(FakeApi { ReadmeApiResponse(404, false, null) }))
        assertEquals(ReadmeResult.NotFound, result)
    }

    @Test
    fun aFourOhFourIsNotFoundEvenIfABodyWasSent() {
        val result = fetch(repository(FakeApi { ReadmeApiResponse(404, false, body()) }))
        assertEquals(ReadmeResult.NotFound, result)
    }

    @Test
    fun anErrorStatusIsAnHttpErrorWithItsCode() {
        val result = fetch(repository(FakeApi { ReadmeApiResponse(500, false, null) }))
        assertEquals(ReadmeResult.HttpError(500), result)
    }

    @Test
    fun aSuccessfulResponseWithNoBodyIsAnHttpError() {
        val result = fetch(repository(FakeApi { ReadmeApiResponse(200, true, null) }))
        assertEquals(ReadmeResult.HttpError(200), result)
    }

    @Test
    fun aBodyThatCannotBeDecodedIsUnreadable() {
        val result = fetch(repository(FakeApi { ReadmeApiResponse(200, true, body()) }, decode = { _, _ -> null }))
        assertEquals(ReadmeResult.Unreadable, result)
    }

    @Test
    fun theDecoderIsGivenTheBodysContentAndEncoding() {
        var seen: Pair<String?, String?>? = null
        val api = FakeApi { ReadmeApiResponse(200, true, body("payload", "base64")) }

        fetch(repository(api, decode = { content, encoding -> seen = content to encoding; "x" }))

        assertEquals("payload" to "base64", seen)
    }

    @Test
    fun aRequestThatThrowsIsFailedWithItsMessage() {
        val result = fetch(repository(FakeApi { throw java.io.IOException("timed out") }))
        assertEquals(ReadmeResult.Failed("timed out"), result)
    }

    @Test
    fun aFailureWithNoMessageIsStillFailed() {
        val result = fetch(repository(FakeApi { throw IllegalStateException() }))
        assertTrue(result is ReadmeResult.Failed)
    }
}
