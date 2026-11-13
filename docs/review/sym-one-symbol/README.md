# Tap SYM for one symbol

A switch in Settings → Modifiers, with the SYM settings: **Tap SYM for one symbol** (off by default). A tap on SYM makes the next key type its symbol, as holding SYM does, without opening the symbols; a second tap opens them. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

- Tap SYM, then a key: that key's symbol, from the page SYM chords use, and the next key is a letter again. A key with no symbol types nothing extra and ends it.
- Tap SYM twice: the symbols open, as one tap does with the switch off. Back after one tap cancels it, as Back closes a page.
- Holding SYM and pressing a key works as before. With the switch on, SYM held past the long-press time without choosing anything just lets go, instead of opening the symbols.
- SYM's LED is lit while it waits for the key. Leaving the field ends it.
- The on-screen SYM key and the switch off: as before.

## Implementation boundaries

- `inputmethod/SymOneSymbol` holds the switch (`sym_sticky_tap`, registered in `BackupContract`) and the waiting state.
- `PhysicalKeyboardInputMethodService`: SYM's key up arms it instead of toggling the page; the key after it goes through `SymLayoutController.resolveChordSymbol`, as a SYM chord does; Back cancels it; `resetModifierStates` ends it; the status snapshot shows SYM pressed while it waits.
- A row in the Modifiers screen and a settings-search link (`modifiers.sym_sticky`); two strings in their own file with all ten translations.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*SymOneSymbolTest'
```

`SymOneSymbolTest` (in `keys-modifiers`), on the real keys: off by default and backed up, with SYM opening the symbols; a tap then a key types that key's symbol, once; a second tap opens the symbols; Back cancels; a long hold lets go; holding SYM as a chord unchanged.

## Validation

VALIDATION_PLACEHOLDER

## Device check (still to do)

Not yet run on a device. Suggested check with the switch on: tap SYM and press Q (its symbol is typed, the symbols stay closed, the LED goes off); tap SYM twice (the symbols open); tap SYM then Back (nothing typed); hold SYM for a second and let go (nothing happens).
