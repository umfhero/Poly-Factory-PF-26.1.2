package net.umf.polyfactory.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.umf.polyfactory.Config;
import net.umf.polyfactory.PolyFactory;
import net.umf.polyfactory.block.FabricatorBlock;
import net.umf.polyfactory.block.UpgradeType;
import net.umf.polyfactory.gui.FabricatorMenu;
import net.umf.polyfactory.recipe.FabricatingRecipe;
import net.umf.polyfactory.recipe.ModRecipes;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Block entity for the Fabricator. Holds a multi-lane item inventory (see
 * {@link FabricatorItemHandler}) and an internal FE buffer, and drives each unlocked lane's
 * input -> output {@link FabricatingRecipe} processing every server tick.
 * <p>
 * Progress is tracked in <b>energy units accumulated</b> toward a recipe's fixed total cost
 * ({@code energyPerTick * processingTime}), not in ticks elapsed. A Speed Upgrade shortens the
 * nominal number of ticks that total should be paid off in (raising the energy needed per tick to
 * stay on schedule), but the energy buffer's transfer rate (raised by Energy Upgrades) still caps
 * how much can actually be extracted each tick - so a lane with maxed speed but unupgraded energy
 * throughput simply takes longer than its nominal time, "buffering" instead of stalling.
 */
public class FabricatorBlockEntity extends BlockEntity implements MenuProvider {

    private static final int[] SPEED_DIVISORS = {1, 4, 16, 64};
    private static final int SPLIT_INTERVAL_TICKS = 20;

    private final FabricatorItemHandler itemHandler = new FabricatorItemHandler();
    private final FabricatorIoView ioView = new FabricatorIoView(this.itemHandler);
    private final FabricatorEnergyHandler energyHandler = new FabricatorEnergyHandler(
            Config.FABRICATOR_ENERGY_CAPACITY.get(), Config.FABRICATOR_MAX_ENERGY_INSERT.get(), Config.FABRICATOR_MAX_ENERGY_INSERT.get());
    private final FabricatorFluidHandler fluidHandler = new FabricatorFluidHandler(Config.FABRICATOR_FLUID_CAPACITY.get());
    private final RecipeManager.CachedCheck<SingleRecipeInput, FabricatingRecipe> quickCheck =
            RecipeManager.createCheck(ModRecipes.FABRICATING_TYPE.get());

    private int speedLevel;
    private int energyLevel;
    private int slotLevel;
    private int fluidLevel;

    private final int[] laneProgress = new int[FabricatorItemHandler.MAX_LANES];
    private final int[] laneMaxProgress = new int[FabricatorItemHandler.MAX_LANES];
    private final boolean[] laneBlocked = new boolean[FabricatorItemHandler.MAX_LANES];
    private int splitTickCounter;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            if (index < FabricatorMenu.LANE_PROGRESS_COUNT) {
                return FabricatorBlockEntity.this.laneProgress[index];
            }
            if (index < FabricatorMenu.LANE_PROGRESS_COUNT * 2) {
                return FabricatorBlockEntity.this.laneMaxProgress[index - FabricatorMenu.LANE_PROGRESS_COUNT];
            }
            if (index == FabricatorMenu.DATA_ENERGY) {
                return FabricatorBlockEntity.this.energyHandler.getAmountAsInt();
            }
            if (index == FabricatorMenu.DATA_MAX_ENERGY) {
                return FabricatorBlockEntity.this.energyHandler.getCapacityAsInt();
            }
            if (index == FabricatorMenu.DATA_SPLIT) {
                return FabricatorBlockEntity.this.itemHandler.isSplitInputs() ? 1 : 0;
            }
            if (index == FabricatorMenu.DATA_FLUID_ID) {
                return BuiltInRegistries.FLUID.getId(FabricatorBlockEntity.this.fluidHandler.getResource(0).getFluid());
            }
            if (index == FabricatorMenu.DATA_FLUID_AMOUNT) {
                return FabricatorBlockEntity.this.fluidHandler.getAmountAsInt(0);
            }
            if (index == FabricatorMenu.DATA_FLUID_CAPACITY) {
                return FabricatorBlockEntity.this.fluidHandler.getCapacityAsInt(0, FabricatorBlockEntity.this.fluidHandler.getResource(0));
            }
            if (index >= FabricatorMenu.DATA_BLOCKED && index < FabricatorMenu.DATA_BLOCKED + FabricatorItemHandler.MAX_LANES) {
                return FabricatorBlockEntity.this.laneBlocked[index - FabricatorMenu.DATA_BLOCKED] ? 1 : 0;
            }
            return 0;
        }

        @Override
        public void set(int index, int value) {
            if (index < FabricatorMenu.LANE_PROGRESS_COUNT) {
                FabricatorBlockEntity.this.laneProgress[index] = value;
            } else if (index < FabricatorMenu.LANE_PROGRESS_COUNT * 2) {
                FabricatorBlockEntity.this.laneMaxProgress[index - FabricatorMenu.LANE_PROGRESS_COUNT] = value;
            } else if (index == FabricatorMenu.DATA_SPLIT) {
                FabricatorBlockEntity.this.itemHandler.setSplitInputs(value != 0);
            }
        }

        @Override
        public int getCount() {
            return FabricatorMenu.DATA_COUNT;
        }
    };

    public FabricatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FABRICATOR.get(), pos, state);
    }

    public FabricatorItemHandler getItemHandler() {
        return this.itemHandler;
    }

    public FabricatorIoView getIoView() {
        return this.ioView;
    }

    public FabricatorEnergyHandler getEnergyHandler() {
        return this.energyHandler;
    }

    public FabricatorFluidHandler getFluidHandler() {
        return this.fluidHandler;
    }

    /** Voids whatever fluid is currently in the tank (the GUI's "Trash" button). */
    public void clearFluid() {
        this.fluidHandler.set(0, FluidResource.EMPTY, 0);
        this.setChanged();
    }

    public ContainerData getContainerData() {
        return this.dataAccess;
    }

    public int getActiveLanes() {
        return 1 + this.slotLevel;
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

    public int getUpgradeLevel(UpgradeType type) {
        return switch (type) {
            case SPEED -> this.speedLevel;
            case ENERGY -> this.energyLevel;
            case SLOTS -> this.slotLevel;
            case FLUID -> this.fluidLevel;
        };
    }

    public boolean isSplitInputs() {
        return this.itemHandler.isSplitInputs();
    }

    public void toggleSplitInputs() {
        this.itemHandler.setSplitInputs(!this.itemHandler.isSplitInputs());
        this.setChanged();
    }

    public void writeMenuData(RegistryFriendlyByteBuf buf, BlockPos pos) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(this.getActiveLanes());
        buf.writeVarInt(this.speedLevel);
        buf.writeVarInt(this.energyLevel);
        buf.writeVarInt(this.slotLevel);
        buf.writeVarInt(this.fluidLevel);
    }

    /**
     * Installs one level of the given upgrade. Returns {@code false} (consuming nothing) if that
     * upgrade is already at {@link UpgradeType#MAX_LEVEL}.
     */
    public boolean applyUpgrade(UpgradeType type) {
        switch (type) {
            case SPEED -> {
                if (this.speedLevel >= UpgradeType.MAX_LEVEL) {
                    return false;
                }
                this.speedLevel++;
            }
            case ENERGY -> {
                if (this.energyLevel >= UpgradeType.MAX_LEVEL) {
                    return false;
                }
                this.energyLevel++;
                this.recomputeEnergyLimits();
            }
            case SLOTS -> {
                if (this.slotLevel >= UpgradeType.MAX_LEVEL) {
                    return false;
                }
                this.slotLevel++;
                this.itemHandler.setActiveLanes(this.getActiveLanes());
                if (this.level != null) {
                    BlockState state = this.getBlockState();
                    this.level.setBlock(this.worldPosition, state.setValue(FabricatorBlock.SLOT_TIER, this.slotLevel), 3);
                }
            }
            case FLUID -> {
                if (this.fluidLevel >= UpgradeType.MAX_LEVEL) {
                    return false;
                }
                this.fluidLevel++;
                this.recomputeFluidLimits();
            }
        }
        this.setChanged();
        return true;
    }

    private void recomputeEnergyLimits() {
        int multiplier = 1;
        for (int i = 0; i < this.energyLevel; i++) {
            multiplier *= 3;
        }
        int capacity = Config.FABRICATOR_ENERGY_CAPACITY.get() * multiplier;
        int rate = Config.FABRICATOR_MAX_ENERGY_INSERT.get() * multiplier;
        this.energyHandler.setLimits(capacity, rate, rate);
    }

    private void recomputeFluidLimits() {
        int multiplier = 1;
        for (int i = 0; i < this.fluidLevel; i++) {
            multiplier *= 3;
        }
        this.fluidHandler.setCapacity(Config.FABRICATOR_FLUID_CAPACITY.get() * multiplier);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, FabricatorBlockEntity be) {
        boolean changed = false;
        boolean anyActive = false;
        int activeLanes = be.getActiveLanes();

        be.splitTickCounter++;
        if (be.splitTickCounter >= SPLIT_INTERVAL_TICKS) {
            be.splitTickCounter = 0;
            if (be.itemHandler.rebalanceInputs()) {
                changed = true;
            }
        }

        for (int lane = 0; lane < activeLanes; lane++) {
            if (tickLane(level, be, lane)) {
                anyActive = true;
                changed = true;
            }
        }

        boolean wasActive = state.getValue(FabricatorBlock.ACTIVE);
        if (wasActive != anyActive) {
            level.setBlock(pos, state.setValue(FabricatorBlock.ACTIVE, anyActive), 3);
            changed = true;
        }

        if (changed) {
            be.setChanged();
        }
    }

    /** @return whether this lane is actively processing (drew energy) this tick. */
    private static boolean tickLane(ServerLevel level, FabricatorBlockEntity be, int lane) {
        int inputSlot = FabricatorItemHandler.inputSlot(lane);
        int outputSlot = FabricatorItemHandler.outputSlot(lane);

        ItemResource inputResource = be.itemHandler.getResource(inputSlot);
        long inputAmount = be.itemHandler.getAmountAsLong(inputSlot);

        FabricatingRecipe recipe = null;
        ItemStack result = ItemStack.EMPTY;
        if (!inputResource.isEmpty() && inputAmount > 0) {
            SingleRecipeInput recipeInput = new SingleRecipeInput(inputResource.toStack((int) inputAmount));
            Optional<RecipeHolder<FabricatingRecipe>> match = be.quickCheck.getRecipeFor(recipeInput, level);
            if (match.isPresent()) {
                recipe = match.get().value();
                result = recipe.assemble(recipeInput);
            }
        }

        if (recipe == null || result.isEmpty() || !hasRequiredFluid(be.fluidHandler, recipe)) {
            be.laneProgress[lane] = 0;
            be.laneMaxProgress[lane] = 0;
            be.laneBlocked[lane] = false;
            return false;
        }

        if (!canInsertOutput(be.itemHandler, outputSlot, result)) {
            be.laneProgress[lane] = 0;
            be.laneMaxProgress[lane] = 0;
            be.laneBlocked[lane] = true;
            return false;
        }
        be.laneBlocked[lane] = false;

        int totalCost = recipe.energyPerTick() * recipe.processingTime();
        int speedDivisor = SPEED_DIVISORS[be.speedLevel];
        int effectiveTicks = Math.max(1, Mth.ceil(recipe.processingTime() / (double) speedDivisor));
        int requiredPerTick = Math.max(1, Mth.ceil((totalCost - be.laneProgress[lane]) / (double) effectiveTicks));

        be.laneMaxProgress[lane] = totalCost;

        int extracted;
        try (Transaction tx = Transaction.openRoot()) {
            extracted = be.energyHandler.extract(requiredPerTick, tx);
            if (extracted > 0) {
                tx.commit();
            }
        }

        if (extracted <= 0) {
            return false;
        }

        be.laneProgress[lane] += extracted;
        if (be.laneProgress[lane] >= totalCost) {
            try (Transaction tx = Transaction.openRoot()) {
                be.itemHandler.extract(inputSlot, inputResource, 1, tx);
                tx.commit();
            }
            consumeFluid(be.fluidHandler, recipe);
            insertOutput(be.itemHandler, outputSlot, result);
            be.laneProgress[lane] = 0;
        }
        return true;
    }

    private static boolean hasRequiredFluid(FabricatorFluidHandler fluidHandler, FabricatingRecipe recipe) {
        Optional<Holder<Fluid>> required = recipe.fluidIngredient();
        if (required.isEmpty()) {
            return true;
        }
        return fluidHandler.getResource(0).typeHolder().equals(required.get())
                && fluidHandler.getAmountAsLong(0) >= recipe.fluidAmount();
    }

    private static void consumeFluid(FabricatorFluidHandler fluidHandler, FabricatingRecipe recipe) {
        if (recipe.fluidIngredient().isEmpty()) {
            return;
        }
        FluidResource tankResource = fluidHandler.getResource(0);
        try (Transaction tx = Transaction.openRoot()) {
            fluidHandler.extract(0, tankResource, recipe.fluidAmount(), tx);
            tx.commit();
        }
    }

    private static boolean canInsertOutput(FabricatorItemHandler handler, int outputSlot, ItemStack result) {
        ItemResource resultResource = ItemResource.of(result);
        ItemResource currentOutput = handler.getResource(outputSlot);
        if (currentOutput.isEmpty()) {
            return true;
        }
        if (!currentOutput.equals(resultResource)) {
            return false;
        }
        // Not handler.getCapacityAsLong(...): that's gated by isValid(), which is deliberately false
        // for output slots (to reject external/GUI inserts there) and would always report 0 room.
        long currentAmount = handler.getAmountAsLong(outputSlot);
        return currentAmount + result.getCount() <= resultResource.getMaxStackSize();
    }

    private static void insertOutput(FabricatorItemHandler handler, int outputSlot, ItemStack result) {
        ItemResource resultResource = ItemResource.of(result);
        long currentAmount = handler.getAmountAsLong(outputSlot);
        handler.set(outputSlot, resultResource, (int) (currentAmount + result.getCount()));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.itemHandler.deserialize(input.childOrEmpty("items"));
        this.energyHandler.deserialize(input.childOrEmpty("energy"));
        this.fluidHandler.deserialize(input.childOrEmpty("fluid"));
        this.speedLevel = Math.min(UpgradeType.MAX_LEVEL, input.getIntOr("speed_level", 0));
        this.energyLevel = Math.min(UpgradeType.MAX_LEVEL, input.getIntOr("energy_level", 0));
        this.slotLevel = Math.min(UpgradeType.MAX_LEVEL, input.getIntOr("slot_level", 0));
        this.fluidLevel = Math.min(UpgradeType.MAX_LEVEL, input.getIntOr("fluid_level", 0));
        this.itemHandler.setActiveLanes(this.getActiveLanes());
        this.itemHandler.setSplitInputs(input.getBooleanOr("split_inputs", false));
        this.recomputeEnergyLimits();
        this.recomputeFluidLimits();
        for (int lane = 0; lane < FabricatorItemHandler.MAX_LANES; lane++) {
            this.laneProgress[lane] = input.getIntOr("progress_" + lane, 0);
            this.laneMaxProgress[lane] = input.getIntOr("max_progress_" + lane, 0);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.itemHandler.serialize(output.child("items"));
        this.energyHandler.serialize(output.child("energy"));
        this.fluidHandler.serialize(output.child("fluid"));
        output.putInt("speed_level", this.speedLevel);
        output.putInt("energy_level", this.energyLevel);
        output.putInt("slot_level", this.slotLevel);
        output.putInt("fluid_level", this.fluidLevel);
        output.putBoolean("split_inputs", this.itemHandler.isSplitInputs());
        for (int lane = 0; lane < FabricatorItemHandler.MAX_LANES; lane++) {
            output.putInt("progress_" + lane, this.laneProgress[lane]);
            output.putInt("max_progress_" + lane, this.laneMaxProgress[lane]);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.polyfactory.fabricator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return FabricatorMenu.createServerMenu(containerId, playerInv, this.worldPosition);
    }
}
