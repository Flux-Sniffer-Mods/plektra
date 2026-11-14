# The extra keys row

A command, **Extra keys**, in the quick launcher and assignable to a key: Esc, Tab, Ctrl, Alt and arrows in the keyboard bar's place, Termux style. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

**Stacked:** this branch carries `flux/terminal-no-microphone` (its feature commit), whose terminal app list chooses the row's set. Review and merge that first; this PR's own change is its last commit.

## Behaviour

- Terminals get Esc, Tab, Ctrl, Alt, -, / and the four arrows; other apps get the arrows, Home, End, select all, cut, copy and paste.
- Tap a key, or press the top-row letter under it while the row is open (Q the first, P the tenth; each key shows its letter). Arrows repeat while their letter is held.
- The row's Ctrl or Alt latches for the next key, whichever it is: Ctrl, then C, is Ctrl+C.
- Back or the row's ✕ closes it; it stays open in the same app's fields and closes in another app. Other keys type as usual.

## Implementation boundaries

- `inputmethod/extrakeys/ExtraKeys` (the keys and both sets, ported) and `ExtraKeysController` (open state, latching, the top-row letters) hold the logic; `ExtraKeysToggle` lets the command open it.
- `ui/ExtraKeysView` (ported, without Flux Keyboard's curved outer buttons) draws the row over the suggestion bar; `FullSuggestionsBar`, `StatusBarController` and `CandidatesBarController` pass it to the bar on screen, keep the bar up while it's open, and count it as content so the bar shows.
- `PhysicalKeyboardInputMethodService`: the row's key handling early in `onKeyDown` and `onKeyUp`, and its field check in `onStartInput`.
- `PlektraCommandSource` gains the command; `CommandExecutor` runs it. Three strings in their own file with all ten translations.
- Flux Keyboard's menu bar button, the emoji key opening it in terminals, and choosing the sets' keys are left out.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*ExtraKeysTest'
```

`ExtraKeysTest` (in `keys-modifiers`): the set for the app; keys sending keys, typing and editing; the top-row letters pressing keys, their releases, a held arrow repeating and other keys left alone; a latched Ctrl going with the next key; Back and another app closing it; and the keyboard opening it from its command.

## Validation

VALIDATION_PLACEHOLDER

## Device check (still to do)

Not yet run on a device. Suggested check: assign **Extra keys** to a key (or open it from the quick launcher) in a notes app: the row replaces the bar; Q moves the cursor left, O copies a selection. In Termux, open it and press Ctrl then C to stop a running command, and Esc in vim. Back closes it.
