# A replacement undone once applies again

With auto-replace on, Backspace straight after a replacement puts the typed word back, and that word is then left alone so the next space doesn't replace it again. It was meant to be left alone until the next letter is typed. With the default long press, the letters of nearly every key never reached that code, so the word was never replaced again until the keyboard restarted: `i` stayed `i`, `im` stayed `im`. Reported against Flux Keyboard, which shares this code, as "text replacements work some of the time, then stop until a reboot"; found and fixed there, and ported onto upstream `68ace8ab` (10 November 2026).

## Behaviour

- Before: type `im`, space (`I'm `), Backspace (`im`). From then on `im` is not replaced, in any field, until the keyboard's process restarts. The same for a word auto-replace corrected.
- Also before: Backspace undid a replacement made earlier whenever the text before the cursor ended with it again. Type `im`, space, a letter, delete the letter, Backspace: `I'm` became `im` (and was then never replaced again).
- After: the undone word stays as typed for as long as it is the word at the cursor (space, delete the space, space again leaves it). Once a letter is typed or another word is finished, it is replaced again. Backspace only undoes a replacement straight after it.

## Cause

`SuggestionController.onCharacterCommitted` is where typing ends a replacement's undo and forgives an undone word. A key with a long-press character commits its letter in `AlternateCharacterManager.handleKeyWithAltMapping`, and with the default long press (the Alt character) `InputEventRouter` does not report it: the word tracker follows these letters by the cursor instead. Titan-style keyboards have an Alt character on nearly every letter, so in practice nothing reported typing at all.

## Implementation boundaries

- `InputEventRouter`: a character committed through the long-press path that is not tracked at once is reported with the new `SuggestionController.onTypingContinued`, on the key press, so it is in order with the keys around it. It only updates what the undo keeps (`clearLastReplacement`, and `clearRejectedWords` for a letter or digit); the word tracker is untouched, so suggestions are computed as before.
- `AutoReplaceController.handleBoundary`: a finished word that is not one of the undone ones ends the rejections. This does not depend on how the letters were typed (paste, a suggestion, the on-screen keyboard), so no path can leave a word unreplaced for good.
- No setting, string or stored data. With the suggestion engine off nothing changes: undo is not offered there.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*ReplacementAfterUndoTest' --tests '*ReplacementAfterUndoKeysTest'
```

`ReplacementAfterUndoKeysTest` (in `ime-integration`) types on the service with real key events, on keys given an Alt character: a replacement, Backspace, another word, then the first word again; and Backspace after a letter was typed and deleted. `ReplacementAfterUndoTest` (in `suggestions`) covers the controller: a finished word ending the rejection without any letter reported, the undone word left alone while it is still the word at the cursor, a letter on a long-press key forgiving it at once, and Backspace after more typing being an ordinary delete.

## Validation

VALIDATION_PLACEHOLDER

## Device check (still to do)

Not yet run on a device. Suggested check, with Text replacements and Auto-replace on: type `im` and a space, Backspace, a space, another word, then `im` and a space again: the last one should become `I'm`. Then type `im`, a space, a letter, delete the letter and press Backspace: it should delete the space, not turn `I'm` back into `im`.
