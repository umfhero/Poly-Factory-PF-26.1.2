package net.umf.polyfactory.block;

import net.minecraft.world.item.Item;
import net.umf.polyfactory.PolyFactory;
import org.jetbrains.annotations.Nullable;

/** The upgrade kinds a Fabricator can be shift-right-clicked with, each up to {@code MAX_LEVEL}. */
public enum UpgradeType {
    SPEED,
    ENERGY,
    SLOTS,
    FLUID;

    public static final int MAX_LEVEL = 3;

    @Nullable
    public static UpgradeType fromItem(Item item) {
        if (item == PolyFactory.UPGRADE_SPEED_ITEM.get()) {
            return SPEED;
        }
        if (item == PolyFactory.UPGRADE_ENERGY_ITEM.get()) {
            return ENERGY;
        }
        if (item == PolyFactory.UPGRADE_SLOTS_ITEM.get()) {
            return SLOTS;
        }
        if (item == PolyFactory.UPGRADE_FLUID_ITEM.get()) {
            return FLUID;
        }
        return null;
    }
}
