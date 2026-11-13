# Drag to swap keys in the layer editors

In SYM customization's emoji and symbols layer editors, hold a key and drag it onto another: the two swap what they type, and the change is saved at once. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

- A tap still opens the picker for that key, as before. Held, the key lifts (a drag shadow, with a haptic tick); the key under it dims, and letting go there swaps the two.
- A key with nothing on it takes the dragged one's, which is left empty.
- With the layer's pages on, the keys that turn them (Q and P) are neither dragged nor dropped on.

## Implementation boundaries

- `LayerKeySwap.swapKeys` swaps two keys of a layer's mappings.
- `StatusBarController.createCustomizableEmojiKeyboard` takes an optional `onSwap`; with it, each key gets a long-press drag and a drop listener (`dragToSwap`). Without it (everywhere else), keys are as before.
- `SymCustomizationScreen` passes `onSwap` for the emoji and symbols editors and saves the swapped layer.
- No setting, string, permission or migration.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*LayerKeySwapTest'
```

`LayerKeySwapTest` (in `ui-settings`): two mapped keys swapping; an empty key taking the other's; and the editor grid offering the drag only when swapping is asked for, never on the page-turning keys.

## Validation

VALIDATION_PLACEHOLDER

## Device check (still to do)

Not yet run on a device (the drag itself goes through Android's drag and drop, which tests can't drive). Suggested check: open SYM customization, edit the emoji layer, hold Q's emoji and drag it onto M: the two swap, and the emoji layer on the keyboard shows the change. Do the same on the symbols layer, and check a tap still opens the picker.
