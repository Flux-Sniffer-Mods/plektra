# Double space: a full stop only after something typed

Double space turned the second space into ". " wherever it was pressed. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

| Before the two spaces | Before this change | Now |
| --- | --- | --- |
| The start of the field | `. ` | two spaces |
| The start of a line (`First line↵`) | `First line↵. ` | two spaces |
| Only spaces | `   . ` | spaces |
| `Hello,` `Note:` `see (` `red/` (punctuation that continues the sentence) | `Hello,. ` | two spaces |
| `hello`, `(see above)`, `42` | `hello. ` | `hello. ` (unchanged) |

Existing checks are kept: the setting, restricted fields, text already ending a sentence (`. ! ?`) and the auto-space case.

## Implementation boundaries

- One check in `TextInputController.handleDoubleSpaceToPeriod()`: the last character before the spaces must exist, must not be a line break, and must not be one of `, ; : - – — ( [ { / \`. Closing brackets and quotes still end a sentence.
- No preference, string or resource changes.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*TextInputControllerTest'
```

Three new cases in `TextInputControllerTest` press Space twice through the real controller: nothing typed before (field start, after a line break, only spaces), punctuation that continues the sentence, and words, closing brackets and digits, which still end the sentence.

## Validation

Run on 11 November 2026 against source commit `a56b7338` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 185 classes, 1,420 tests, no failures, errors or skips (upstream alone: 185 classes, 1,417 tests). New: 3 in `TextInputControllerTest`.
- The new tests on upstream alone, without the fix: `doubleSpaceToPeriod_addsNoFullStopWhereNothingWasTyped` and `doubleSpaceToPeriod_addsNoFullStopAfterPunctuationThatContinuesTheSentence` fail; `doubleSpaceToPeriod_stillEndsTheSentenceAfterAWordOrClosingBracket` passes on both, as it checks what is unchanged.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`.

## Device check (still to do)

Not yet run on a device. Suggested check: in a new message, press Space twice at the very start, after Enter, and after typing `Hello,`; none should add a full stop. After `Hello`, two spaces should still give `Hello. ` and a capital next.
