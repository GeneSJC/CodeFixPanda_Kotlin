# GitHubIssueFinder — Setup Docs (Draft)

## TO-DO

- [ ] **Flip this repo to public, then enforce CI on `develop`.** The repo is
  intentionally private today, but the actual intent is for it to be public
  for portfolio/demo access. GitHub's Free plan only enforces branch
  protection rules/rulesets (blocking a merge on red CI) on **public**
  repos — on a private repo it's paywalled behind GitHub Team ($4/user/
  month). Once public: GitHub Actions minutes are unlimited/free too (a
  private repo has a free-tier monthly cap, though CI usage here is well
  within it either way). Steps once ready to flip:
  1. Settings → General → Danger Zone → "Change repository visibility" →
     Public. Do a quick gut-check on commit history first for anything
     that shouldn't be public (the real GITHUB_API_TOKEN itself is safe —
     it only ever lives in `local.properties`, gitignored, never
     committed — but worth a last look regardless, since this is hard to
     fully undo: anyone who forks/clones while it's public keeps their
     copy even if the repo is flipped back to private later).
  2. The `develop protection` ruleset (Settings → Rules → Rulesets)
     already exists with `build-and-test` as a required status check —
     it should start actually enforcing automatically once the repo is
     public, no reconfiguration needed.
  3. Confirm by opening a throwaway PR with a deliberately broken commit
     and checking that GitHub actually blocks the merge button.

> **Status: TBD / iterative.** This is a first-pass doc living under `/_meta` while it takes
> shape — not yet promoted into the top-level `README.md`. Content and structure here can
> change freely; nothing links to this file as a stable reference yet.

`/_meta` is a catch-all top-level folder for project metadata that isn't part of the app's
build/runtime — docs, planning material, and similar, kept separate from `app/` and the other
Gradle-managed directories.

## Running Tests

Test source lives under `app/src/test/java/` (local/JVM, no emulator or device needed —
plain JUnit4, no Mockito/Robolectric dependency yet; see `RepoIssueGroupMergeTest.kt` for the
existing pattern: functions under test are kept as standalone, pure top-level functions
rather than private methods on `AndroidViewModel` subclasses specifically so they're testable
this way, without needing a real or mocked `Application`).

### IntelliJ / Android Studio

- **Single test method or class**: click the green gutter icon next to the `@Test`
  annotation (method) or the class declaration (whole class), or right-click the file in the
  Project view → **Run 'RepoIssueGroupMergeTest'**.
- **Whole package**: right-click a package under `app/src/test/java/` (e.g.
  `com.example.githubissuefinder.data`) → **Run Tests in '...'**.
- **Whole module**: right-click `app` (or `app/src/test/java`) → **Run All Tests**, or use
  the pre-configured **app** run configuration's dropdown if one exists.
- Results show in the **Run** panel with per-test pass/fail and a re-run-failed option — no
  need to drop to the terminal for routine local runs.

### Bash Shell

Local (JVM) unit tests — no emulator or physical device required:

```bash
./gradlew testDebugUnitTest
```

Run a single test class:

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.githubissuefinder.data.RepoIssueGroupMergeTest"
```

Instrumented tests (`app/src/androidTest/java/`) require a device or emulator and aren't
covered by the command above:

```bash
./gradlew connectedDebugAndroidTest
```
