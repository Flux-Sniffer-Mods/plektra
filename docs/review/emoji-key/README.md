# The emoji key

A setting in SYM customization, above the emoji pages: **Emoji picker key** (off until a key is chosen, by pressing it). In a text field, that key opens or closes the emoji picker, or the emoji layer (**Emoji key opens**). Held, the next key types its emoji from the emoji layer; with **Tap the emoji key for one emoji** (off by default), one tap does the same for the next key. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026), where Right Shift is the emoji key by default; here no key is taken until one is chosen.

## Behaviour

- Any key the keyboard has can be chosen except ones that type text or are needed while typing (letters, digits, Space, Enter, Backspace, Tab, Esc, Back, arrows, SYM, volume and power). Outside text fields the key keeps its usual role.
- A key that isn't a modifier opens or closes the screen when pressed. A modifier key (Right Shift, Right Alt) does it when released, so it still works in chords: with Ctrl or Alt, or with another key pressed meanwhile, it opens nothing.
- Held, the next key types its emoji from the emoji layer (its shifted emoji with the other Shift). A key with no emoji types as usual. Held past the long-press time without choosing anything, it just lets go.
- Tap for one emoji: a tap makes the next key type its emoji, a second tap opens the screen, and Back cancels it. Leaving the field ends it.

## Implementation boundaries

- `inputmethod/EmojiKey` holds the three settings (`emoji_picker_key`, `emoji_key_opens_layer`, `emoji_sticky_tap`, registered in `BackupContract`), which keys are allowed, and the key's held, chorded and waiting state.
- `PhysicalKeyboardInputMethodService`: `handleEmojiKeyDown` runs early in `onKeyDown`, before the modifiers, so a Shift chosen as the emoji key never reaches the Shift state; the emoji key's release is handled first in `onKeyUp`. `toggleEmojiKeyScreen` opens the picker (as the existing emoji shortcuts do) or the layer. `resetModifierStates` resets it.
- `SymLayoutController.resolveEmojiLayerSymbol` looks a key's emoji up on the emoji layer.
- `EmojiKeySetting` holds the rows and the press-to-assign dialog; two settings-search links (`sym.emoji_key`, `sym.emoji_sticky`, the second while a key is chosen); 18 strings in their own file with all ten translations.
- No LED of its own (Flux Keyboard adds a fifth status LED for it).

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*EmojiKeyTest'
```

`EmojiKeyTest` (in `keys-modifiers`), with Right Shift as the emoji key on the real keys: off until chosen, and backed up; keys that type or are needed while typing refused; a tap opening and closing the screen; held, the next key typing its emoji without Shift carrying over; a long hold letting go; tap for one emoji, and twice to open; Back cancelling.

## Validation

VALIDATION_PLACEHOLDER

## Device check (still to do)

Not yet run on a device. Suggested check: choose Right Shift in **Emoji picker key** by pressing it. In a text field, tap Right Shift (the picker opens) and again (it closes). Hold Right Shift and press Q (Q's emoji is typed, nothing opens). Press Ctrl+Right Shift+Q and check it isn't taken as the emoji key. Then turn on tap for one emoji and check one tap then Q types the emoji, and Back after a tap cancels it.
