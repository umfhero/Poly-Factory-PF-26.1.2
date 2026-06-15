# Simple Digital Storage (SDS) — Mod Overview

> A lightweight, beginner-friendly alternative to Applied Energistics and Refined Storage for Minecraft 26.1.2 (NeoForge).

---

## Vision

Simple Digital Storage gives players a **no-nonsense digital inventory system**.  
No channels, no power grids, no crafting CPUs — just **plug containers into a hub and access everything from one screen**.

---

## Core Blocks & Items

| Block / Item | Purpose |
|---|---|
| **Storage Hub** | Central block. All attached inventories are aggregated through this block. |
| **Storage Cable** | Connects inventories (chests, barrels, etc.) to a Storage Hub. Transmits inventory data. |
| **Storage Interface** | Placed onto a Storage Hub face. Right-clicking opens the unified GUI. |

> **That's it.** Three things to craft, place, and use. Simplicity is the entire point.

---

## Interface (GUI) Features

The Storage Interface screen provides a single, unified view of every item across all connected inventories.

### Default Features

- [ ] **Unified item grid** — displays all items from all connected containers in one scrollable grid.
- [ ] **Search bar** — real-time text search to filter items by name.
- [ ] **Sort options** — sort by:
  - [ ] Name (A → Z / Z → A)
  - [ ] Quantity (smallest → largest / largest → smallest)
  - [ ] Mod / namespace
- [ ] **Insert items** — click or shift-click items from the player inventory into the network; items route to an available slot in a connected container.
- [ ] **Extract items** — click or shift-click items in the grid to pull them into the player inventory.

### Stretch / Future Features

- [ ] **Crafting grid overlay** — optional 3×3 crafting grid inside the interface.
- [ ] **Favourites / pinned items** — pin frequently-used items to the top of the grid.
- [ ] **Per-container labels** — tooltip showing which physical container an item lives in.
- [ ] **Wireless Interface item** — access the network without standing next to the hub.
- [ ] **Capacity upgrades** — hub tiers that allow more connected containers.
- [ ] **Import/Export Bus** — auto-push / auto-pull items between the network and external inventories.

---

## Technical Architecture (High Level)

```
[ Chest ] ──cable──┐
[ Barrel ] ──cable──┤
[ Chest ] ──cable──┼── [ Storage Hub ] ◄── [ Storage Interface ]
[ Shulker ] ──cable─┘         ▲                     │
                              │                     │
                     aggregates all              opens GUI
                     IItemHandler caps       showing merged inv
```

1. **Storage Hub (BlockEntity)** scans for connected cables & inventories every time the network changes.
2. **Storage Cable** is a simple connection block (like redstone dust but for inventories). No BlockEntity needed — just blockstate connectivity.
3. **Storage Interface (BlockEntity + Menu + Screen)** queries the Hub for the aggregated inventory and presents it in a GUI.

### Network Discovery

- On cable/container place/break → Hub re-scans via BFS/flood-fill along cables.
- Each connected block exposing `IItemHandler` capability is added to the Hub's tracked inventory list.
- The Hub merges all `IItemHandler` instances into a single virtual view.

---

## Development Phases

### Phase 1 — Foundation (Completed)
- [x] Remove template example block/item/tab boilerplate
- [x] Register **Storage Hub** block + block entity
- [x] Register **Storage Cable** block (with directional connectivity blockstates)
- [x] Register a **Creative Tab** for SDS items
- [x] Add basic block models & textures
- [x] Add lang entries
- [x] Basic networking: Hub scans for `ResourceHandler<ItemResource>` via BFS flood-fill

### Phase 2 — Unified Grid & UI Enhancements ✦ *current target*
- [ ] Overhaul `StorageNetworkScanner` to return raw `ResourceHandler`s instead of legacy wrappers (Fixes `ClassCastException`).
- [ ] Implement custom `PayloadRegistrar` packets (`SyncNetworkItemsPacket`, `ExtractItemPacket`).
- [ ] Rewrite `StorageHubMenu` to only use vanilla slots for the player's inventory, abandoning fake slots for the network.
- [ ] Rewrite `StorageHubScreen` to render a custom scrollable, virtual grid of synced items.
- [ ] Add real-time Search Bar (using vanilla `EditBox`).
- [ ] Add Sort capabilities (by name, quantity).
- [ ] Implement backend routing: Left/Right click grid for extraction, Shift-click player inventory for insertion.

### Phase 3 — Polish & Extras
- [ ] Proper textures and block models
- [ ] Crafting recipes for blocks
- [ ] Config options (max cable range, etc.)
- [ ] Sound effects for insert/extract

---

## Change Log & Technical Pivots

### 2026-06-11 — Phase 2 Pivot (Unified Grid)
**The Problem:** The initial Phase 1 implementation attempted to map the modern NeoForge 1.21.2 `ResourceHandler` capabilities into legacy `SlotItemHandler`s. This caused a `ClassCastException` during item extraction because the adapter did not implement `IItemHandlerModifiable`. Furthermore, mapping hundreds of chest slots 1:1 onto a static vanilla container proved inflexible, causing the UI to break when multiple chests were connected due to vanilla's 54-slot limit. 
**The Solution:** Phase 2 was pivoted to abandon vanilla slots for the network inventory. Instead, the server aggregates items and syncs them to the client via custom packets. The client renders a custom virtual grid (like AE2/Refined Storage) that supports unlimited slots, search, and sorting.

### 2026-06-11 — Block Registration NPE Fix
Fixed a severe `NullPointerException: Block id not set` during startup. In NeoForge 1.21.2+, modifying `BlockBehaviour.Properties` (like setting drops) without an ID causes a crash. Fixed by switching from the legacy `BLOCKS.register()` to the modern `BLOCKS.registerBlock()` which automatically injects the `ResourceKey` during instantiation.

### 2026-06-11 - 1.21.2 API Migration & Compilation Fixes
**The Problem:** The transition to NeoForge 1.21.2 introduced significant breaking changes in the rendering pipeline and networking API. `Screen.render` was renamed to `extractContents` and `GuiGraphics` became `GuiGraphicsExtractor`. Methods like `drawString` were replaced by `text`, and `renderItem` by `item`. Networking packets required transition to `CustomPacketPayload` using `Identifier` instead of `ResourceLocation`. The `EventBusSubscriber` annotation also faced changes.
**The Solution:** Audited the `minecraft-patched-26.1.2.75` decompiled sources (`AbstractContainerScreen`, `GuiGraphicsExtractor`, `InputWithModifiers`) to map the new 1.21.2 API signatures. Replaced legacy rendering calls with their modern equivalents, updated packet registration to use `Identifier.fromNamespaceAndPath`, and fixed `MouseButtonEvent` overrides for inputs. Resolved all compilation errors and verified a successful build.

---

## Project Metadata

| Field | Value |
|---|---|
| Mod ID | `simpledigitalstorage` |
| Group | `net.umf.simpledigitalstorage` |
| Minecraft | 26.1.2 |
| NeoForge | 26.1.2.75 |
| Java | 25 |
| License | All Rights Reserved |

---

*Last updated: 2026-06-11*
