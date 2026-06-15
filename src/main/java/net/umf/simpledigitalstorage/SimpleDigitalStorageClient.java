package net.umf.simpledigitalstorage;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.umf.simpledigitalstorage.gui.ModMenuTypes;
import net.umf.simpledigitalstorage.gui.StorageHubScreen;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = SimpleDigitalStorage.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = SimpleDigitalStorage.MODID, value = Dist.CLIENT)
public class SimpleDigitalStorageClient {
    public SimpleDigitalStorageClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.STORAGE_HUB_MENU.get(), StorageHubScreen::new);
    }
}
