# Emoticons keep their shape

With a mark ticked under **Before next text** in **Punctuation spacing**, the deferred space went in before whatever came next, so text emoticons broke apart: `:-)` became `: -)` and `:D` became `: D`. Plektra's note on the setting says all marks are off by default "so ASCII smileys and quotes stay easy to type"; this makes the two work together. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

This branch builds on `flux/punctuation-space-numbers-and-fields`: its first commit is that change, which this one needs (it records which mark the space is pending after).

## Behaviour

| Typed, with the mark ticked | Before this change | Now |
| --- | --- | --- |
| `:-)` `;(` `:'(` | `: -)` `; (` `: '(` | `:-)` `;(` `:'(` |
| `:D` then a space | `: D ` | `:D ` |
| `Note:Done` | `Note: Done` | `Note: Done` (the space goes in at the second letter) |
| `Done.Then` | `Done. Then` | unchanged: full stops always keep their space |
| `a,(b)` | `a, (b)` | unchanged: a bracket after a comma starts the next words |
| `12:30`, `Time:30` | `12:30` (with the numbers change), `Time: 30` | unchanged |

Rules, in `DeferredPunctuationSpaceTracker.continuesEmoticon()`: never after a full stop; a letter waits only after `:` or `;`, and so does one of the digits used as mouths (`:3`); a double quote or guillemet never continues one, and an opening bracket or `'` only after `:` or `;`; any other mark continues it. A letter held back after `:` or `;` gets its space as soon as the next key shows it starts a word.

**Emoticons keep their shape** is on by default and appears under Punctuation spacing once any mark is ticked under Before next text. Off, spacing works as before.

## Implementation boundaries

- `DeferredPunctuationSpaceTracker` gains the emoticon check and a held letter, which `clear()` and `startField()` reset. No other typing path changes.
- New: one preference (`emoticon_punctuation`, default true) with `SettingsManager.get/setEmoticonPunctuation`, registered in `BackupContract`; one settings-search link (`text_input.emoticon_punctuation`) that falls back to Punctuation spacing while no mark is ticked; two strings in their own file (`emoticon_punctuation.xml`) with all ten translations.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*DeferredPunctuationSpaceTrackerTest' --tests '*BackupPreferenceContractIntegrationTest' --tests '*SettingLinkCoverageTest'
```

Four new cases in `DeferredPunctuationSpaceTrackerTest` type character by character through the tracker: a sentence with `:-)` `;(` `:D` and `:'(`; `Note:Done`, where the held letter gets its space back; full stops and a bracket after a comma, which keep theirs; and the switch, on by default and, turned off, spacing as before. `BackupPreferenceContractIntegrationTest` checks that the new key is exported.

## Validation

Run on 11 November 2026 against source commit `2f93fa14` (upstream `68ace8ab` plus `flux/punctuation-space-numbers-and-fields` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 185 classes, 1,427 tests, no failures, errors or skips (upstream alone: 185 classes, 1,417 tests; the numbers-and-fields change adds 6). New: 4 in `DeferredPunctuationSpaceTrackerTest`. `BackupPreferenceContractIntegrationTest` and `SettingLinkCoverageTest` pass with the new preference and link.
- On upstream alone, the emoticon sentence gives `Hi : -) ok ; ( fine : D yes : '( no`.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`; the new strings have all ten translations.

## Device check (still to do)

Not yet run on a device. Suggested check: in Punctuation spacing, tick `:` `;` and `,` under Before next text. In a message, type `Great :-) see you ;( maybe :D` and `Note:Done`; the emoticons should stay whole and `Note: Done` get its space. Turn Emoticons keep their shape off and type `:-)` again; it should come out as `: -)`.
