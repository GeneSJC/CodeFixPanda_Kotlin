# Design notes

Decisions that are worth remembering but too long for a code comment. Code points here by section title.

## Top Repos screen

- **What it shows.** Repos as they fall out of grouping matching issues by parent repo, sorted by match count. Stars, forks, open-issue counts and "updated X ago" are not in an issue-search response, so cards show only the title, the match count and a README preview. That is a deliberate reduction, not a bug.
- **Mode is one whole-app choice.** It is made once at the gate screen. A mode dropdown in the title was tried and removed: it undercut the "you have to choose" framing and was not discoverable. The two "Find Good First Issues" / "Find Help Wanted Issues" menu items switch the mode in place, but sit at the bottom of a menu the user has to open deliberately. They are hardcoded; reusing the gate screen would scale better to a third mode but was not worth the navigation complexity for two. A third mode means editing those two items.
- **Hidden issues.** `uiState.repoGroups` stays the raw cached data; the hidden-filtered groups are derived on every recomposition and never written back. A repo whose issues are all hidden stays in the list with an "All hidden" badge, and leaves only when the cache is cleared. The repo screen gets the raw group so it can show hidden issues and bring them back.
- **Cache actions.** "Check for New Repos" merges fresh results into the list and does not replace it. "Clear Cache" wipes the whole cache (a known limitation, not an oversight), asks for confirmation, says that bookmarks and hidden issues are kept, and reloads the screen afterwards.
- **Load More is manual.** Auto-loading on scroll would spend Search API calls the user did not ask for, so the footer is an explicit tap.
- **Rate-limit chip always renders.** A cache hit never calls the network, so without a placeholder the chip could stay absent for a whole session and then pop in next to the mode's info icon. Numbers normally arrive with the first list (a live search, or a `/rate_limit` lookup after a cache hit). The icon is a fixed drawing; it replaced a key emoji that was too small to read on a phone.
- **Landscape split view.** A landscape phone is short on height, not width. The mode, repo count and language picker move into a narrow independently-scrolling left rail so the card list gets the full height. The language chips wrap in the rail because a horizontally scrolling row nested inside a vertically scrolling column would compete for gestures.
- **Menu groups.** Repo Management, Find Issues, then Sandbox (the dev-only animation replays, deliberately last). The build label is always visible in the menu header so the running build can be matched to the version line in a chat message.
- **README preview on cards.** A plain-text snippet, 2 lines when collapsed, expandable with no extra network call. It renders nothing while loading or when the repo has no README, because a spinner or a "no README" note would be clutter.

## Issue card

- **Actions.** Share opens the system share sheet (one `ACTION_SEND` call, which also covers email: Gmail is one entry in the sheet). Copy Link puts the URL on the clipboard and shows a Toast, which is self-contained: a Snackbar would force every screen that shows a card to supply a host. Bookmark does use a Snackbar, because that was the specific request, and so the caller must pass a `SnackbarHostState`. Having one action on a Toast and one on a Snackbar is a known inconsistency, kept on purpose.
- **Bookmark message.** "Bookmarked" or "Unbookmarked" is predicted from the current `isBookmarked`, which is safe because the in-memory effect of the toggle is synchronous. The disk write happens afterwards, off the UI thread.
- **Hide has no Snackbar here.** The card is about to be removed from the list, which would cancel a coroutine scope tied to it before an Undo could be tapped. The screen owns the hide Snackbar with a scope that outlives any one card.
- **Density.** The repo identity row is gone (the repo name is redundant on a per-repo screen). Labels always show, because a label such as "good first issue" identifies the issue. The actions are collapsed behind a chevron. `expanded` is local state; the screen keys cards by issue id, so it survives recomposition.
- **Chevron animation** is a 300 ms tween, not the README widget's 2 s: it fires on every card in a scrolling list, where a slow animation would feel sluggish. The top-right badge stack (external-link badge above the chevron, a 2 dp gap, a 7 dp top offset) is accepted as it is; it will not line up exactly with a two-line title.

## Repo issues screen

- **No repo description.** An issue-search response has no repo description, so the section that used to sit under the title is gone. That is a visible, deliberate reduction.
- **Refresh footer.** "Refresh this repo" stays reusable: after a refresh a result line appears above the button instead of replacing it. The empty-state message is an item inside the list, so the footer renders either way. It is only reachable after a refresh finds zero issues, since a repo's group is never empty on first load.
- **README is an overlay.** An earlier version gave the README `weight(1f)` in the column and skipped composing the list while it was open. That broke the expand animation, because `weight` forces the child's size on every frame, so the README jumped to full size and the list shifted. Now the list is always composed and full-size inside a Box, and the README is a second Box child with an opaque background, which can animate freely in both directions.
- **README fetch** uses GitHub's Contents API, a different API and quota from search, so it is deliberately not tracked by the rate-limit chip.
- **Header action** opens the repo's GitHub page. It keeps the diagonal glyph, unlike the card's external-link badge (a plain arrow), because it opens a different destination: the repo, not one issue.

## Navigation

- **The gate is a real destination.** An earlier version returned early outside the NavHost; Top Repos was then the only back-stack entry and pressing Back closed the app. That was found by pressing Back on a device.
- **Reset App** is the only way back to the gate once a mode is chosen. It clears the mode and navigates with `popUpTo(graph.id) { inclusive = true }`, wiping the whole back stack; a plain `navigate()` would add a second gate on top of the open screens. Bookmarks, hidden issues and the cache are untouched.
- **Selected repo group and language** are plain Compose state in the nav host, not passed through the route, to avoid serializing the model just to pass it along. Typed navigation will revisit this.
- **Hidden Issues** is reachable from the Top Repos menu and from the repo screen's "all hidden" state; both push it on top, so Back returns to where it was opened. A separate full-screen README destination was removed when the inline README widget replaced it.

## Top Repos state

- **One call per load.** The list is built from one Search API call for the active mode's label, grouped by parent repo (`repository_url`). A repo appears only if it had a matching issue in the batch, and the ranking is match count only. The old 60-second cooldown went away because one call per load no longer risks the 30-per-minute Search API cap.
- **Load More adds, never appends.** A repo's matches are not guaranteed to land on one page, so naively appending would show it as a second card. New issues are re-grouped against the repos already held, then re-sorted; a repo's rank can legitimately change.
- **Rate-limit event.** A Load More that returns 403 with `X-RateLimit-Remaining: 0` is the Search API rate limit, not a generic error. It emits one `RateLimited` event and leaves `canLoadMore` and `currentPage` untouched, so the button works again after the reset. Events are a `Channel` so each is delivered exactly once, not re-shown to every new collector.
- **Cache-first.** A hit fills the state with no network call, including `currentPage` and `canLoadMore`, so Load More resumes where a previous session stopped. Every successful fetch writes the accumulated groups back. Fresh data is explicit, through "Check for New Repos".
- **Check for New Repos replaced a refresh that lost data.** The old refresh went through `load()` and replaced the whole list with page 1: with 200 repos built from ten Load More taps, one refresh discarded nine taps' worth. The replacement fetches the same page 1 but updates the list in place, and never resets `currentPage`, or the data it exists to protect would be lost another way.
- **Why `syncFromCache`.** Top Repos and the repo screen have no handle on each other's ViewModel. The repo screen writes its refreshed group to the cache, and Top Repos re-reads the cache when it is navigated back to.

## Repo issues state

- **Why it makes no network call on open.** Every issue the screen can show is already in the repo's group; that is what it means for the repo to be on Top Repos at all.
- **Why there is a refresh at all.** The group holds only the matches swept up while Top Repos built its list, bounded by how far Load More had got, so a repo with more matches can look partial. Closed issues can only be discovered this way too, because issue search returns open issues only.
- **The refresh** is one call (`repo:owner/name label:"..." state:open`, up to 100 per page). It replaces the held issues outright, which is right here because the result is the complete live set for the repo, unlike Check for New Repos, which updates from a partial page.
- **Write-back.** A successful refresh updates the Top Repos cache entry the group came from, so the match count agrees on return. An empty result removes the repo from the cache: a repo cannot legitimately be on Top Repos with zero matching issues.
- **The language is passed down** from Top Repos. The refresh needs an exact `LanguageFilter` for the query's `language:` qualifier and for the cache key, and a repo's display language string is not reliably reversible to one.
- **Manual on purpose.** It is an explicit button with a visible cost note, not something that happens automatically.

## Data layer

- **Which storage.** SharedPreferences for single values (the Issue Mode, the token, the start-animation flag); a JSON file in private storage for lists of real objects (bookmarks, hidden issues, the Top Repos cache). Hand-rolling serialization for one enum value would be avoidable complexity. File names have been kept across refactors, so data saved earlier still loads.
- **Writes.** File writes run in order on a single background thread. An earlier version started a thread per write, so two quick changes could finish out of order and leave the older list on disk. A failed write is swallowed on purpose: it never rolls back what the user already saw.
- **Hiding is a display filter.** The hidden-issue repository stores only the ids to filter out; the cached data stays exactly as GitHub returned it, so Unhide is instant.
- **The Top Repos cache** is keyed by (language, mode) and is cache-first, never auto-refreshed. Saving and replacing are read-modify-write over one combined file, so the repository takes a mutex.

## README previews

- **In memory only.** A snippet is cheap to refetch and has no reason to survive process death.
- **Cost.** Each card's first appearance makes one Contents API call, a different and far larger quota (5,000 per hour authenticated) than the 30-per-minute Search API; after a few Load More taps there can be dozens of cards.
- **Retry only on a thrown exception.** An early version marked a repo "handled" the moment it was first requested. Two repos whose README loaded fine on the repo screen never got a preview, because a transient failure during the initial burst of calls left their cards permanently empty. Now only an exception is retried (on a later recomposition); any real HTTP answer, including a 404 for "no README", is final.

## Mode gate

- **A literal gate.** With no mode chosen the app shows this screen instead of a silent default; the mode is one deliberate choice made here.
- **Nested theme.** The screen's content sits in a theme with `issueMode = null`. Tapping a card sets the mode synchronously, so the single root theme recomposed every visible screen, including this one still fading out; the theme's crossfade covers only a handful of color roles, so the subheader and text color visibly snapped to the new mode's tone just before the transition. Pinning this subtree to the neutral scheme removes the snap.
- **Expanding cards.** "Good first issue" is not self-explanatory to someone outside open-source convention. Only the colored pill selects a mode; a separate chevron reveals the description, which is the same wording `LabelInfoDialog` uses, held in one place so the two cannot drift.

## Splash screens

- **Pinned palette.** The splash is the project's fixed brand identity, so every splash uses the Good First Issue (purple) palette and does not repaint under Help Wanted.
- **Not GitHub's logo.** The "GH" mark is the project's own: an independent, unaffiliated portfolio project.
- **Which one plays** is chosen in one place, `AppStartSplashScreen`, which both the cold start and the menu replay call, so a replay cannot drift from what a user saw. After the first launch a short version replaces the full animation: it fades the settled title in linearly from the first frame until the burst has finished (a short fade was not visible) and shows it on its own for about 1.35 seconds, then plays the same "sonic boom" burst and hold the full animation ends with, at the same 0.5x speed but with the burst 25% slower (1.25 s) and the hold trimmed by 150 ms. It is an independent copy of that ending (`TitleSettledSplashScreen`), so changes to it can never affect the first-launch animation.
- **Origami fold.** Every shape is drawn already oriented as a diamond so each fold is a single-axis rotation; two earlier attempts needed to compose two rotations on one layer, which could not be verified without seeing it render.
- **Shape stack** is a separate animation built to keep the snap-and-hold rhythm that emerged while tuning the folds. It is deliberately longer than the others.
