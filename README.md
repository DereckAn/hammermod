# The Hammer Mod: unofficial 26.3 Fabric port

A port of **The Hammer Mod** by **Wally Whip** from Minecraft 1.19.2 (Fabric, `beta4`) to **Minecraft 26.3 (Fabric)**.

> **Personal use only. Do not publish.** The original mod is *All Rights Reserved* and has no public source code.
> It was rebuilt from the decompiled 1.19.2 jar. Don't upload it to GitHub releases, CurseForge or Modrinth, and don't share the jar.

## Features

| | |
|---|---|
| **Hammers** | Pickaxe that mines an area (3x3 and up) |
| **Excavators** | Shovel that digs an area; makes paths |
| **Lumber axes** | Fell the whole log column above the mined log; strip logs |
| **Sickles** | Harvest and replant crops/plants in an area; till soil |
| **Paxels** | Pickaxe + axe + shovel in one; strip, till, or make paths while sneaking |
| **Handles** | Crafting ingredient |

Every tool comes in wooden, stone, iron, golden, diamond, netherite and copper. The mod's copper is its own material (759 durability, iron mining level), not vanilla copper.

**Enchantments** (enchanting table, villagers, loot):

| Enchantment | Max | Effect | Goes on |
|---|---|---|---|
| Diggers' AOE | V | +1 area step per level (3x3 → up to 13x13) | hammers, excavators, sickles, paxels |
| Diggers' Magnet | I | Drops go straight to your inventory | any mining tool |
| Diggers' Void | I | Drops are deleted | any mining tool |
| Miners' Melter | I | Drops are smelted | any mining tool, shears |
| Miners' Delight | I | Chance of a bonus ore when mining stone, deepslate or netherrack | pickaxes, hammers, paxels |
| Lumbers' Planker | I | Logs drop as planks | axes, lumber axes, paxels |
| Lumbers' Stripper | I | Logs drop as stripped logs | axes, lumber axes, paxels |

Void can't be combined with Magnet, Melter, Delight, Planker, Silk Touch or Fortune.

## How to use

- **Change the area size:** hold **sneak** and **scroll** while holding a tool. A new tool starts at 1x1; without Diggers' AOE the largest size is 3x3. The size is saved on the item.
- **Lumber axe:** sneak + scroll switches between *Lumber Mode* (whole tree) and *Single Block Mode*.
- **Mine a single block:** sneak while mining.
- The area is one block deep and lies flat against the face you hit.
- **Outline colours:** green = will be mined, orange = can't mine but can right-click it (strip/till), red = neither.
- **Creative:** the "The Hammer Mod" tab (on the second page of tabs) has every tool plus the 7 enchanted books. Tools also appear in *Tools & Utilities*.

## Config

`config/thehammermod.json`. It uses the same keys as 1.19.2, so an old file still works.

```json
{
  "primaryColour": 65280,
  "secondaryColour": 16747325,
  "nonColour": 10028289,
  "chanceMinersDelight": 5,
  "requiresSeeds": false
}
```

Colours are RGB integers. `chanceMinersDelight` is a percent. `requiresSeeds` makes sickle replanting cost a seed. There's an in-game editor only when **ModMenu** and **Cloth Config** are both installed; they are optional.

## Build and install

Requirements: Java 25+ to run Gradle (Gradle 9.8.2 wrapper, Loom 1.19.3). The build targets Java 25.

```bash
JAVA_HOME=/usr/lib/jvm/java-27-openjdk ./gradlew build
# -> build/libs/TheHammerMod-26.3-beta4.jar
```

Install into the CurseForge test profile (it only needs Fabric API):

```bash
python3 -I ~/.claude/skills/install-mod/install.py ~/Documents/curseforge/minecraft/Instances/test build/libs/TheHammerMod-26.3-beta4.jar
```

Or run `/install-mod` in Claude Code. Restart the game afterwards. Logs: `<profile>/logs/latest.log`.

## Project layout

```text
src/main/java/wallywhip/thehammermod/
  TheHammerMod.java        init, enchantment keys, mode component, scroll packet handler, shared break/recipe helpers
  ModItems.java            all 42 items + the copper tool material
  DropEnchantments.java    Void/Planker/Stripper/Delight/Melter/Magnet applied to block drops
  items/                   AoeToolItem (hammer, excavator, area logic), LumberAxeItem, SickleItem, PaxelItem
  client/                  AoeHighlight (outline + crack overlay), client entrypoint
  mixin/                   BlockMixin (drops), client/MouseHandlerMixin (scroll), client/BlockTransformerMixin (particles)
  network/                 ScrollModePayload
  config/, compat/         JSON config, ModMenu screen
src/main/resources/data/   enchantments (JSON), recipes, tags, advancements
```

## What changed from 1.19.2

Minecraft changed a lot between 1.19.2 and 26.3, so much of the mod was rewritten rather than renamed:

- **Enchantments** are data files (`data/thehammermod/enchantment/`); their effects are still code.
- **Tools** are plain items configured with components. They join vanilla tags (`#minecraft:pickaxes`, etc.) so vanilla enchantments apply.
- **Right-click actions** (strip, path, till) use vanilla's new data-driven block transformers.
- **Mode** is an item component (`thehammermod:mode`) instead of NBT, so old 1.19.2 items won't carry over (worlds can't be upgraded across that gap anyway).
- **Outline rendering** uses Fabric render events instead of a renderer mixin.

Behaviour differences:

1. Which neighbours a hammer or excavator breaks: 1.19.2 compared block *Materials*, which no longer exist. Now a neighbour breaks if the tool is effective on both blocks and the hardness is similar. If the tool isn't effective on the clicked block, only the same block type breaks.
2. Lumbers' Stripper finds the stripped log by name (`oak_log` → `stripped_oak_log`). It also works on paxels now; in 1.19.2 it would have crashed with them.
3. Melter and Planker scale with stack size (3 raw iron → 3 ingots; 1.19.2 gave 1).
4. Kept from 1.19.2, though it looks like a bug: Miners' Delight's bonus ore is smelted when Melter is **not** on the tool.
5. The "energy" tools never existed in 1.19.2 beta4 (textures only), so they don't exist here either.

## Credits

All design, textures and original code: **Wally Whip**. This port only updates it to run on 26.3.
