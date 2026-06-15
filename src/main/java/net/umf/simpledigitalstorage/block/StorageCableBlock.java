package net.umf.simpledigitalstorage.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * A cable block that visually and logically connects Storage Hubs to inventory blocks.
 * <p>
 * The cable is a thin pipe rendered using a multipart blockstate model. It has 6 boolean
 * blockstate properties (one per direction) that control which arm segments are visible.
 * The cable connects to other cables, storage hubs, and any block exposing an IItemHandler capability.
 */
public class StorageCableBlock extends Block {

    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    private static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = new EnumMap<>(Direction.class);
    static {
        PROPERTY_BY_DIRECTION.put(Direction.NORTH, NORTH);
        PROPERTY_BY_DIRECTION.put(Direction.SOUTH, SOUTH);
        PROPERTY_BY_DIRECTION.put(Direction.EAST, EAST);
        PROPERTY_BY_DIRECTION.put(Direction.WEST, WEST);
        PROPERTY_BY_DIRECTION.put(Direction.UP, UP);
        PROPERTY_BY_DIRECTION.put(Direction.DOWN, DOWN);
    }

    // Collision / selection shapes
    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape ARM_NORTH = Block.box(5, 5, 0, 11, 11, 5);
    private static final VoxelShape ARM_SOUTH = Block.box(5, 5, 11, 11, 11, 16);
    private static final VoxelShape ARM_EAST = Block.box(11, 5, 5, 16, 11, 11);
    private static final VoxelShape ARM_WEST = Block.box(0, 5, 5, 5, 11, 11);
    private static final VoxelShape ARM_UP = Block.box(5, 11, 5, 11, 16, 11);
    private static final VoxelShape ARM_DOWN = Block.box(5, 0, 5, 11, 5, 11);

    private static final Map<Direction, VoxelShape> ARM_SHAPES = new EnumMap<>(Direction.class);
    static {
        ARM_SHAPES.put(Direction.NORTH, ARM_NORTH);
        ARM_SHAPES.put(Direction.SOUTH, ARM_SOUTH);
        ARM_SHAPES.put(Direction.EAST, ARM_EAST);
        ARM_SHAPES.put(Direction.WEST, ARM_WEST);
        ARM_SHAPES.put(Direction.UP, ARM_UP);
        ARM_SHAPES.put(Direction.DOWN, ARM_DOWN);
    }

    public StorageCableBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return getConnectionState(context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction directionToNeighbour,
            BlockPos neighbourPos,
            BlockState neighbourState,
            net.minecraft.util.RandomSource random
    ) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(directionToNeighbour), canConnect(level, pos, directionToNeighbour));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction dir : Direction.values()) {
            if (state.getValue(PROPERTY_BY_DIRECTION.get(dir))) {
                shape = Shapes.or(shape, ARM_SHAPES.get(dir));
            }
        }
        return shape;
    }

    /**
     * Compute the full connection state for a given position by checking all 6 neighbors.
     */
    private BlockState getConnectionState(LevelReader level, BlockPos pos) {
        BlockState state = this.defaultBlockState();
        for (Direction dir : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(dir), canConnect(level, pos, dir));
        }
        return state;
    }

    /**
     * Determine whether this cable should visually connect in the given direction.
     * Connects to: other cables, storage hubs, and blocks with IItemHandler capability.
     */
    private boolean canConnect(LevelReader level, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);
        Block neighborBlock = neighborState.getBlock();

        // Always connect to cables and hubs
        if (neighborBlock instanceof StorageCableBlock || neighborBlock instanceof StorageHubBlock) {
            return true;
        }

        // Check for inventory capability (requires full Level)
        if (level instanceof Level realLevel) {
            return realLevel.getCapability(Capabilities.Item.BLOCK,
                    neighborPos, direction.getOpposite()) != null;
        }

        return false;
    }
}
