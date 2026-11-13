# The phone's spell checker in suggestions

A switch in Auto-correction, with the other suggestion settings: **The phone's spell checker in suggestions** (off by default). When Android's spell checker is another app's (Gboard's, say), its corrections for a word Plektra's dictionary doesn't know as typed join the suggestion bar. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026), where it is on by default; here it is off, as the words being typed go to that app.

## Behaviour

- Asked only for a word of three letters or more that the dictionary doesn't have as typed, and only while the chosen spell checker is another app's (Android 12 and later report which it is; earlier, nothing is asked).
- Its corrections, if it says the word looks misspelt, come second in the bar, after the dictionary's best; one already shown isn't repeated. A reply for a word no longer being typed is dropped.
- Off, or with Plektra's own spell checker chosen: as before.

## Implementation boundaries

- `spellcheck/PhoneSpellChecker` keeps one `SpellCheckerSession` per language and reads its answer; the switch (`phone_spell_checker_suggestions`) is registered in `BackupContract`.
- `SuggestionController` takes an optional `externalSuggestions` and, after publishing the dictionary's suggestions for an unknown word, merges what it answers. The keyboard service passes the phone's spell checker in and closes its session in `onDestroy`.
- `spellcheck/PhoneSpellCheckerSetting` is the switch's row (enabled with the suggestion engine); a settings-search link (`auto_correction.phone_spell_checker`); two strings in their own file with all ten translations.
- With the incognito typing PR also merged, a word typed incognito shouldn't be sent; that is one check in `SuggestionController` once both are in.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*PhoneSpellCheckerTest'
```

`PhoneSpellCheckerTest` (in `suggestions`): off by default; corrections read only from an answer marking the word misspelt; and through `SuggestionController`, an unknown word asked about once with the answer second in the bar and nothing shown twice.

## Validation

VALIDATION_PLACEHOLDER

## Device check (still to do)

Not yet run on a device. Suggested check: with Gboard's spell checker chosen in Android's settings and the switch on, type `recieve`: Gboard's `receive` should appear in the bar if Plektra's dictionary doesn't offer it first. Turn the switch off and check it no longer does.
