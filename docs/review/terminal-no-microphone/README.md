# No microphone button in terminal apps

In a terminal or SSH app (Termux, ConnectBot, JuiceSSH, Termius and others), the keyboard bar and its menu leave the microphone button out: there's no use for dictation in a shell. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

- Only the microphone button, only while a terminal app's field is in front; every other button, and the microphone everywhere else, as before.
- Speech shortcuts (Alt + Ctrl, say) are unchanged.

## Implementation boundaries

- `inputmethod/TerminalApps` holds the terminal packages and whether one is in front; `PhysicalKeyboardInputMethodService.onStartInput` updates it.
- `StatusBarButtonRegistry` (the bar's buttons, full and slim) and `HamburgerMenuView` (the menu) leave the microphone out while it is.
- No setting, string, permission or migration. Flux Keyboard reads this from its terminal mode's app list; Plektra has no terminal mode, so the list is the known terminal apps.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*TerminalAppsTest'
```

`TerminalAppsTest` (in `ime-integration`): the bar's buttons with and without a terminal in front, other buttons kept; and the keyboard service noting a terminal as its field starts.

## Validation

Run on 14 November 2026 against source commit `01631502` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 186 classes, 1,419 tests, no failures, errors or skips (upstream alone: 185 classes, 1,417 tests). Of these, 2 are new, in `TerminalAppsTest`.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass; the new class is in the `ime-integration` topic.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`.

## Device check (still to do)

Not yet run on a device. Suggested check with the microphone on the keyboard bar: open Termux (no microphone button, in the bar or the menu), then a messaging app (the microphone is back).
