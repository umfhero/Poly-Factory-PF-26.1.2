package net.umf.polyfactory;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.umf.polyfactory.block.FabricatorBlock;
import net.umf.polyfactory.block.entity.ModBlockEntities;
import net.umf.polyfactory.block.entity.ModCapabilities;
import net.umf.polyfactory.gui.ModMenuTypes;
import net.umf.polyfactory.network.ModNetwork;
import net.umf.polyfactory.recipe.ModRecipes;

@Mod(PolyFactory.MODID)
public class PolyFactory {
    public static final String MODID = "polyfactory";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<FabricatorBlock> FABRICATOR = BLOCKS.registerBlock("fabricator",
            FabricatorBlock::new,
            properties -> properties.mapColor(MapColor.METAL)
                    .strength(3.5f)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL));

    public static final DeferredItem<BlockItem> FABRICATOR_ITEM = ITEMS.registerSimpleBlockItem("fabricator", FABRICATOR);

    public static final DeferredItem<Item> UPGRADE_ITEM = ITEMS.registerSimpleItem("upgrade");
    public static final DeferredItem<Item> UPGRADE_SPEED_ITEM = ITEMS.registerSimpleItem("upgrade_speed");
    public static final DeferredItem<Item> UPGRADE_ENERGY_ITEM = ITEMS.registerSimpleItem("upgrade_energy");
    public static final DeferredItem<Item> UPGRADE_SLOTS_ITEM = ITEMS.registerSimpleItem("upgrade_slots");
    public static final DeferredItem<Item> UPGRADE_FLUID_ITEM = ITEMS.registerSimpleItem("upgrade_fluid");

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> POLY_FACTORY_TAB =
            CREATIVE_MODE_TABS.register("polyfactory_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.polyfactory"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> FABRICATOR_ITEM.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(FABRICATOR_ITEM.get());
                        output.accept(UPGRADE_ITEM.get());
                        output.accept(UPGRADE_SPEED_ITEM.get());
                        output.accept(UPGRADE_ENERGY_ITEM.get());
                        output.accept(UPGRADE_SLOTS_ITEM.get());
                        output.accept(UPGRADE_FLUID_ITEM.get());
                    })
                    .build());

    public PolyFactory(IEventBus modEventBus, ModContainer modContainer) {
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModRecipes.register(modEventBus);
        modEventBus.addListener(ModCapabilities::register);
        modEventBus.addListener(ModNetwork::register);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
