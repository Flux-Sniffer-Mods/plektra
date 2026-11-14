# Colours from the wallpaper

A switch in the keyboard theme's assignment panel (Customization → Keyboard theme): **Colours from wallpaper** (Android 12 and later, off by default). The keyboard takes its colours from the wallpaper's Material You palette, in light or dark mode; the theme's shape and sizes stay. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

- Over whichever theme applies (fixed, following the system, or a layout's own), for both the hardware bar and the on-screen keyboard: background, keys, text, LEDs, accent, popups, suggestions and bar buttons from the wallpaper's neutral and accent tones. A see-through background stays see-through.
- Light or dark goes with the system, as Material You does. Changing the wallpaper changes the colours once Android updates its palette.
- Below Android 12, the row isn't shown and the theme is unchanged. Off: as before.

## Implementation boundaries

- `WallpaperKeyboardColours` (ported from Flux Keyboard) holds the switch (`keyboard_wallpaper_colours`, registered in `BackupContract`), reads Android's `system_*` palette colours and maps them onto `KeyboardThemeSettings`.
- `SettingsManager.getEffectiveKeyboardTheme` passes its result through it; `isKeyboardThemePreferenceKey` includes the switch, so the keyboard redraws when it changes.
- `WallpaperColoursSetting` is the row, in `KeyboardThemeAssignmentSection`; a settings-search link (`keyboard_theme.wallpaper_colours`, Android 12+); two strings in their own file with all ten translations.
- The theme editor's previews still show the theme's own colours.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*WallpaperKeyboardColoursTest'
```

`WallpaperKeyboardColoursTest` (in `ui-settings`): dark and light tones from a test palette, with the background's transparency and the theme's shape kept; off by default, backed up and redrawing the keyboard; and, switched on, the effective theme in Android's palette colours.

## Validation

Run on 14 November 2026 against source commit `5de26d13` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 186 classes, 1,420 tests, no failures, errors or skips (upstream alone: 185 classes, 1,417 tests). Of these, 3 are new, in `WallpaperKeyboardColoursTest`.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass; the new class is in the `ui-settings` topic.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`.

## Device check (still to do)

Not yet run on a device. Suggested check on Android 12 or later: turn the switch on and check the keyboard's colours match the system's Material You colours (the quick settings tiles, say); change the wallpaper to a very different colour and check the keyboard follows; switch dark mode on and off; turn the switch off and check the theme's own colours return.
