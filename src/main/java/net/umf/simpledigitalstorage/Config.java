package net.umf.simpledigitalstorage;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common configuration for Simple Digital Storage.
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue MAX_CABLE_RANGE = BUILDER
            .comment("Maximum number of cable blocks the network scanner will traverse from a hub.")
            .defineInRange("maxCableRange", 32, 1, 256);

    static final ModConfigSpec SPEC = BUILDER.build();
}
