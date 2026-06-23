package net.umf.polyfactory.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ResourceHandlerSlot;
import net.umf.polyfactory.PolyFactory;
import net.umf.polyfactory.block.entity.FabricatorBlockEntity;
import net.umf.polyfactory.block.entity.FabricatorItemHandler;

/**
 * Server/client container for the Fabricator GUI: one input/output slot pair per unlocked lane
 * (1-{@value FabricatorItemHandler#MAX_LANES}, depending on the Slot Upgrade level), backed
 * directly by the block entity's {@link FabricatorItemHandler}, plus the player's inventory and
 * synced per-lane progress/energy/upgrade data.
 */
public class FabricatorMenu extends AbstractContainerMenu {

    public static final int MAX_LANES = FabricatorItemHandler.MAX_LANES;
    public static final int SLOT_SIZE = 16;
    public static final int LANE_INPUT_X = 52;
    public static final int LANE_OUTPUT_X = 104;
    public static final int LANE_Y_START = 26;
    public static final int LANE_HEIGHT = 20;

    public static final int LANE_PROGRESS_COUNT = MAX_LANES;
    public static final int DATA_ENERGY = LANE_PROGRESS_COUNT * 2;
    public static final int DATA_MAX_ENERGY = DATA_ENERGY + 1;
    public static final int DATA_SPLIT = DATA_MAX_ENERGY + 1;
    public static final int DATA_BLOCKED = DATA_SPLIT + 1;
    public static final int DATA_FLUID_ID = DATA_BLOCKED + MAX_LANES;
    public static final int DATA_FLUID_AMOUNT = DATA_FLUID_ID + 1;
    public static final int DATA_FLUID_CAPACITY = DATA_FLUID_AMOUNT + 1;
    public static final int DATA_COUNT = DATA_FLUID_CAPACITY + 1;

    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final BlockPos pos;
    private final int activeLanes;
    private final int speedLevel;
    private final int energyLevel;
    private final int slotLevel;
    private final int fluidLevel;

    private FabricatorMenu(int containerId, Inventory playerInv, BlockPos pos, FabricatorItemHandler itemHandler, ContainerData data,
                            int activeLanes, int speedLevel, int energyLevel, int slotLevel, int fluidLevel) {
        super(ModMenuTypes.FABRICATOR_MENU.get(), containerId);
        this.access = ContainerLevelAccess.create(playerInv.player.level(), pos);
        this.pos = pos;
        checkContainerDataCount(data, DATA_COUNT);
        this.data = data;
        this.activeLanes = activeLanes;
        this.speedLevel = speedLevel;
        this.energyLevel = energyLevel;
        this.slotLevel = slotLevel;
        this.fluidLevel = fluidLevel;

        for (int lane = 0; lane < activeLanes; lane++) {
            int y = LANE_Y_START + lane * LANE_HEIGHT;
            this.addSlot(new ResourceHandlerSlot(itemHandler, itemHandler::set, FabricatorItemHandler.inputSlot(lane), LANE_INPUT_X, y));
            this.addSlot(new ResourceHandlerSlot(itemHandler, itemHandler::set, FabricatorItemHandler.outputSlot(lane), LANE_OUTPUT_X, y));
        }
        this.addStandardInventorySlots(playerInv, 8, inventoryY(activeLanes));
        this.addDataSlots(data);
    }

    /**
     * Up to 4 upgrade icons (Speed/Energy/Slots/Fluid) get stacked and vertically centered next to
     * the lane area (see {@code FabricatorScreen#renderUpgradeIcons}). With few lanes that stack is
     * taller than the lanes themselves, so the lane area's height for layout purposes - title
     * clearance above, inventory label clearance below - has to be at least this, regardless of
     * how few lanes are actually unlocked.
     */
    public static final int MIN_LANE_AREA_HEIGHT = 64;

    /** Effective height of the lane area for vertical layout (see {@link #MIN_LANE_AREA_HEIGHT}). */
    public static int laneAreaHeight(int activeLanes) {
        return Math.max(activeLanes * LANE_HEIGHT, MIN_LANE_AREA_HEIGHT);
    }

    /** Top Y coordinate for the player inventory block, below however many lane rows are shown. */
    public static int inventoryY(int activeLanes) {
        return LANE_Y_START + laneAreaHeight(activeLanes) + 14;
    }

    public static int imageHeight(int activeLanes) {
        return inventoryY(activeLanes) + 90;
    }

    public static FabricatorMenu createServerMenu(int containerId, Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof FabricatorBlockEntity fabricator) {
            return new FabricatorMenu(containerId, playerInv, pos, fabricator.getItemHandler(), fabricator.getContainerData(),
                    fabricator.getActiveLanes(), fabricator.getSpeedLevel(), fabricator.getEnergyLevel(), fabricator.getSlotLevel(),
                    fabricator.getFluidLevel());
        }
        return new FabricatorMenu(containerId, playerInv, pos, new FabricatorItemHandler(), new SimpleContainerData(DATA_COUNT), 1, 0, 0, 0, 0);
    }

    public static FabricatorMenu clientFactory(int containerId, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int activeLanes = buf.readVarInt();
        int speedLevel = buf.readVarInt();
        int energyLevel = buf.readVarInt();
        int slotLevel = buf.readVarInt();
        int fluidLevel = buf.readVarInt();
        return new FabricatorMenu(containerId, playerInv, pos, new FabricatorItemHandler(), new SimpleContainerData(DATA_COUNT),
                activeLanes, speedLevel, energyLevel, slotLevel, fluidLevel);
    }

    public int getActiveLanes() {
        return this.activeLanes;
    }

    public int getSpeedLevel() {
        return this.speedLevel;
    }

    public int getEnergyLevel() {
        return this.energyLevel;
    }

    public int getSlotLevel() {
        return this.slotLevel;
    }

    public int getFluidLevel() {
        return this.fluidLevel;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public boolean isSplitInputs() {
        return this.data.get(DATA_SPLIT) != 0;
    }

    public boolean isBlocked(int lane) {
        return this.data.get(DATA_BLOCKED + lane) != 0;
    }

    public float getProgress(int lane) {
        int max = this.data.get(LANE_PROGRESS_COUNT + lane);
        int current = this.data.get(lane);
        return max != 0 ? Mth.clamp((float) current / max, 0.0F, 1.0F) : 0.0F;
    }

    public float getEnergyRatio() {
        int max = this.data.get(DATA_MAX_ENERGY);
        int current = this.data.get(DATA_ENERGY);
        return max != 0 ? Mth.clamp((float) current / max, 0.0F, 1.0F) : 0.0F;
    }

    public int getEnergyStored() {
        return this.data.get(DATA_ENERGY);
    }

    public int getMaxEnergyStored() {
        return this.data.get(DATA_MAX_ENERGY);
    }

    public FluidResource getFluidResource() {
        return BuiltInRegistries.FLUID.get(this.data.get(DATA_FLUID_ID))
                .map(FluidResource::of)
                .orElse(FluidResource.EMPTY);
    }

    public int getFluidAmount() {
        return this.data.get(DATA_FLUID_AMOUNT);
    }

    public int getFluidCapacity() {
        return this.data.get(DATA_FLUID_CAPACITY);
    }

    public float getFluidRatio() {
        int max = this.getFluidCapacity();
        return max != 0 ? Mth.clamp((float) this.getFluidAmount() / max, 0.0F, 1.0F) : 0.0F;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, PolyFactory.FABRICATOR.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = slot.getItem();
        ItemStack copy = stackInSlot.copy();
        int machineSlotCount = this.activeLanes * 2;

        if (index < machineSlotCount) {
            if (!this.moveItemStackTo(stackInSlot, machineSlotCount, machineSlotCount + 36, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stackInSlot, 0, machineSlotCount, false)) {
            return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stackInSlot.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stackInSlot);
        return copy;
    }
}
