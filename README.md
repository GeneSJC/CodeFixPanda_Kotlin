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
- **Every top-level ViewModel extends `BaseViewModel`**, which carries the app-level values any
  screen may need (today the build configuration, an immutable `AppConfig`). It holds values
  only: a ViewModel that needs a repository or a state holder declares it as its own member.
  A guard test fails if a ViewModel does not extend it.
- **Local UI state stays in composables.** Animation values, dialog and menu toggles,
  expanded flags and snackbar hosts are not hoisted into ViewModels.

## Building and testing

See `_meta/docs/README_01.md` for running the tests and the CI setup.
