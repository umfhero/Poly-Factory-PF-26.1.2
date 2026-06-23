# Adding Fabricator Recipes

The Fabricator processes data-driven recipes of type `polyfactory:fabricating`. Each recipe is a
plain JSON file — no Java code needed to add one, and no extra step is needed for it to show up
in JEI (see [Why nothing else is required](#why-nothing-else-is-required) below).

## File location

```
src/main/resources/data/polyfactory/recipe/<any_name>.json
```

`<any_name>` just needs to be unique inside that folder — it doesn't have to match the input or
output item id. Existing example: `iron_ingot_from_raw_iron.json`.

## Schema

```json
{
  "type": "polyfactory:fabricating",
  "ingredient": "minecraft:raw_iron",
  "result": {
    "id": "minecraft:iron_ingot",
    "count": 1
  },
  "processing_time": 60,
  "energy_per_tick": 20
}
```

| Field | Required | Default | Meaning |
|---|---|---|---|
| `type` | yes | — | Must be exactly `"polyfactory:fabricating"`. |
| `ingredient` | yes | — | What the input slot accepts. See [Ingredient formats](#ingredient-formats). |
| `result.id` | yes | — | Item id placed in the output slot once processing finishes. |
| `result.count` | yes | — | How many of `result.id` to produce. Always set this explicitly. |
| `processing_time` | no | `100` | Ticks needed to finish one operation (20 ticks = 1 second). |
| `energy_per_tick` | no | `20` | FE drained from the Fabricator's buffer each tick while running. |
| `fluid_ingredient` | no | *(none)* | A fluid id, e.g. `"minecraft:lava"`. If set, the Fabricator's tank must hold at least `fluid_amount` mB of exactly this fluid for the recipe to run. Omit entirely for recipes that don't need a fluid. |
| `fluid_amount` | no | `0` | mB of `fluid_ingredient` consumed per craft (1000 mB = 1 bucket). Only meaningful if `fluid_ingredient` is set - always set both together. |

Example with a fluid requirement (cobblestone + 2 buckets of lava → obsidian):

```json
{
  "type": "polyfactory:fabricating",
  "ingredient": "minecraft:cobblestone",
  "fluid_ingredient": "minecraft:lava",
  "fluid_amount": 2000,
  "result": { "id": "minecraft:obsidian", "count": 1 },
  "processing_time": 100,
  "energy_per_tick": 40
}
```

**Total energy cost per craft = `energy_per_tick × processing_time`.** This total, plus the time
in seconds, is exactly what JEI's "uses" lookup on the Fabricator displays per recipe — e.g. the
existing raw iron recipe (`60` ticks × `20` FE/t) shows as "Energy: 1,200 FE / Time: 3.0s".

## Ingredient formats

`ingredient` accepts any of the standard vanilla/NeoForge ingredient shapes:

- A single item id: `"minecraft:raw_iron"`
- A tag (any item in the tag matches): `"#minecraft:logs"`
- A list of alternatives (any one matches): `["minecraft:raw_iron", "minecraft:raw_gold"]`

## Balancing against the machine

Check `Config.FABRICATOR_ENERGY_CAPACITY` / `Config.FABRICATOR_MAX_ENERGY_INSERT` (in
[Config.java](src/main/java/net/umf/polyfactory/Config.java)) before picking `energy_per_tick` —
a recipe that needs more FE/tick than the machine can ever pull in (before any Energy Upgrades
are installed) will stall every lane indefinitely instead of just running slowly.

If you set `fluid_ingredient`, also check `Config.FABRICATOR_FLUID_CAPACITY` — a `fluid_amount`
larger than the base tank capacity means the recipe is unusable until the player installs a Fluid
Upgrade (each level triples capacity, same scaling as Energy Upgrades).

## Testing a new recipe

1. Add the JSON file as above.
2. If you already have a world open, run `/reload` in-game to pick it up without restarting.
   Otherwise just launch normally (`./gradlew runClient`).
3. Check `run/logs/debug.log` for `Loaded N recipes` — if `N` didn't go up by one, your file
   failed to parse. Search the log for your filename or item id; bad item ids and JSON syntax
   errors are logged as warnings/errors there and the recipe is silently skipped otherwise.
4. Confirm in JEI: look up the input item's "recipes" or the Fabricator's "uses" — the new entry
   should appear automatically (see below).

## Why nothing else is required

`ModRecipeSync` ([ModRecipeSync.java](src/main/java/net/umf/polyfactory/recipe/ModRecipeSync.java))
tells NeoForge to sync the whole `polyfactory:fabricating` **type** to the client, and
`PolyFactoryJeiPlugin` ([PolyFactoryJeiPlugin.java](src/main/java/net/umf/polyfactory/jei/PolyFactoryJeiPlugin.java))
pulls every recipe of that type into JEI. Both are wired to the recipe *type*, not to individual
recipes, so any JSON file you add under that type is picked up automatically — only adding a
genuinely new recipe *type* (not just a new recipe) would need a Java change.

## Current limitations

- **One item ingredient, one optional fluid ingredient, one output per recipe.**
  `FabricatingRecipe` extends vanilla's `SingleItemRecipe` for the item side, with the fluid
  requirement checked separately - so multiple item ingredients or multiple outputs still aren't
  supported without a bigger change to the recipe type (tracked in `overview.md`'s "Possible
  future extensions").
- **No declared recipe priority.** If two recipes' `ingredient`s overlap, which one the Fabricator
  picks for a given input item isn't controlled — avoid overlapping ingredients across files.
