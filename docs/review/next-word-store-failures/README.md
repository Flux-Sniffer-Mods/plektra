# Next-word store failures

Learned next words live in SQLite (`UserNGramStore`). After every word, `SuggestionController` reads the next-word suggestions from it on the typing path, and forgetting a suggestion deletes from it there too. A database that can't be opened (storage full, the file locked or damaged) threw `SQLiteException` out of those calls, which ended the keyboard at the next space, and again after every restart. Learning was already safe: it runs on a queue that catches failures. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

When the database fails, the store logs it and answers with nothing: no learned next words, nothing deleted, nothing cleared. Typing goes on, and suggestions from other sources are unchanged. When the database works, nothing changes.

## Implementation boundaries

- One private helper in `UserNGramStore`, `guarded(fallback) { … }`, around `predict`, `delete`, `deleteNextWord` and `clearAll`, and around `learn` for callers without the queue. It catches `android.database.SQLException` (the parent of every `SQLiteException`) and `IllegalStateException` (a closed or recursively opened database), and nothing else.
- The schema, queries, transactions and the `UserNGramRepository` interface are unchanged.
- No preference, string or resource changes.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*UserNGramStoreFailureTest'
```

`UserNGramStoreFailureTest` puts a directory where the database file belongs, so SQLite can't open it, and checks that learning, predicting, deleting and clearing return their empty answers instead of throwing.

## Validation

Run on 11 November 2026 against source commit `b826f6b5` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 186 classes, 1,418 tests, no failures, errors or skips (upstream alone: 185 classes, 1,417 tests). New: 1 in `UserNGramStoreFailureTest`.
- The new test on upstream alone, without the fix, fails with `SQLiteCantOpenDatabaseException: Cannot open database`.
- An earlier full run on this commit had one failure elsewhere, `EmojiPagesSurfacesTest.aCategoryTabTappedWhileSearchingShowsThatCategorysResults`, a test that races the picker's background recents reload (`flux/test-races` fixes the wait). It passed three times on its own here and three times on upstream, and in the full run above.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass; the new class is in the `suggestions` topic.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`.

## Device check (still to do)

Not yet run on a device. The failure is hard to cause on purpose. One way, on a debug build: `adb shell run-as rocks.pkb.plektra sh -c 'rm -f databases/user_ngrams.db* && mkdir databases/user_ngrams.db'`, restart the keyboard and type a few sentences; it should keep working without learned next words. Remove the directory afterwards.
