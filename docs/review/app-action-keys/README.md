# Apps' shortcuts on keys

A switch with the quick launcher's search settings: **Apps' shortcuts on keys** (off by default). In any app, Ctrl + Alt + 1 to 4 open the app's own shortcuts (the ones its icon shows on a long press), in the app's order, and Ctrl + , opens its settings screen. Ported from Flux Keyboard's app shortcuts onto upstream `68ace8ab` (10 November 2026).

**Stacked:** this branch carries `flux/quick-launcher-app-shortcuts` (its feature commit), whose shortcut discovery it uses. Review and merge that first; this PR's own change is its last commit.

## Behaviour

- On a keyboard without a number row, the keys whose Alt character is 1 to 4 do the same (Ctrl + Alt + the key that types 1).
- An app without that shortcut, or without a settings screen, gets the key as before. Only screens the app lets other apps open are opened.
- With or without a text field; Plektra's own screens are left alone.

## Implementation boundaries

- `shortcuts/AppActionKeys` holds the switch (`app_action_keys`, registered in `BackupContract`), which combo opens which screen, and opens it through `AppActionDiscovery`.
- `PhysicalKeyboardInputMethodService`: `openAppActionKey` runs early in `onKeyDown`, before Plektra's own Ctrl shortcuts; the key's release is dropped in `onKeyUp`.
- `AppActionKeysSetting` is the row; a settings-search link (`quick_launcher.app_action_keys`); two strings in their own file with all ten translations (new here).
- Flux Keyboard's other app shortcuts (the same standard combos sent as each app's own documented shortcut, from presets per app and category) are left out: they rest on a large researched list of apps.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*AppActionKeysTest'
```

`AppActionKeysTest` (in `keys-modifiers`): Ctrl + Alt + 1 to 4 and Ctrl + , choosing the screens, by key or by the key's Alt character; other combos left alone; off by default and backed up; an app without the shortcut letting the key go on.

## Validation

Run on 14 November 2026 against source commit `837c8e80` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 187 classes, 1,425 tests (upstream alone: 185 classes, 1,417 tests). Of these, 4 are new, in `AppActionKeysTest`; the other 4 are `AppShortcutCommandsTest`, from `flux/quick-launcher-app-shortcuts`, which this branch is built on (186 classes and 1,421 tests without this change). One failure, `ClipboardHistoryTest.startupShowsLoadingAndBlocksHistoryActionsUntilReady`: a race in that upstream test, unrelated to this change. The class passed when rerun on its own; `flux/test-races` fixes the race.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass; the new class is in the `keys-modifiers` topic.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`.

## Device check (still to do)

Not yet run on a device. Suggested check with the switch on: in WhatsApp or Gmail press Ctrl + Alt + 1 (the app's first long-press shortcut opens, as from its icon) and Ctrl + , (its settings, for apps that offer them). In an app with no shortcuts, check the keys do what they did before.
