# Two UI tests that raced background work

Two upstream tests check the screen before background work they depend on has finished, so they fail now and then on a busy machine. Each failed in a full `testStableDebugUnitTest` run while validating unrelated branches (11 November 2026), and passed on rerun. Test-only change; no app code changes.

## The races

- `EmojiPagesSurfacesTest.aCategoryTabTappedWhileSearchingShowsThatCategorysResults` waited until the typed emoji was saved to the recents, then looked for the Recents tab (`IllegalStateException: RECENTS`). The tab comes from a second step: after the save (`Dispatchers.IO`), the picker reloads the recents category on `Dispatchers.IO` again and only then rebuilds its tab row on the main thread. The wait now covers both: the emoji in storage and `RECENTS` among the tabs.
- `ClipboardHistoryTest.startupShowsLoadingAndBlocksHistoryActionsUntilReady` idled the main looper once and expected the stored clip on screen (`AssertionError` at its last assertion). The history list is a `ListAdapter`: once a list has been shown (the empty one while loading), the next one is diffed on a background thread before it is applied. The test now lays out again until the clip appears, with the same 10-second limit `EmojiPagesSurfacesTest` uses.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*EmojiPagesSurfacesTest' --tests '*ClipboardHistoryTest'
```

The failures were intermittent: each passed three times in a row on its own on upstream. They can't be forced in a unit test without changing app code, so this change can't be shown failing and then passing; it removes the assumption about timing.

## Validation

Run on 11 November 2026 against source commit `df56c7b1` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 185 classes, 1,417 tests, no failures, errors or skips, the same counts as upstream alone (the change adds no tests); both tests pass in it.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`.
