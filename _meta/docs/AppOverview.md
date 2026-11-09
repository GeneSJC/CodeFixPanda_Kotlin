# Code Fix Panda - App Overview

Feature and value report - prepared September 28, 2026 | Android project (Jetpack Compose, MVVM, Coroutines/Flow)

## What the App Is

Code Fix Panda is an Android app that helps developers discover real, currently-open GitHub issues labeled "good first issue" or "help wanted" - browse them, track them, and act on them, without leaving the app. It runs against a live connection to GitHub's Search API (via a personal access token), not sample or static data, so everything shown reflects the real, current state of GitHub.

The app runs a single, focused flow: pick a contribution mode, discover repos with matching open issues, drill into a repo's issues, and act on them - bookmark, hide, share, or open directly on GitHub.

## Core Discovery Flow

- **Issue Mode gate** - on first launch, the user makes one deliberate choice: "good first issue" or "help wanted." That choice colors the whole app (a distinct pastel theme per mode) and drives every search from that point on. It persists across launches, so a returning user skips straight to their results, and can switch modes later from the hamburger menu without losing their place.
- **Top Repos** - the main discovery screen. Repos are built directly from live matching issues, grouped by parent repo, so every repo shown is guaranteed to have at least one real, currently-open match - never a repo that turns out to have zero. Ranked by match count, with a small info icon explaining exactly how that ranking works.
- **Language filtering** - narrow results to Kotlin, Java, Swift, JavaScript, Python, or Any, via a fast, tappable chip row.
- **Pagination with cross-page de-duplication** - "Load More" pulls additional pages without ever showing the same repo twice, even when its matches span multiple pages of results.
- **Persistent, per-language/mode caching** - revisiting a combination already loaded is instant and free (zero network calls), with a clear "Updated X ago" label always showing whether what's on screen is fresh or cached.
- **Check for New Repos** - an explicit refresh that merges newly-found repos and issues into what's already loaded, rather than replacing it, with a snackbar summarizing what changed.
- **Clear Cache** - wipes cached search results (behind a confirmation dialog that makes clear bookmarks and hidden issues are unaffected) and reloads immediately.
- **Rate-limit awareness** - a persistent, tappable key-icon "remaining/limit" indicator in the title bar, backed by a detailed dial showing exactly how much of GitHub's Search API quota is left and when it resets - so a user is never caught off guard by GitHub's rate limit mid-session.

## Repo Issues Screen

Tapping into a repo opens its full list of matching issues - every issue already swept up while Top Repos was built, with zero additional network calls just to open it.

- **Refresh this repo** - one explicit action that fetches the complete current set for that repo (up to GitHub's own 100-result per-call maximum), closing the gap where a repo could otherwise only ever show whatever happened to be caught in Top Repos' own batch - including surfacing issues that have since closed, which is otherwise impossible to detect since GitHub's search only ever returns open issues.
- **Rich issue cards** - title, labels, comment count, relative update time, and a direct link to open the issue on GitHub.
- **Bookmark, Share, and Copy Link** - every card offers a one-tap bookmark toggle, the system share sheet, and a quick copy-link action.
- **Hide Issue** - a one-tap way to remove a single issue from view (with an Undo snackbar), for issues a user has already decided aren't a fit - a display-time filter only, never a deletion, applied consistently across both this screen and Top Repos' own counts and cards.

## Bookmarks and Hidden Issues

Two dedicated screens, reachable from the hamburger menu, both backed by their own persistent storage independent of the search cache - so a bookmarked or hidden issue survives Clear Cache untouched, and is never lost just because the search results that originally surfaced it are gone.

- **Bookmarks** - every currently-bookmarked issue, using the exact same card and actions as the repo screen; un-bookmarking removes it from the list immediately.
- **Hidden Issues** - every currently-hidden issue, newest-hidden-first, each with a relative "Hidden X ago" timestamp. Per-row Unhide, plus an "Unhide all" action behind a confirmation dialog.

## Polish and Reliability

- A hamburger menu organized into three clearly labeled, color-coded sections - Repo Management, Find Issues, and Sandbox (developer-only tooling, off by default) - so related actions are easy to scan rather than buried in one flat list.
- Clear, actionable error states: a dedicated explanation when no GitHub token is configured at all, and a distinct one when a configured token is present but being rejected by GitHub - each telling the user exactly what to do next, rather than one generic error covering both cases.
- A polished, branded launch animation, consistent regardless of which contribution mode is active.
- Correct system back-stack navigation throughout, including a fix for a rapid-double-tap-back race that could otherwise leave a blank screen.

## Technical Foundation

Built to demonstrate real Android engineering practice, not just a working demo:

- **Jetpack Compose** throughout, with a clean MVVM split between ViewModels and UI state.
- **Kotlin Coroutines/Flow** for all asynchronous work, including reactive state shared across screens (e.g. the rate-limit indicator and bookmark state updating live, anywhere they're shown, the instant they change).
- **Retrofit + Moshi** for a typed, real network layer against GitHub's live Search API.
- Three deliberately right-sized persistence mechanisms - SharedPreferences for a single primitive value, and a JSON file each for the search cache, bookmarks, and hidden issues - each matched to what it actually stores rather than one heavyweight mechanism used everywhere by default.
- A growing suite of JUnit4 unit tests covering the pure logic behind caching, merging, and filtering - extracted into standalone, Context-free functions specifically so they're testable without a real or mocked Android framework.
- A GitHub Actions CI workflow that runs the full test suite and a full compile on every push and pull request.

## Value to Developers Looking to Contribute

The app is aimed squarely at a real, common problem: finding a genuinely good first open-source issue to work on is harder than it should be. GitHub's own search is powerful but generic - it doesn't group results by repo, doesn't remember what you've already looked at, and gives no sense of how close you are to hitting its rate limit. Code Fix Panda closes those specific gaps:

- **Faster discovery** - results are grouped by repo and ranked by match count, so a developer sees which projects actually have multiple approachable issues right now, instead of scrolling a flat, unranked issue list.
- **Language-first browsing** - filtering by the language you actually know turns a generic "good first issue" search into a genuinely relevant shortlist.
- **Nothing gets lost** - bookmark an issue to come back to later, or hide the ones that aren't a fit, and both persist independently of whatever search results happen to be on screen at the time.
- **Confidence the data is current** - "Refresh this repo" and "Check for New Repos" give an explicit, honest way to know whether what's on screen is fresh, including surfacing issues that have since been closed by someone else - avoiding the frustration of showing up to claim an issue that's already taken.
- **No rate-limit surprises** - the persistent quota indicator means a developer actively searching across several repos and languages can see exactly how much room they have left, rather than hitting a throttled request with no warning.

## Current State

As of this report, the core feature set described above is complete and stable: every screen runs the live discovery-to-action flow end to end, error and edge-case states are handled deliberately rather than left to chance, and the codebase is backed by automated tests and continuous integration on every change.

---

This report reflects the app's state as of September 28, 2026. It is a snapshot, not a living document - re-verify against the real codebase before relying on it for anything beyond orientation.
