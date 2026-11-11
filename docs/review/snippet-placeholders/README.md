# Snippet placeholders

Snippets can hold `{date}`, `{time}`, `{datetime}`, `{isodate}`, `{day}` and `{clipboard}`, filled in at the moment the snippet expands. This answers the request in palsoftware/pastiera#282 and is ported from Flux Keyboard (in its 2.0.0 release) onto upstream `68ace8ab` (10 November 2026).

## Behaviour

A snippet such as `Sent {isodate} from {clipboard}` expands to `Sent 2026-11-10 from https://example.org` when its shortcut is accepted (space, Tab or a suggestion tap, as configured).

| Placeholder | Filled with |
| --- | --- |
| `{date}` | The date, as the device locale writes it (medium style) |
| `{time}` | The time, as the device locale writes it (short style) |
| `{datetime}` | Both together |
| `{isodate}` | The date as `yyyy-MM-dd`, whatever the locale |
| `{day}` | The weekday, in the device language |
| `{clipboard}` | The text of the clipboard's first item; nothing when it is empty |

Anything else in braces (`{name}`, `{}`, `{DATE}`) stays as written. The snippet editor lists the placeholders under the replacement field. Emoji and symbol shortcodes are not affected: only the snippet provider fills placeholders.

## Implementation boundaries

- One pure helper, `SnippetPlaceholders`, and one hook in `TextExpansionController.commit()` for matches from the snippet provider. The text committed and the text reported to `onCommitted` are the same, filled text.
- The clipboard is read only when the snippet contains `{clipboard}`, once per expansion, through the controller's new optional `clipboardTextProvider` (default: the primary clip's first item as text). Clipboard text is inserted literally: `$1`, backslashes and placeholders inside it are not interpreted.
- Suggestion previews keep showing the stored snippet text; filling happens only when the snippet is committed.
- No new preference, permission, route or backup field. One new string, `snippets_placeholders_help`, in its own file (`snippet_placeholders.xml`) with all ten translations, so lint's `MissingTranslation` check adds no new error.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*SnippetPlaceholdersTest' --tests '*TextExpansionControllerTest'
python3 scripts/ci-tests.py run --topic editing
```

`SnippetPlaceholdersTest` checks each placeholder against a fixed date (including another locale's weekday), that the clipboard is read once and only when asked for, literal insertion, empty clipboards and untouched braces. `TextExpansionControllerTest` gains three cases through the real controller: a snippet expanding with `{clipboard}`, a snippet without placeholders never reading the clipboard, and a non-snippet provider committing `{clipboard}` unchanged.

## Validation

Run on 11 November 2026 against source commit `20ec627b` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 186 classes, 1,425 tests, no failures, errors or skips (upstream alone: 185 classes, 1,417 tests). New: 5 in `SnippetPlaceholdersTest`, 3 in `TextExpansionControllerTest`.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass; the new class is in the `editing` topic.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`.

## Device check (still to do)

Not yet run on a device. Suggested check: create a snippet `!sig` → `Sent {isodate} {time}: {clipboard}`, copy a link, type `!sig` and a space in any text field, and confirm the date, time and link are filled in. Then type it once more with an empty clipboard and confirm nothing is left where `{clipboard}` was.
