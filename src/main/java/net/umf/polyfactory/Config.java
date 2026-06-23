package net.umf.polyfactory;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common configuration for Poly Factory.
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue FABRICATOR_ENERGY_CAPACITY = BUILDER
            .comment(
                    "Base FE capacity of an unupgraded Fabricator (enough for ~5 crafts of the example recipe).",
                    "Each Energy Upgrade level multiplies this by 3 (so level 3 = 27x base).")
            .defineInRange("fabricatorEnergyCapacity", 6000, 1000, 1000000);

    public static final ModConfigSpec.IntValue FABRICATOR_MAX_ENERGY_INSERT = BUILDER
            .comment(
                    "Base FE/tick an unupgraded Fabricator can accept from external sources and draw for its own",
                    "processing. Each Energy Upgrade level multiplies this by 3 as well - a Fabricator with maxed",
                    "Speed and Slot upgrades but no Energy upgrades will be throughput-limited by this value and",
                    "process slower than its nominal \"instant\" speed.")
            .defineInRange("fabricatorMaxEnergyInsert", 200, 1, 100000);

    public static final ModConfigSpec.IntValue FABRICATOR_FLUID_CAPACITY = BUILDER
            .comment(
                    "Base fluid tank capacity (in mB) of an unupgraded Fabricator (4000 mB = 4 buckets).",
                    "Each Fluid Upgrade level multiplies this by 3 (so level 3 = 27x base), same as Energy Upgrades.")
            .defineInRange("fabricatorFluidCapacity", 4000, 1000, 1000000);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
