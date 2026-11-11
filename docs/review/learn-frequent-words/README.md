# Learn words you use often

A word that isn't in the dictionary goes into the user dictionary once it has been typed three times, so it is suggested and never auto-corrected. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

- **Learn words you use often**, in Auto-correction under the user dictionary (on by default).
- Counted: words of 3 to 32 letters, with an inner apostrophe or hyphen allowed (`can't`, `well-known`, `caffè`); not digits or punctuation. Only words unknown to the active dictionaries, and only once they are loaded.
- The form learned is lowercase if the word was ever typed that way, so a word capitalised only at the start of sentences isn't kept capitalised; a name always typed with a capital keeps it.
- Counts are kept between sessions for the 300 most recently seen words, and are in ZIP backups.

## Implementation boundaries

- `core/suggestions/FrequentWordLearner` counts in a preference (`frequent_unknown_words`, JSON) and returns the form to learn at the third use. `SuggestionController` calls it at each completed word, next to next-word learning, and adds the word with its existing `addUserWord`.
- New: one switch (`learn_frequent_words`, default true) and the counts, both in `BackupContract`; a settings-search link (`auto_correction.learn_frequent_words`); two strings in their own file with all ten translations.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*FrequentWordLearnerTest'
```

`FrequentWordLearnerTest` (in `suggestions`): learned at the third use and counted afresh after, counts kept between sessions, the lowercase form preferred, a name kept capitalised, only real words counted, and only the most recently seen words tracked.

## Validation

VALIDATION_PLACEHOLDER

## Device check (still to do)

Not yet run on a device. Suggested check: type a made-up word (`zorbly`) followed by a space three times in a message; it should then appear in the user dictionary (Auto-correction, user dictionary) and be offered as a suggestion instead of being corrected.
