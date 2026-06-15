package net.umf.simpledigitalstorage.block.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.umf.simpledigitalstorage.SimpleDigitalStorage;

/**
 * Deferred registration of all block entity types for Simple Digital Storage.
 */
public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SimpleDigitalStorage.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorageHubBlockEntity>> STORAGE_HUB =
            BLOCK_ENTITY_TYPES.register("storage_hub",
                    () -> new BlockEntityType<>(StorageHubBlockEntity::new, SimpleDigitalStorage.STORAGE_HUB.get()));

    private ModBlockEntities() {}

    public static void register(IEventBus bus) {
        BLOCK_ENTITY_TYPES.register(bus);
    }
}
