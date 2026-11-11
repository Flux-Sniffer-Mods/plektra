# Pick suggestions from the keyboard

Ctrl+Shift+Q, W or E takes the bar's left, middle or right suggestion; on keyboards with a number row, Ctrl+1, 2 or 3 can be chosen instead. This answers palsoftware/pastiera#217 and is ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

- **Pick suggestions from the keyboard**, in Auto-correction after edit-type ranking: Ctrl+Shift+Q/W/E (until chosen), Ctrl+1/2/3, or Off.
- Only while the bar shows suggestions and the SYM layer is off; otherwise the keys do what they always did. Ctrl+W alone, Ctrl+Alt chords and plain letters are never taken. Either Shift works, and Ctrl held or tapped.
- A pick goes through the same path as a trackpad swipe or a tap (`acceptSuggestionAtIndex`): the suggestion replaces the word, an emoji goes after it. The key's release is swallowed with it.

## Implementation boundaries

- `inputmethod/SuggestionKeys.slotFor()` is pure: the bar third a key and modifiers pick, or null. The service checks it before Alt+Enter and Ctrl+Space layout switching, and swallows the matching key-up.
- New: one preference (`suggestion_keys`, `ctrl_shift_qwe` until chosen) in `BackupContract`; a settings-search link (`auto_correction.suggestion_keys`); four strings in their own file with all ten translations.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*SuggestionKeysTest'
```

`SuggestionKeysTest` (in `keys-modifiers`): Q, W and E follow the bar's left, middle and right; Ctrl+W, Ctrl+Alt chords, Shift without Ctrl, other letters and Off take nothing; the number-row option.

## Validation

VALIDATION_PLACEHOLDER

## Device check (still to do)

Not yet run on a device. Suggested check with a hardware keyboard: type `helo` and press Ctrl+Shift+W; the middle suggestion replaces it. With no suggestions shown, Ctrl+Shift+W does what the app does with it.
