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
