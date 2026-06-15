package net.umf.simpledigitalstorage.gui;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.umf.simpledigitalstorage.SimpleDigitalStorage;

/**
 * Deferred registration of all menu (container) types for Simple Digital Storage.
 */
public final class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, SimpleDigitalStorage.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<StorageHubMenu>> STORAGE_HUB_MENU =
            MENU_TYPES.register("storage_hub_menu",
                    () -> IMenuTypeExtension.create(StorageHubMenu::clientFactory));

    private ModMenuTypes() {}

    public static void register(IEventBus bus) {
        MENU_TYPES.register(bus);
    }
}
