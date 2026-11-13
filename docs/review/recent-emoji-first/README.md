# Recent emoji first on the emoji layer

A switch in SYM customization, under the emoji pages switch: **Recent emoji first on the layer** (off by default), with how many keys they take (1 to 10, 5 by default). The emoji you used last take the classic emoji layer's first keys, and its own emoji move along. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

- Only the classic emoji layer, one emoji per key: the row is shown while the emoji pages are off, as the pages have Recents of their own.
- The most recent emoji goes on Q, then along the rows; the layer's own emoji follow in their key order, without any already shown. Keys without an emoji stay empty, so the layer keeps its shape. SYM chords type what the layer shows.
- "Used" counts emoji typed from the layer, with a SYM chord, from the emoji pages and from the picker, newest first, up to 40. Nothing is kept while the switch is off.
- Shifted emoji (the layer's uppercase mappings) are unchanged.

## Implementation boundaries

- `data/emoji/EmojiLayerRecents` holds the switch and count (`emoji_layer_recents_first`, `emoji_layer_recents_count`, registered in `BackupContract`), the used list (`emoji_layer_used`, left out of backups as usage history, like the picker's recents) and the arrangement.
- `SymLayoutController`: the classic emoji layer's mappings and its chord symbol go through `EmojiLayerRecents.layer`; an emoji typed from the layer or its pages is marked used, and `chordSymbolTyped` marks a chord's. The keyboard service calls it after a chord commits; `EmojiPickerView` marks the picker's emoji.
- `EmojiLayerRecentsSetting` is the row; a settings-search link (`sym.emoji_layer_recents_first`), available while the emoji pages are off; three strings in their own file with all ten translations.
- With the incognito typing PR also merged, an emoji typed incognito shouldn't be marked; that is one check in `markUsed` once both are in.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*EmojiLayerRecentsTest'
```

`EmojiLayerRecentsTest` (in `ui-settings`): the arrangement (recent first, the layer's own moved along, none twice, the count kept, empty keys left empty); off by default keeping nothing, and the backup registration; the used list newest first; through `SymLayoutController`, an emoji typed from the layer coming first on it and in its chord; and a chord from the emoji layer marked used.

## Validation

VALIDATION_PLACEHOLDER

## Device check (still to do)

Not yet run on a device. Suggested check: turn the emoji pages off and the switch on, open the emoji layer and type the emoji on its last key; open the layer again and check it is on Q, with Q's own emoji on W. Type an emoji from the picker and check it comes first too.
