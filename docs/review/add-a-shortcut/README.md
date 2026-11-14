# Add a shortcut

A row with the quick launcher's search settings: **Add a shortcut**. It opens a screen listing the apps that offer shortcuts for the home screen (a contact's direct dial, a bookmark, a settings page); picking one opens that app's own screen for choosing the shortcut, and the shortcut becomes a quick launcher result, assignable to a key like any command. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

**Stacked:** this branch carries `flux/quick-launcher-app-shortcuts` (its feature commit), whose shortcut intents it uses. Review and merge that first; this PR's own change is its last commit.

## Behaviour

- Only the home screen app can read the shortcuts already on the home screen, so each is added once here. Your shortcuts are listed with their app and can be removed.
- A shortcut an app only pins to the home screen (one only a home screen may start) can't be kept: a message says so, and that app's screen isn't offered again.
- A contact's direct dial without the phone permission opens the dialer with the number ready.
- A shortcut whose app is uninstalled isn't offered (it comes back if the app does).

## Implementation boundaries

- `shortcuts/UserShortcuts` keeps the shortcuts (`quick_launcher_user_shortcuts`) and the screens that can't be used (`quick_launcher_unsupported_shortcut_screens`), both left out of backups as they belong to this phone's apps. Shortcut icons are saved in `filesDir`.
- Hardening beyond Flux Keyboard's: a returned shortcut that would open one of Plektra's own screens is refused, both when added and when started; a stored intent is started without file-access grants or clip data.
- `commands/UserShortcutCommandSource` lists them (source App actions, kind Shortcut); `CommandExecutor` starts them through `UserShortcuts.startableIntent`. Added to `CommandRegistry`.
- `UserShortcutsActivity` (not exported) is the screen; a row and a settings-search link (`quick_launcher.add_shortcut`); nine strings in their own file with all ten translations.
- Flux Keyboard's own shortcuts (call or message a contact, a website, Termux tasks) and the quick launcher's long-press entry are left out.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*UserShortcutsTest'
```

`UserShortcutsTest` (in `ime-integration`): an app's shortcut becoming a result and leaving when removed; Plektra's own screens never kept or started; a stored shortcut starting without file access; a direct dial without the phone permission opening the dialer; and both preferences kept out of backups.

## Validation

Run on 14 November 2026 against source commit `dfec305b` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 187 classes, 1,426 tests (upstream alone: 185 classes, 1,417 tests). Of these, 5 are new, in `UserShortcutsTest`; the other 4 are `AppShortcutCommandsTest`, from `flux/quick-launcher-app-shortcuts`, which this branch is built on (186 classes and 1,421 tests without this change). One failure, `ClipboardHistoryTest.startupShowsLoadingAndBlocksHistoryActionsUntilReady`: a race in that upstream test, unrelated to this change. The class passed when rerun on its own; `flux/test-races` fixes the race.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass; the new class is in the `ime-integration` topic.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`.

## Device check (still to do)

Not yet run on a device. Suggested check: open **Add a shortcut**, pick Contacts' direct dial (or Chrome's bookmark, or Settings' shortcut), choose one in the app's screen, then find it in the quick launcher and open it. Remove it and check it's gone.
