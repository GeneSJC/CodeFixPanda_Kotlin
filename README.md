# GitHubIssueFinder

An Android app (Kotlin, Jetpack Compose) for finding GitHub issues worth working on:
the top repositories with open "good first issue" or "help wanted" issues, by language,
with the issues of each repository one tap away.

## Architecture

The standard Android stack, one direction only:

```
View (Compose screen)
  └─ ViewModel
       └─ StateHolder          (only where a feature has real state to manage)
            └─ Repository
                 └─ Data source (GitHub client, or local storage)
```

| Layer | Owns | Must not |
|---|---|---|
| View | Layout, local UI state (animations, dialogs, expanded flags) | Hold data state or call a repository |
| ViewModel | The state the screen shows, the wording, one-off events | Hold the feature's data logic, or know about files and network |
| StateHolder | A feature's data state and the logic that changes it | Contain user-facing wording, or call the network or disk directly |
| Repository | The source of truth for one kind of data (what is fetched, stored, cached) | Know about screens or wording |
| Data source | One external thing: the GitHub API, or a file | Decide what the app does with the data |

### Decisions and the terms used

- **Repository.** The Android term for the class that is the single source of truth for one
  kind of data, hiding where it comes from. Developers coming from Flutter will know this role
  as a *service* (the Flutter architecture guide calls the layer above it a repository too).
- **Data source.** The thin wrapper beneath a repository that talks to one external thing:
  `GitHubIssueSearchApi` and `GitHubReadmeApi` for the network, `JsonFileRepoIssueGroupStorage`
  for the cache file. Each sits behind an interface, so tests can swap it for a fake.
- **StateHolder.** The term Google's architecture guide uses for a class that owns state and
  the logic that changes it; a ViewModel is itself one. Here it is a collaborator of the
  ViewModel, not a layer in its own right: the ViewModel keeps what is specific to a screen
  (wording, one-off events, mapping to UI state) and delegates the feature's data state to a
  state holder. `TopReposStateHolder` and `RepoIssuesStateHolder` are the two.
- **Why a state holder outlives the screen.** A ViewModel is discarded when its screen leaves
  the back stack, and its coroutines are cancelled with it. The paged search spends API quota,
  so its state holder is an app-wide singleton running in `AppScope`: a load started on a
  screen finishes and caches even if the user has navigated away.
- **Facts versus wording.** State holders report what happened (`SearchFailure.Http(401)`,
  `TopReposEvent.RateLimited`); the ViewModels' `*UiMapping` files turn those facts into text.
  The state holders are tested without any UI.
- **Koin supplies the dependencies.** `di/AppModule.kt` is the one place that says how the
  shared objects are built, bottom layer first: repositories (with their data sources inside),
  then state holders. Those take what they need as constructor arguments. A ViewModel declares
  what it needs as plain members, `private val repository: X by inject()`, so it keeps a
  no-argument constructor and is created by the ordinary `viewModel()` factory. A test registers
  fakes with `restartKoin { single { fake } }` and then constructs the ViewModel. Concrete
  classes are registered, with no interface per class: an interface appears where there is a
  second implementation to choose.
- **Every top-level ViewModel extends `BaseViewModel`**, which carries the app-wide values and
  cross-cutting helpers any screen may need: the build configuration (an immutable `AppConfig`)
  and `string(...)` for resolving string resources. Logging would be another fit. A ViewModel
  that needs a repository or a state holder declares it as its own member. A guard test fails if
  a ViewModel does not extend it.
- **Every user-facing word is a string resource.** Fixed text is resolved in the composable with
  `stringResource`; text built from the data layer's facts is resolved in the ViewModel through
  `BaseViewModel.string`, and the data and model layers hold facts (timestamps, seconds, query
  values), never wording. Tests use a fake resolver that echoes the resource id and arguments.
- **No token means demo data.** The app runs without a GitHub token: the search, README and cache
  data sources are switched on every call between the real ones and demo ones (sixteen invented repos across the five languages,
  every title marked "[Demo]", a "Demo data" chip on the list screens), so the repositories and state holders
  above them never know which answered. Entering a token switches to real data at once. Saved
  bookmarks and hidden issues carry a tag (`Issue.tag`, null for real data), and each mode lists only
  the entries whose tag matches the data in use.
- **Navigation stays on string routes, by decision.** There are six routes, each a named
  constant, and none takes an argument; the selected repo is passed as host state, not through
  the route. Type-safe routes (Navigation Compose 2.8+) would need the Kotlin serialization
  plugin and a second serialization library, and would add nothing here, because the usual
  weakness of string routes is hand-built arguments, which this app has none of. Considered and
  decided against, to be revisited if a route ever needs arguments.
- **Local UI state stays in composables.** Animation values, dialog and menu toggles,
  expanded flags and snackbar hosts are not hoisted into ViewModels.

## Build flavors

One codebase, two published builds, as a `distribution` flavor dimension:

| Flavor | Published to | Application id | Version suffix |
|---|---|---|---|
| `foss` | GitHub, as the open-source build | `io.github.genesjc.githubissuefinder` | `-foss` |
| `commercial` | Google Play | `io.github.genesjc.githubissuefinder.commercial` | `-commercial` |

`foss` is the default, so a fresh checkout builds and runs the open build; pick `commercialDebug`
in Android Studio's Build Variants to run the other. The two ids differ, so both can be installed on
one device. The reason for flavors and not a runtime flag is that code only the commercial build may
contain stays out of the open build at compile time. The code
package is `io.github.genesjc.githubissuefinder` in both.

### Advertising (commercial flavor only)

The `foss` build has no ads, no ads library and no ads permission; CI checks this
(`scripts/check-foss-clean.sh`). The `commercial` build shows one banner at the bottom of the
Top Repos and repo issues screens, after Google's consent form where one is required.

- **Replaceable.** All ad software lives in `app/src/commercial` (the controller and the banner).
  The shared code only sees the small `AdsController` interface, so another ad provider, or none,
  means replacing those few files.
- **Test ads by default.** Every build, release included, uses Google's sample ids and shows only
  test ads. Live ads need a deliberate production build (`-PadmobProduction=true`) with the three real
  ids kept in `local.properties` (never committed). A live build says `LIVE ADS` in its version label.
- **Ad-free hour.** The commercial build offers, in the title bar, to hide the banner for one hour in
  exchange for watching one short rewarded video. It is opt-in and never asks anyone to tap an ad.
- **Rules for anyone testing** are in [`docs/ads-testing-rules.md`](docs/ads-testing-rules.md).

## License

Apache License 2.0; see [`LICENSE`](LICENSE). The Google Mobile Ads SDK that the commercial build
links is proprietary, is not covered by this license, and is not part of this repository.

## Building and testing

See `_meta/docs/README_01.md` for running the tests and the CI setup.

## Design notes

Decisions that are too long for a code comment are in [`docs/design-notes.md`](docs/design-notes.md).
