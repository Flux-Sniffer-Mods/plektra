# Remember the language per app

A switch, **Remember language per app** (Settings → Input styles, with the language-switch shortcuts; off by default): a language chosen while typing in an app becomes that app's, and entering the app again switches back to it. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026), where it is on by default; here it is off, as it changes the language on its own.

## Behaviour

- Switching language in any way (Ctrl+Space, Alt+Shift, the globe, the system picker) while an app's field is focused makes that the app's language, with its input style (layout).
- Entering another app switches to its language, once per visit: changing the language again there is not undone while you stay in it. An app with no language of its own keeps the current one.
- A language no longer enabled is skipped. Plektra's own settings screens are not an app here.
- Turning the switch off forgets every app's language.

## Implementation boundaries

- `inputmethod/LanguagePerApp` holds the switch (`language_per_app_enabled`, registered in `BackupContract`) and the languages, kept in a preferences file of their own (`app_languages`) that backups don't export: they belong to the apps on this phone.
- `SubtypeCycler.subtypeKey` names a language as its locale and input style; `switchToSubtypeKey` switches to an enabled one with its layout, through the same path as cycling.
- `PhysicalKeyboardInputMethodService`: `onStartInput` (not on a restart of the same field) restores the app's language; `onCurrentInputMethodSubtypeChanged` remembers the new one for the focused app.
- New: one settings-search link (`custom_input_styles.language_per_app`); two strings in their own file (`language_per_app.xml`) with all ten translations. No permission, route or migration.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*LanguagePerAppTest'
```

`LanguagePerAppTest` (in `devices-layouts`): off unless switched on, and turning it off forgetting every app; a language kept with its input style; a language change in an app's field remembered for that app only while the switch is on; and the keyboard's own settings not taken for an app. Switching languages needs the system's input method list, which Robolectric doesn't provide, so the restore itself is for the device check.

## Validation

VALIDATION_PLACEHOLDER

## Device check (still to do)

Not yet run on a device, with two languages enabled. Suggested check: turn the switch on; in a messaging app switch to the second language and type; open a browser and switch back to the first; return to the messaging app: the second language (and its layout) should come back. Turn the switch off and confirm returning no longer changes the language.
