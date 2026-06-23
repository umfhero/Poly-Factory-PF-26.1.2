# Poly Factory — Mod Overview

> A small Mekanism-inspired processing mod for Minecraft 26.1.2 (NeoForge). Pipe an item in,
> let the machine process it using stored FE, pipe the result out.

---

## Vision

One block, one job: the **Fabricator** takes an item (and optionally a fluid) in, consumes FE
energy over time to process it according to a data-driven recipe, and places the result in its
output slot. Pipe mods (a separate mod the user already has) connect to the input/output via the
standard NeoForge item-transfer capability, to the energy buffer via the standard FE capability,
and to the fluid tank via the standard fluid-transfer capability.

---

## Core Block

| Block / Item | Purpose |
|---|---|
| **Fabricator** | Directional processing machine. Right-click opens a GUI with input/output slots, a progress arrow, an FE energy bar, and a fluid tank bar; right-click with a bucket fills/drains the tank directly. Lights up (`active=true` blockstate) while processing. |

There are no cables/pipes/networking in this mod — that's intentionally left to whichever pipe
mod the user pairs this with. The Fabricator just exposes standard capabilities on all 6 sides.

---

## Technical Architecture

```
[ Pipe mod ]  --item--> [ input slot ] --(FabricatingRecipe, draining FE/tick + fluid/craft)--> [ output slot ] --item--> [ Pipe mod ]
[ Power mod ] --FE-----------------------------> [ internal energy buffer ]
[ Pipe mod ]  --fluid/bucket-------------------> [ fluid tank ]
```

- **FabricatorBlock** — directional (`facing`) + `active` blockstate, `EntityBlock` with a server-side ticker.
  Right-clicking with a fluid container routes through `FluidUtil.interactWithFluidHandler`.
- **FabricatorBlockEntity** — owns a 2-slot `FabricatorItemHandler` (input/output, via NeoForge's
  modern `ResourceHandler<ItemResource>` transfer API), a `SimpleEnergyHandler` (FE buffer), and a
  1-slot `FabricatorFluidHandler` (tank, via `ResourceHandler<FluidResource>`). Each server tick it
  looks up a matching `FabricatingRecipe` via `RecipeManager.CachedCheck`, also checks the tank
  against the recipe's optional fluid requirement, drains `energy_per_tick` FE while processing,
  and after `processing_time` ticks consumes the item + fluid and moves the result into the
  output slot.
- **ModCapabilities** — registers `Capabilities.Item.BLOCK`, `Capabilities.Energy.BLOCK`, and
  `Capabilities.Fluid.BLOCK` for the block entity so any pipe/cable mod can insert into the input
  slot, extract from the output slot, push FE into the buffer, and push/pull fluid from any side.
- **FabricatingRecipe** — a `SingleItemRecipe` subclass (ingredient -> `ItemStackTemplate` result,
  plus `processing_time` ticks, `energy_per_tick` FE, and an optional `fluid_ingredient`/
  `fluid_amount` pair) loaded from datapack JSON under `data/polyfactory/recipe/`, type
  `polyfactory:fabricating`.
- **FabricatorMenu / FabricatorScreen** — GUI built on vanilla `ContainerData` (no custom
  networking needed for state sync; a `ClearFluidPacket` handles the "Trash" button); the screen
  is rendered programmatically (filled rectangles) since no GUI texture atlas is provided.

---

## Project Metadata

| Field | Value |
|---|---|
| Mod ID | `polyfactory` |
| Group | `net.umf.polyfactory` |
| Minecraft | 26.1.2 |
| NeoForge | 26.1.2.75 |
| Java | 25 |
| License | All Rights Reserved |

---

## Adding more recipes

See [recipe.md](recipe.md) for the full schema, ingredient formats, balancing notes, and how
testing/JEI pick up new recipes automatically.

---

## Possible future extensions

- [ ] Multiple machine tiers (faster processing / bigger energy buffer).
- [x] Fluid input requirement (see `fluid_ingredient`/`fluid_amount` in `recipe.md`) and Fluid Upgrade.
- [ ] Multi-item-input or multi-output recipes (would require moving off `SingleItemRecipe`).
- [x] JEI recipe display integration (see `net.umf.polyfactory.jei`).
- [ ] Per-side I/O configuration (currently any side can insert/extract/charge).

---

*Last updated: 2026-06-23 — rebuilt from the old Simple Digital Storage project into Poly Factory.*
