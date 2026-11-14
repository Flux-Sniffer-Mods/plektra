# Terminal mode

A switch in Settings → Modifiers: **Terminal mode** (off by default). In Termux and the other terminal apps, Alt and SYM type Plektra's symbols, and Ctrl stays Ctrl. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

**Stacked:** this branch carries `flux/terminal-no-microphone` (its feature commit), whose terminal app list it uses. Review and merge that first; this PR's own change is its last commit.

## Behaviour

- A terminal tells keyboards it has no text field, so Alt and SYM go to the terminal and Plektra's symbols don't work there. With terminal mode on, Plektra types into a terminal app's view like a text field without suggestions, corrections or automatic capitals (a visible-password field, what Termux's own "enforce-char-based-input" option asks for).
- Ctrl, held, tapped or latched, reaches the terminal as a real Ctrl (Ctrl+C, Ctrl+D), with Alt and Shift if they are held too, instead of Plektra's copy and cursor shortcuts.
- Enter, Backspace, Tab, Esc, the arrows, Home, End, Page Up and Down and F1 to F12 go to the terminal as they are, unless Alt or SYM is typing a symbol.
- Only the terminal apps in the list, and only their "no text field" views; their ordinary text fields, and every other app, as before.

## Implementation boundaries

- `inputmethod/TerminalMode` holds the switch (`terminal_mode_enabled`, registered in `BackupContract`) and rewrites a terminal's field type in place; `TerminalKeys` sends Ctrl combos and lets the terminal's keys through, each with its release.
- `PhysicalKeyboardInputMethodService`: `onStartInput` and `onStartInputView` apply it; `onKeyDown` sends Ctrl combos and terminal keys before Plektra's own handling; `onKeyUp` sends their releases.
- A row in the Modifiers screen and a settings-search link (`modifiers.terminal_mode`); two strings in their own file with all ten translations.
- Flux Keyboard's terminal extras (Nav Mode and swipes in terminals, the emoji key's terminal actions, the hidden keyboard and Termux's one-paste setup) are left out.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*TerminalModeTest'
```

`TerminalModeTest` (in `keys-modifiers`), on the keyboard service: off by default leaving Termux's view as it is, and backed up; on, Termux typed into as a text field and another app's view left alone; Ctrl+C sent to the terminal as a real Ctrl, press and release; and Enter going as it is.

## Validation

VALIDATION_PLACEHOLDER

## Device check (still to do)

Not yet run on a device. Suggested check in Termux with the switch on: Alt plus a key types its Alt symbol, SYM plus a key its symbol; run `sleep 100` and press Ctrl+C (it stops); Enter, Backspace and the arrows work as before; and no suggestions appear. Turn the switch off and check Termux behaves as it did.
