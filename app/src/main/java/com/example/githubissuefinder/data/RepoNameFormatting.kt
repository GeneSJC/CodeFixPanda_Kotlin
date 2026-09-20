package com.example.githubissuefinder.data

/**
 * Turns a repo slug ("github-readme-stats") into a readable display
 * title ("Github Readme Stats"). GitHub's API has no separate "display
 * title" field — repos that look like they have one (e.g. "GitHub
 * Readme Stats" as a big heading) are just rendering their own
 * README.md's first line; that's README CONTENT, not repo metadata,
 * and reading it reliably would mean fetching and parsing arbitrary
 * markdown per repo (a real extra API call, and fragile — plenty of
 * READMEs open with a badge row or a logo image, not a clean heading).
 *
 * This is the cheap, deterministic alternative: split on hyphens and
 * underscores, title-case each word. Deliberately an approximation,
 * not a perfect match — "Github" rather than the brand's own "GitHub"
 * capitalization, and something like "three.js" only gets its first
 * letter capitalized ("Three.js") since the dot isn't a split point.
 * Reliable and API-cost-free, which real README-title extraction is
 * not — that idea is filed separately, not attempted here.
 */
fun humanizeRepoName(slug: String): String {
    return slug.split('-', '_')
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }
        }
}
