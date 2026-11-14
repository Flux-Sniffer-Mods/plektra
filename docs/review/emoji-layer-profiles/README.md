# Emoji layer profiles

A section in SYM customization, under the emoji pages: **Emoji layer profiles**. A profile puts 26 emoji on the emoji layer, one per letter key, for a situation: nine built in (Everyday, Chatting, Work, Social media, Love, Celebrations, Food and drink, Travel and outdoors, Gaming), and your own. With **Switch by app** (off by default), the layer follows the app you're in without changing your saved one. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

- Tapping a situation makes it your emoji layer (it replaces the layer's saved emoji, as editing them does; the layer editor shows it at once). **Save the emoji layer as a profile** keeps the layer as it is now; a profile of your own can be used, updated from the layer, renamed or deleted.
- Switch by app: messengers get Chatting, social apps Social media, dating apps Love, work and email apps Work, food delivery apps Food and drink, maps and travel apps Travel and outdoors. Other apps keep your own layer. An app is placed from a short list of well-known apps, then from the category it declares to Android (social, productivity, maps).
- The emoji pages, built from the layer's emoji, follow the same layer.

## Implementation boundaries

- `data/mappings/EmojiLayerProfiles` (ported; Flux Keyboard's app categories come from its app shortcut list, which Plektra doesn't have, so `AppSituations` holds a short list and reads `ApplicationInfo.category`). Its three preferences (`emoji_layer_profiles`, `emoji_layer_active_profile`, `emoji_layer_switch_by_app`) are registered in `BackupContract`.
- `AlternateCharacterManager.setEmojiLayerOverride`: an app's layer in place of the saved one, without saving it.
- `PhysicalKeyboardInputMethodService`: `onStartInput` sets the app's layer; the preference listener applies the switch at once.
- `EmojiLayerProfilesSection` is the section (collapsed until tapped); a settings-search link (`sym.emoji_profiles`); 26 strings in their own file with all ten translations. Flux Keyboard's import and share of profile files are left out.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*EmojiLayerProfilesTest'
```

`EmojiLayerProfilesTest` (in `ui-settings`): every built-in profile on all 26 letter keys; applying one; your own kept, renamed and deleted (forgetting the one in use); apps placed by list and by declared category; switch by app off by default, and the backup registration; the override leaving your saved layer alone; and the keyboard switching the layer as an app's field starts.

## Validation

VALIDATION_PLACEHOLDER

## Device check (still to do)

Not yet run on a device. Suggested check: in SYM customization open **Emoji layer profiles** and tap Food and drink; open the emoji layer and check 🍕 is on Q. Turn on Switch by app, open WhatsApp or Messages (Chatting: 😂 on Q) and Google Maps (Travel: ✈️ on Q), then an app with neither and check your own layer is back.
