# Select from the cursor

A new command, **Select from the cursor**, for an assigned key or a Nav Mode key: Nav Mode turns on and every move extends the selection from where the cursor was. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

- Nav Mode's arrows, Home, End, Page Up and Page Down are sent with Shift, so they select; other Nav Mode keys (Tab, Enter, Esc) are sent as they are.
- Cursor moves through `TextSelectionHelper` (cursor swipes, the trackpad) extend the selection the same way.
- Leaving Nav Mode, however it ends (its key, a Ctrl tap, another field), ends the selecting; the selection itself stays.

## Implementation boundaries

- `core/SelectFromCursor` is set by `NavModeController.startSelectingFromCursor()`, which turns Nav Mode on (as `enterNavMode` does, without needing it enabled in settings). It reads as active only while Nav Mode still is, so every way Nav Mode ends (there are many) ends it without hooks in each. `sendMappedKey` adds Shift for the cursor keys while it is set; `moveCursorLeft/Right` become `expandSelectionLeft/Right`.
- The command is a `PlektraCommandSource` target (`plektra.select_from_cursor`, surfaces: assigned key and Nav Mode) run by `CommandExecutor`. Two strings in their own file with all ten translations. No preference.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*SelectFromCursorTest'
```

`SelectFromCursorTest` (in the `keys-modifiers` topic): starting turns Nav Mode on and its arrow mappings carry Shift while Tab does not; `exitNavMode` ends it, and so does the Nav Mode latch being cleared another way; and a cursor move with a collapsed selection at 5 selects 4–5.

## Validation

Run on 11 November 2026 against source commit `2a70595d` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 186 classes, 1,421 tests, no failures, errors or skips (upstream alone: 185 classes, 1,417 tests). New: 4 in `SelectFromCursorTest`.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass; the new class is in the `keys-modifiers` topic.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`; the new strings have all ten translations.

## Device check (still to do)

Not yet run on a device. Suggested check: assign Select from the cursor to a key (Keyboard shortcuts), put the cursor in a sentence, press it, then use Nav Mode's arrows and Home/End: the text should be selected from the starting point. Leave Nav Mode and confirm arrows move without selecting again.
