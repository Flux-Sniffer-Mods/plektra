# Cursor and selection moves in long fields

`TextSelectionHelper` reads the selection from `ExtractedText`, which is relative to the extract, and passes it to `setSelection`, which takes positions in the whole field. When an app hands over only part of a long field (`startOffset > 0`), arrow moves, Shift+arrow selections and word moves therefore jumped towards the start of the field. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

Moves now land where they should in such fields: one character or one word on, and selections grow or shrink from the right place. Fields handed over whole are unchanged (an offset of 0).

## Implementation boundaries

- `TextSelectionHelper` gets one small wrapper, `Extracted`, built by `extract()`: the extract's text, its offset, and the selection in whole-field positions. The eight `getExtractedText` calls go through it; field-end checks use the extract's end; word searches convert to and from positions within the extract.
- The fallback paths without an extract, the selection anchor logic and every public signature are unchanged. Moves past the end of the extract are still refused, as before.
- No preference, string or resource changes.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*TextSelectionHelperWordNavigationTest'
```

Four new cases in `TextSelectionHelperWordNavigationTest` use an extract starting 1,000 characters in: word left, word right, word selection to the right and a single move right at the extract's last character, each checking the whole-field positions given to `setSelection`.

## Validation

Run on 11 November 2026 against source commit `c2cbe39c` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 185 classes, 1,421 tests, no failures, errors or skips (upstream alone: 185 classes, 1,417 tests). New: 4 in `TextSelectionHelperWordNavigationTest`.
- The new tests on upstream alone, without the fix: all four fail, each with the extract-relative position given to `setSelection` (for example 11 where 1,011 was wanted).
- `python3 scripts/ci-tests.py check` and the Python guard tests pass.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`.

## Device check (still to do)

Not yet run on a device. Suggested check: in an app that hands over long fields in part (some note and code editors), in a note of several thousand characters, use Alt+arrows (word moves) and Shift+arrows near the end; the cursor and selection should move from where they are, not from near the top.
