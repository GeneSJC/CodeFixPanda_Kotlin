# GitHubIssueFinder — Setup Docs (Draft)

> **Status: TBD / iterative.** This is a first-pass doc living under `/_meta` while it takes
> shape — not yet promoted into the top-level `README.md`. Content and structure here can
> change freely; nothing links to this file as a stable reference yet.

`/_meta` is a catch-all top-level folder for project metadata that isn't part of the app's
build/runtime — docs, planning material, and similar, kept separate from `app/` and the other
Gradle-managed directories.

## Running Tests

Test source lives under `app/src/test/java/` (local/JVM, no emulator or device needed —
plain JUnit4, no Mockito/Robolectric dependency yet; see `RepoIssueGroupingTest.kt` for the
existing pattern: functions under test are kept as standalone, pure top-level functions
rather than private methods on `AndroidViewModel` subclasses specifically so they're testable
this way, without needing a real or mocked `Application`).

### IntelliJ / Android Studio

- **Single test method or class**: click the green gutter icon next to the `@Test`
  annotation (method) or the class declaration (whole class), or right-click the file in the
  Project view → **Run 'RepoIssueGroupingTest'**.
- **Whole package**: right-click a package under `app/src/test/java/` (e.g.
  `io.github.genesjc.githubissuefinder.data`) → **Run Tests in '...'**.
- **Whole module**: right-click `app` (or `app/src/test/java`) → **Run All Tests**, or use
  the pre-configured **app** run configuration's dropdown if one exists.
- Results show in the **Run** panel with per-test pass/fail and a re-run-failed option — no
  need to drop to the terminal for routine local runs.

### Bash Shell

Local (JVM) unit tests — no emulator or physical device required. The project has two build
flavors (`foss` and `commercial`). The bare name `testDebugUnitTest` is ambiguous to Gradle with two
flavors, so name the flavors:

```bash
./gradlew testFossDebugUnitTest testCommercialDebugUnitTest
```

One flavor only: `./gradlew testFossDebugUnitTest`

Run a single test class:

```bash
./gradlew :app:testFossDebugUnitTest --tests "io.github.genesjc.githubissuefinder.data.RepoIssueGroupingTest"
```

Instrumented tests (`app/src/androidTest/java/`) require a device or emulator and aren't
covered by the command above:

```bash
./gradlew connectedDebugAndroidTest
```

## Continuous Integration

`.github/workflows/ci.yml` runs the unit tests and the debug build for both flavors on every push and
pull request against `develop`/`main`, on GitHub's own runners (which have normal internet
access, unlike the Claude Code sandbox's blocked Gradle toolchain download). It currently
just reports status on the PR — it is NOT yet a required check in branch protection, so it
doesn't block merging. Test result reports (one folder per flavor) are uploaded as a workflow artifact
(`unit-test-results`) for a few weeks after each run.
