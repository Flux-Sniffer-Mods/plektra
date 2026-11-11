# Ctrl + Shift + D adds the last word, and the "add to dictionary" chip can be switched off

Two dictionary controls in Auto-correction. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

- **Ctrl + Shift + D adds the last word** (on by default): the word before the cursor goes into the user dictionary, so it's suggested and never auto-corrected; a short message says it was added, was already known, or that there was no word. Without a word before the cursor, the unknown word the suggestions offered to add is used. Works with Ctrl held, latched or one-shot, and either Shift.
- **Offer to add new words** (on by default): off, the suggestions no longer show the "add to dictionary" chip for an unknown word; words can still be added with the shortcut, the trackpad gesture or the dictionary settings.
- The word: letters and digits with an inner apostrophe or hyphen (`can't`, `well-known`), without quotes or dashes around it; numbers alone don't count.

## Implementation boundaries

- `PhysicalKeyboardInputMethodService`: the shortcut next to the other modifier shortcuts (before Ctrl+Space), clearing Ctrl and Shift as they do; `addLastWordToDictionary()` uses `SuggestionController.addUserWord` and `isKnownWordInActiveDictionaries`; the top-level `lastWordIn()` finds the word. The bar's chip candidate (`pendingAddWord`) is dropped when the switch is off; the trackpad add-word gesture is unchanged.
- New: two preferences (`show_add_word_suggestion`, `add_last_word_shortcut`, both default true) in `BackupContract`; two settings-search links; seven strings in their own file with all ten translations.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*AddLastWordTest'
```

`AddLastWordTest` (plain JVM, in `suggestions`): the word before the cursor found after spaces and punctuation, with inner apostrophes and hyphens and accents; quotes and dashes around it left out; numbers, punctuation and empty text give none.

## Validation

VALIDATION_PLACEHOLDER

## Device check (still to do)

Not yet run on a device. Suggested check with a hardware keyboard: type `zorbly` and press Ctrl + Shift + D; the message says it was added, and Auto-correction's user dictionary lists it. Press it again for "already in the dictionary". Turn Offer to add new words off and type an unknown word: no chip.
