package net.umf.simpledigitalstorage;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.umf.simpledigitalstorage.block.StorageCableBlock;
import net.umf.simpledigitalstorage.block.StorageHubBlock;
import net.umf.simpledigitalstorage.block.entity.ModBlockEntities;
import net.umf.simpledigitalstorage.gui.ModMenuTypes;
import net.umf.simpledigitalstorage.network.ModNetwork;

@Mod(SimpleDigitalStorage.MODID)
public class SimpleDigitalStorage {
    public static final String MODID = "simpledigitalstorage";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<StorageHubBlock> STORAGE_HUB = BLOCKS.registerBlock("storage_hub",
            StorageHubBlock::new,
            properties -> properties.mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    public static final DeferredBlock<StorageCableBlock> STORAGE_CABLE = BLOCKS.registerBlock("storage_cable",
            StorageCableBlock::new,
            properties -> properties.mapColor(MapColor.COLOR_GRAY)
                    .strength(1.0f)
                    .sound(SoundType.METAL)
                    .noOcclusion());

    public static final DeferredItem<BlockItem> STORAGE_HUB_ITEM = ITEMS.registerSimpleBlockItem("storage_hub", STORAGE_HUB);
    public static final DeferredItem<BlockItem> STORAGE_CABLE_ITEM = ITEMS.registerSimpleBlockItem("storage_cable", STORAGE_CABLE);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SDS_TAB =
            CREATIVE_MODE_TABS.register("sds_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.simpledigitalstorage"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> STORAGE_HUB_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(STORAGE_HUB_ITEM.get());
                        output.accept(STORAGE_CABLE_ITEM.get());
                    }).build());

    public SimpleDigitalStorage(IEventBus modEventBus, ModContainer modContainer) {
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        modEventBus.addListener(ModNetwork::register);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }
}
