package net.umf.polyfactory.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.umf.polyfactory.block.entity.FabricatorBlockEntity;
import net.umf.polyfactory.block.entity.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

/**
 * The Fabricator: a directional processing machine. Right-clicking with an upgrade item in hand
 * installs it; right-clicking with a fluid container fills/drains its tank; right-clicking with
 * anything else (or empty-handed) opens its GUI. Pipes from other mods interact with its
 * input/output slots, FE buffer, and fluid tank via the registered capabilities.
 */
public class FabricatorBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final IntegerProperty SLOT_TIER = IntegerProperty.create("slot_tier", 0, UpgradeType.MAX_LEVEL);

    public FabricatorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVE, false)
                .setValue(SLOT_TIER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE, SLOT_TIER);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        UpgradeType type = UpgradeType.fromItem(stack.getItem());
        if (type == null) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FabricatorBlockEntity fabricator
                    && FluidUtil.interactWithFluidHandler(player, hand, pos, fabricator.getFluidHandler(), null)) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FabricatorBlockEntity fabricator) {
                Component itemName = stack.getHoverName();
                if (fabricator.applyUpgrade(type)) {
                    stack.shrink(1);
                    level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.4F);
                    player.sendOverlayMessage(
                            Component.translatable("message.polyfactory.upgrade_applied",
                                    itemName, fabricator.getUpgradeLevel(type), UpgradeType.MAX_LEVEL));
                } else {
                    player.sendOverlayMessage(
                            Component.translatable("message.polyfactory.upgrade_maxed", itemName));
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FabricatorBlockEntity fabricator) {
                player.openMenu(fabricator, buf -> fabricator.writeMenuData(buf, pos));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FabricatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.FABRICATOR.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> FabricatorBlockEntity.serverTick((ServerLevel) lvl, pos, st, (FabricatorBlockEntity) be);
    }
}
