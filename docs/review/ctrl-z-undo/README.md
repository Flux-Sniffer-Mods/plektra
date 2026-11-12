# Ctrl+Z undoes typing in any app

Many apps' fields (chats, web pages, notes) have no undo of their own. The keyboard now keeps a short history of the field as it is typed in, so Ctrl+Z undoes and Ctrl+Shift+Z or Ctrl+Y redoes. When the keyboard has nothing to undo, the shortcut goes to the app as before. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

- A step after each word (a space, a newline or `.,!?;:` was just typed) and after a pause of about a second; typing since the last step is a step of its own. Undo puts the field back a step, replacing only the part that differs, with the cursor where it was.
- Ctrl held, latched or tapped once; not Nav Mode's Ctrl. The bar's undo and redo buttons use the same history first, then the app's.
- Kept in memory only, for the last six fields and 60 steps each, for fields under 20,000 characters handed over whole; never for password fields or editors without a text type.

## Implementation boundaries

- `core/EditHistory` holds the steps per field (package, field id and input type) and applies undo and redo through the `InputConnection` in one batch edit. A step recorded just after an undo only moves the cursor, so redo survives it.
- `InputEventRouter.handleCtrlModifiedKey` tries the history before passing a held Ctrl combo to the app. `PhysicalKeyboardInputMethodService` starts the field's history in `onStartInput`, schedules steps on selection changes, and points the bar's undo and redo at the history.
- No preference, string or resource changes.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*EditHistoryTest'
```

`EditHistoryTest` (in `editing`) types into a whole field through a real `BaseInputConnection`: undo back two steps and nothing more (the app's turn), redo forward again, a step recorded after an undo keeping the redo, a password field keeping no history, and the diff that replaces only the changed middle.

## Validation

Run on 12 November 2026 against source commit `92d954a4` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 186 classes, 1,421 tests, no failures, errors or skips (upstream alone: 185 classes, 1,417 tests). New: 4 in `EditHistoryTest`.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass; the new class is in the `editing` topic.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`.

## Device check (still to do)

Not yet run on a device. Suggested check with a hardware keyboard, in an app without its own undo (a chat): type two sentences, press Ctrl+Z twice and Ctrl+Shift+Z once. In a password field, Ctrl+Z should reach the app.
