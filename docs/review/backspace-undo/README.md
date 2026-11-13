# Backspace undoes any replacement, keeping what was typed after it

Three changes to Backspace straight after a replacement. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

**Stacked:** this branch carries `flux/replacement-after-undo` (its feature commit), which it builds on. Review and merge that first; this PR's own change is its last commit.

## Behaviour

- **Text replacements can be undone with auto-replace off.** Text replacements (`i` → `I`, `im` → `I'm`) apply with auto-replace off, which is the default, and with the suggestion engine off, but Backspace undid them only with both on. Now Backspace straight after any replacement undoes it.
- **The space or punctuation typed after the word stays** (palsoftware/pastiera#316). Before: `im`, space, Backspace gave `im` and the space was gone too. Now it gives `im `, and typing carries on where it was.
- **Punctuation after the word doesn't stop the undo.** Before, only a space was looked past: after `im,` (replaced as `I'm,`), Backspace deleted the comma. Now it gives `im,`.
- Unchanged: only straight after the replacement; a second Backspace, or any typing first, is an ordinary delete. The word put back isn't replaced again until typing moves on.

## Implementation boundaries

- `InputEventRouter`: the Backspace undo is tried whatever the auto-replace and engine settings (each undo checks for itself that a replacement was just made).
- `SuggestionController`: `handleBackspaceUndo` without the engine check; `onCharacterCommitted` updates what the undo keeps (`onTypingContinued`) before the engine check, so with the engine off typing still ends the undo.
- `AutoReplaceController.handleBackspaceUndo`: no auto-replace check; the replaced word is matched with a space or punctuation after it, and that is committed back after the original word.
- No setting, string or stored data. `SuggestionControllerReplacementReadinessTest` and the two `ReplacementAfterUndo` tests now expect the space to stay.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*BackspaceUndoTest' --tests '*ReplacementAfterUndo*' --tests '*SuggestionControllerReplacementReadinessTest'
```

`BackspaceUndoTest` (in `suggestions`): with auto-replace off and the engine off or on, a text replacement undone with the space kept and a second Backspace left alone; a comma kept; typing ending the undo with the engine off. `ReplacementAfterUndoKeysTest` adds the same with real key events and default settings.

## Validation

VALIDATION_PLACEHOLDER

## Device check (still to do)

Not yet run on a device, with default settings (text replacements on, auto-replace off). Suggested check: type `im` and a space (`I'm `), press Backspace: `im ` with the cursor after the space. Type `im,` (`I'm,`) and Backspace: `im,`. Type `im`, a space, a letter, delete the letter and press Backspace: the space is deleted, `I'm` stays.
