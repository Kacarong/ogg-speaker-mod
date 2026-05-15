# OGG Speaker Mod

Minecraft **Fabric** mod for **Minecraft 26.1.2** that adds a black **Speaker block** which plays sounds at its position. Use it as a server-side jukebox for custom `.ogg` files (via resource pack overrides) or any registered `SoundEvent`.

## Features

- **Speaker block** — placed in-world, command-controlled (no GUI).
- **32 mod sound slots** (`oggspeaker:slot1` … `oggspeaker:slot32`) you can override with your own `.ogg` files in a resource pack.
- Accepts **any registered SoundEvent ID** (e.g. `minecraft:block.note_block.bell`).
- Customizable **volume** (0.0–16.0), **pitch** (0.5–2.0), and **range** (1–256 blocks, default 16).
- **Redstone trigger**: a rising edge replays the last sound the speaker played.
- `/speaker stop` cuts playback near the speaker.

## Commands (OP level 2+)

| Command | Description |
|---|---|
| `/speaker give` | Gives the player one Speaker block. |
| `/speaker list` | Lists the 32 mod-defined sound slot IDs. |
| `/speaker play <pos> <sound> [volume] [pitch] [range]` | Plays a sound at the given speaker. |
| `/speaker stop <pos>` | Stops the last sound the speaker played. |

## Resource pack — overriding sound slots

Place your `.ogg` files in your resource pack at:

```
assets/oggspeaker/sounds/slot1.ogg
assets/oggspeaker/sounds/slot2.ogg
...
assets/oggspeaker/sounds/slot32.ogg
```

The mod ships a `sounds.json` that maps each slot to `oggspeaker:slot<N>.ogg`. Your resource pack's `.ogg` files take precedence with no additional JSON.

## Build

Requires **Java 25**.

```bash
./gradlew build
```

The jar is written to `build/libs/oggspeaker-<version>.jar`.

## Requirements

- Minecraft **26.1.2**
- Fabric Loader **0.19.2+**
- Fabric API **0.145.4+26.1.2** or newer
