# GitHubIssueFinder — Setup Docs (Draft)

> **Status: TBD / iterative.** This is a first-pass doc living under `/_meta` while it takes
> shape — not yet promoted into the top-level `README.md`. Content and structure here can
> change freely; nothing links to this file as a stable reference yet.

`/_meta` is a catch-all top-level folder for project metadata that isn't part of the app's
build/runtime — docs, planning material, and similar, kept separate from `app/` and the other
Gradle-managed directories.

## Running Tests

Local (JVM) unit tests — no emulator or physical device required:

```bash
./gradlew testDebugUnitTest
```

Run a single test class:

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.githubissuefinder.data.RepoIssueGroupMergeTest"
```

Test source lives under `app/src/test/java/` (plain JUnit4, no Mockito/Robolectric
dependency yet — see `RepoIssueGroupMergeTest.kt` for the existing pattern: functions under
test are kept as standalone, pure top-level functions rather than private methods on
`AndroidViewModel` subclasses specifically so they're testable this way, without needing a
real or mocked `Application`).

Instrumented tests (`app/src/androidTest/java/`) require a device or emulator and aren't
covered by the command above:

```bash
./gradlew connectedDebugAndroidTest
```
