package io.github.genesjc.opensourcepanda.model

/**
 * Turns a repo slug ("github-readme-stats") into a readable title ("Github Readme Stats"). GitHub
 * has no display-title field, and reading one from the README would cost a request per repo and be
 * fragile. So this splits on hyphens and underscores and title-cases each word: a cheap,
 * deterministic approximation ("Github" not "GitHub"; "three.js" becomes "Three.js").
 */
fun humanizeRepoName(slug: String): String {
    return slug.split('-', '_')
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }
        }
}
