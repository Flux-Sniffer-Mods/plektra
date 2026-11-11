# Emoji suggestions

While a word is being typed, the bar's third suggestion (its left slot) can be an emoji for it: 🍕 for `pizza`, 🚀 for `rocket`. Picking it keeps the word and adds the emoji after it. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

- **Emoji suggestions**, in Auto-correction next to accent matching (on by default, available with word suggestions on).
- The emoji is the one whose name is the word, else the first in picker order of those with it as a keyword (`happy`, `love`), in the languages emoji search already uses. None for words under three letters, generic words (`time`, `up`, `button`, `the`…) or keywords shared by more than 30 emoji.
- It takes the third slot after the word suggestions, or replaces the third; an emoji already among them isn't repeated, and keycaps (`1️⃣`) aren't offered.
- Picking it, by tap or a trackpad swipe, commits `word 🍕 `; the word is kept as typed.

## Implementation boundaries

- `EmojiSearchRepository.suggestionFor()` looks the word up in the emoji search index, which holds only emoji the device can draw; `cachedSearchIndex()` returns it without waiting once built. The typing path never builds it: the first request builds it on `expansionAssetScope` and the next suggestions use it.
- `PhysicalKeyboardInputMethodService.visibleSuggestionStrings()` adds the emoji; the pick paths (`SuggestionButtonHandler` and the service's suggestion pick) commit it with `EmojiSuggestion.commitAfterWord` instead of replacing the word. `SuggestionController.currentWord()` exposes the word being typed.
- New: one preference (`emoji_suggestions_enabled`, default true) in `BackupContract`; a settings-search link (`auto_correction.emoji_suggestions`); two strings in their own file with all ten translations.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*EmojiSuggestionTest'
```

`EmojiSuggestionTest` (in `ui-settings`, with the other emoji search tests): a name match first and the index then at hand without waiting, a keyword match, nothing for generic, short and unknown words, emoji told apart from words and keycaps, and the emoji committed after the word with a space each side.

## Validation

VALIDATION_PLACEHOLDER

## Device check (still to do)

Not yet run on a device. Suggested check: with word suggestions on, type `pizza` and confirm 🍕 in the left slot; tap it and confirm `pizza 🍕 `. Type `the` and confirm no emoji. Turn Emoji suggestions off and confirm the third word suggestion comes back.
