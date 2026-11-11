# Incognito typing

Two switches in Settings → Advanced: **Incognito typing** (off by default) and, while it is off, **Incognito when an app asks** (on by default). While a field is incognito, the keyboard learns nothing from what is typed in it. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

- Apps mark fields where keyboards should not learn with `EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING`: private browser tabs and some messengers. Plektra does not read the flag at present, so it learns there. By default those fields are now incognito; **Incognito typing** makes every field incognito.
- Not learned while incognito: next-word predictions and sentence starts, how often personal dictionary words are used (their ranking), and recent emoji, symbols and kaomoji.
- Still works: suggestions, predictions and recents learned earlier are offered as before, and adding a word to the dictionary yourself still adds it.
- Each field decides when it starts, so a change to the switches applies from the next field.

## Implementation boundaries

- `core/IncognitoTyping` holds both preferences (`incognito_always`, default false; `incognito_follow_apps`, default true), the field rule and the `active` flag. The service sets the flag in `onStartInput`, and `SuggestionController.incognito` with it.
- Guards: the word boundary in `SuggestionController` (next words, sentence starts), `UserDictionaryStore.markUsed`, `RecentEmojiManager.addRecentEmoji` and `RecentItems.add` (symbols and kaomoji).
- New: two preferences registered in `BackupContract`; two settings-search links (`advanced.incognito_always`, and `advanced.incognito_follow_apps`, hidden while the first is on and falling back to it); four strings in their own file (`incognito_typing.xml`) with all ten translations. No permission, route or migration.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*IncognitoTypingTest' --tests '*SuggestionControllerIncognitoTest' --tests '*IncognitoFieldServiceTest' --tests '*IncognitoTypingBackupTest'
```

`IncognitoTypingTest`: the defaults and the field rule; recent emoji, symbols and kaomoji untouched while incognito and recorded again after; uses of dictionary words not counted. `SuggestionControllerIncognitoTest`: no next words learned while incognito, earlier ones still predicted, learning resumes after. `IncognitoFieldServiceTest`: `onStartInput` sets the flag for each field under each setting. `IncognitoTypingBackupTest`: ZIP export, clear and restore of both switches.

## Validation

VALIDATION_PLACEHOLDER

## Device check (still to do)

Not yet run on a device. Suggested check: in a browser's private tab, type `pink elephant` a few times and insert an emoji; in an ordinary field, type `pink` and a space: `elephant` should not be predicted and the emoji should not be among the recents. Then turn on Incognito typing and repeat in an ordinary field.
