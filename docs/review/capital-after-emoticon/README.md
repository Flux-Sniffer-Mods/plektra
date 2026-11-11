# Capital after an emoticon

A new switch under automatic capitalization, **Capital after an emoticon** (on by default): a text emoticon such as `:)` `;D` or `<3` ends a sentence as a full stop does. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

- `Great :) see you` → `Great :) See you`, with **Capitalize after period** on.
- Double space after an emoticon adds no full stop (`Great :)` then two spaces stays `Great :)  `), as after `.` `!` `?`.
- Counted: eyes `:` `;` `=`, an optional nose or tear (`-` `'` `^` `o`), and a mouth (`)` `(` `D` `P` `p` `O` `o` `3` `/` `\` `|` `*` `$` `@` `S` `s` `b` `c` `X` `>` `]` `[`), plus `<3` `^^` `^_^` `xD` `XD` `T_T` `-_-` `o_O`. Only as a whole word, after a space or at the start: `(see above)`, `chapter 8)`, links and `Note:D` don't count.
- Off: as before.

## Implementation boundaries

- `core/EmoticonSentences` holds the pattern and a flag set from the setting as each field starts (and when the switch changes). `AutoCapitalizeHelper.hasSentenceEndingPunctuation`, used by automatic Shift and by double space, returns true for an emoticon first. No other typing path changes.
- New: one preference (`auto_cap_after_emoticon`, default true) in `BackupContract`; a settings-search link (`text_input.auto_cap_after_emoticon`); two strings in their own file with all ten translations.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*EmoticonSentencesTest'
```

`EmoticonSentencesTest` (in `editing`): emoticons that end a sentence and look-alikes that don't; and through `hasSentenceEndingPunctuation`, a capital once a space follows, no full stop from double space, a bracket closing words not counted, and nothing with the switch off.

## Validation

Run on 11 November 2026 against source commit `07856ecb` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 186 classes, 1,419 tests, no failures, errors or skips (upstream alone: 185 classes, 1,417 tests). New: 2 in `EmoticonSentencesTest`; `SettingLinkCoverageTest` and `BackupPreferenceContractIntegrationTest` pass with the new link and preference.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass; the new class is in the `editing` topic.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`.

## Device check (still to do)

Not yet run on a device. Suggested check: with Capitalize after period on, type `Great :) see you` and confirm the capital; type `ok ;-)` and press Space twice, and confirm no full stop is added. Turn the switch off and repeat.
