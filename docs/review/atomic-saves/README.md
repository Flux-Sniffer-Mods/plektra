# Saves that are whole or not at all

Several saves of user data wrote over the file in place. A save cut short (a crash, the battery running out, a storage error) then left a truncated file: user words or a mapping that read back as empty or broken, or a first-run copy of the app's defaults that was never repaired, because the file existed. Ported from Flux Keyboard's stability work onto upstream `68ace8ab` (10 November 2026).

## Behaviour

These saves now write a temporary file beside the real one and swap it in. If anything fails, the previous version stays and the error is reported as before:

| File | Saved by |
| --- | --- |
| `ctrl_key_mappings.json` | Nav Mode mappings: the first copy from the app's defaults and both saves |
| `variations.json` | Variation rows: the three saves and Reset to default |
| `user_defaults.json` | Auto-correction's default user words: the first copy, the empty fallback and every edit |
| `locale_layout_mapping.json` | Custom input styles: the language-to-layout mapping and its one-off migration |
| `dictionaries_serialized/custom/*.dict` | Imported dictionaries |

An imported dictionary used to be read twice, once to check it and once to copy it; it is now read once, and the bytes that passed the check are the bytes saved.

## Implementation boundaries

- One helper file, `core/AtomicFiles.kt`: `File.writeBytesAtomically` writes `<name>.tmp` in the same folder, renames it over the file (atomic on the same file system) and deletes the temporary file whatever happens; `File.writeTextAtomically` writes UTF-8 text through it.
- Each site changes only its write call; the data written, the file names and the error handling around them are as before. The variation saves already wrote UTF-8; one of them used the platform default charset, which is UTF-8 on Android.
- A temporary file left by a process killed mid-save has the `.tmp` extension, which no folder listing reads (dictionaries are listed as `.dict`, backups as `.json` and `.xml`), and the next save replaces it.
- No preference, string or resource changes.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*AtomicFilesTest'
```

`AtomicFilesTest` (plain JVM, in a temporary folder) checks that a save replaces the file whole and leaves no temporary file, that a save which fails before the swap keeps the previous version and reports the failure, and that bytes are written exactly. A save cut short by a crash or power loss can't be reproduced in a unit test; the failing write stands in for it.

## Validation

Run on 11 November 2026 against source commit `93b617f0` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 186 classes, 1,420 tests, no failures, errors or skips (upstream alone: 185 classes, 1,417 tests). New: 3 in `AtomicFilesTest`. The existing tests that save Nav Mode mappings and save or reset variations pass unchanged; the dictionary import has no automated test.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass; the new class is in the `data-backup` topic.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`.

## Device check (still to do)

Not yet run on a device. Suggested check: change a Nav Mode mapping, a variation row and an auto-correction default word, reopen each screen and confirm the change is kept; import a `*_base.dict` dictionary and confirm it is listed and used. With adb, `run-as rocks.pkb.plektra ls files` should show no `.tmp` files afterwards.
