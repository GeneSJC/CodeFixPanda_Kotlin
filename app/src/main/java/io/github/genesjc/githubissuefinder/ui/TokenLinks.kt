package io.github.genesjc.githubissuefinder.ui

/**
 * GitHub's page for creating a personal access token, with the token's name pre-filled. If the user is
 * not signed in, GitHub asks them to sign in and then continues to this page. The token needs no
 * permissions, because the app only reads public information.
 */
const val NEW_TOKEN_URL = "https://github.com/settings/tokens/new?description=GitHub%20Issue%20Finder"
