# Plektra as Android's spell checker

Plektra can be chosen as Android's spell checker (Settings → System → Languages → Spell checker): apps then underline words that aren't in Plektra's dictionary for the language, words you added included, and offer its suggestions on a tap. A row in Settings → Advanced, **Spell checker**, explains it and opens Android's spell checker settings. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

- Nothing changes until it is chosen in Android's settings; Android's own or Gboard's spell checker stays the default.
- Every language with a dictionary in Plektra's catalog: German, English and Italian built in, the others once their dictionary is downloaded. A language with no dictionary on the phone underlines nothing.
- Words kept whole with their apostrophes (`couldn't`, `l'homme`), and known when what they are made from is (`couldn't`: `could`, `won't`: `will`, `l'homme`: `homme`). Not checked: numbers, links, addresses, handles, hashtags, acronyms and single letters.
- While the keyboard is typing in the same language, the spell checker uses the dictionary already loaded for it, so it isn't loaded twice; otherwise it loads one more language of its own.

## Implementation boundaries

- `spellcheck/PlektraSpellCheckerService` (ported from Flux Keyboard) is the `SpellCheckerService`, declared with `BIND_TEXT_SERVICE` and `res/xml/spellchecker.xml`, which lists the catalog's languages. `SpellCheckRules` holds what is checked and how a word is split.
- `SuggestionController.spellCheck()` answers from the loaded dictionary when the language matches. The keyboard service hands the controller over (a weak reference) in `onCreate` and clears it in `onDestroy`.
- `spellcheck/SpellCheckerSetting` is the Advanced row; a settings-search link (`advanced.spell_checker`). Three strings in their own file with all ten translations, and an untranslated `%s` subtype label (Android shows the language).
- No preference, permission request, route or migration.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*SpellCheckRulesTest' --tests '*SpellCheckWordsTest' --tests '*KeyboardSpellCheckTest'
```

`SpellCheckRulesTest` and `SpellCheckWordsTest` (Flux Keyboard's, plain JVM): what is checked, words with apostrophes, contractions, possessives and elisions; and here, that the languages are exactly the catalog's and the manifest's. `KeyboardSpellCheckTest` (Robolectric): the loaded dictionary answers known and misspelt words, and leaves another language or a dictionary not yet loaded to the spell checker's own. All in `suggestions`.

## Validation

Run on 13 November 2026 against source commit `efef495b` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 188 classes, 1,425 tests (upstream alone: 185 classes, 1,417 tests). Of these, 8 are new: 4 in `SpellCheckRulesTest`, 2 in `SpellCheckWordsTest` and 2 in `KeyboardSpellCheckTest`. One failure, `EmojiPagesSurfacesTest.aCategoryTabTappedWhileSearchingShowsThatCategorysResults`: a race in that upstream test, unrelated to this change. The class passed when rerun on its own; `flux/test-races` fixes the race.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass; the new classes are in the `suggestions` topic.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`.

## Device check (still to do)

Not yet run on a device. Suggested check: Settings → Advanced → Spell checker, choose Plektra's; in a messaging app type `helo wrold couldn't`: the first two should be underlined, the third not, and tapping `helo` should offer `hello`. Add a word to the user dictionary and confirm it is no longer underlined.
