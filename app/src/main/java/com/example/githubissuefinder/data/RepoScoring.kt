package com.example.githubissuefinder.data

import com.example.githubissuefinder.model.Repo
import com.example.githubissuefinder.network.dto.RepoDto
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Below this age, starsPerDay would be noise, not signal — see toRepo(). */
private const val MIN_AGE_DAYS_FOR_VELOCITY = 30f

/**
 * Composite "interesting repo" score, from the earlier design discussion.
 * Every term traces to a real, visible GitHub field — no hidden/black-box
 * inputs:
 *
 *   score = normalize(stars)          * 0.4
 *         + normalize(forks)          * 0.3   <- "actively cloned for use"
 *         + recencyBonus              * 0.2
 *         + (1 - normalize(openIssues)) * 0.1
 *
 * Normalization is relative to the current candidate batch (divide by
 * that batch's own max value), not an absolute external scale — keeps
 * scoring meaningful regardless of which query produced the candidates,
 * and avoids needing an arbitrary "what counts as a lot of stars"
 * assumption.
 *
 * starsPerDay (v0.37) is computed separately from this score and does
 * NOT currently feed into it — it's a display-only "how fast is this
 * catching on right now" signal, not (yet) a ranking factor. Folding it
 * into the score itself is a reasonable future step once this is seen
 * in practice.
 *
 * displayTitle (v0.39) uses humanizeRepoName() (RepoNameFormatting.kt,
 * same package) — a deterministic slug-to-title approximation, not an
 * attempt at fetching/parsing each repo's actual README heading.
 */
fun scoreAndRankRepos(candidates: List<RepoDto>): List<Repo> {
    if (candidates.isEmpty()) return emptyList()

    val maxStars = candidates.maxOf { it.stargazersCount }.coerceAtLeast(1)
    val maxForks = candidates.maxOf { it.forksCount }.coerceAtLeast(1)
    val maxOpenIssues = candidates.maxOf { it.openIssuesCount }.coerceAtLeast(1)

    return candidates.map { dto ->
        val starScore = dto.stargazersCount.toFloat() / maxStars
        val forkScore = dto.forksCount.toFloat() / maxForks
        val recency = recencyBonus(dto.updatedAt)
        val issuesPenaltyTerm = 1f - (dto.openIssuesCount.toFloat() / maxOpenIssues)
        val score = starScore * 0.4f + forkScore * 0.3f + recency * 0.2f + issuesPenaltyTerm * 0.1f
        dto.toRepo(score = score)
    }.sortedByDescending { it.score }
}

fun RepoDto.toRepo(score: Float): Repo = Repo(
    id = id,
    fullName = fullName,
    ownerLogin = owner.login,
    name = name,
    description = description?.takeIf { it.isNotBlank() } ?: "No description provided.",
    stars = stargazersCount,
    forks = forksCount,
    openIssuesCount = openIssuesCount,
    language = language,
    updatedAtLabel = formatRelativeTime(updatedAt),
    htmlUrl = htmlUrl,
    score = score,
    displayTitle = humanizeRepoName(name),
    starsPerDay = calculateStarsPerDay(stargazersCount, createdAt),
)

/**
 * 1.0 if updated very recently, decaying linearly to 0 by one year old.
 * Deliberately simple/explainable rather than a more elaborate curve.
 */
private fun recencyBonus(isoTimestamp: String): Float {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val thenMillis = format.parse(isoTimestamp)?.time ?: return 0f
        val daysAgo = (System.currentTimeMillis() - thenMillis) / (1000f * 60 * 60 * 24)
        (1f - daysAgo / 365f).coerceIn(0f, 1f)
    } catch (e: Exception) {
        0f
    }
}

/**
 * stars / days-since-creation, an honest "how fast is this actually
 * catching on" number — no hidden weighting, just the real arithmetic.
 * Returns null for repos younger than MIN_AGE_DAYS_FOR_VELOCITY, where
 * a handful of launch-day stars would look "explosive" without being a
 * real signal yet — null means "too new to gauge," never a stand-in
 * for zero.
 */
private fun calculateStarsPerDay(stars: Int, createdAtIso: String): Float? {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val createdMillis = format.parse(createdAtIso)?.time ?: return null
        val daysSinceCreation = (System.currentTimeMillis() - createdMillis) / (1000f * 60 * 60 * 24)
        if (daysSinceCreation < MIN_AGE_DAYS_FOR_VELOCITY) return null
        stars / daysSinceCreation
    } catch (e: Exception) {
        null
    }
}
