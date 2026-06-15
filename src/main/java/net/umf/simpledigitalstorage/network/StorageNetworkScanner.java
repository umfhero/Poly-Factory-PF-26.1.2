package net.umf.simpledigitalstorage.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.umf.simpledigitalstorage.block.StorageCableBlock;
import net.umf.simpledigitalstorage.block.StorageHubBlock;

import java.util.*;

/**
 * BFS flood-fill scanner that discovers inventories connected to a Storage Hub via cables.
 * <p>
 * Starting at the hub position, the scanner traverses through adjacent StorageCable blocks.
 * At every visited position (hub or cable), it checks all 6 neighbors for IItemHandler capabilities.
 * Blocks with inventories are added to the result list but are NOT traversed through — only cables
 * carry the network.
 */
public final class StorageNetworkScanner {

    private StorageNetworkScanner() {}

    /**
     * Scans the network originating from {@code hubPos}.
     *
     * @param level  the server level
     * @param hubPos position of the Storage Hub block
     * @return list of all discovered ResourceHandler instances (never null, may be empty)
     */
    public static List<ResourceHandler<ItemResource>> scan(Level level, BlockPos hubPos) {
        List<ResourceHandler<ItemResource>> handlers = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        visited.add(hubPos);
        queue.add(hubPos);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);
                if (visited.contains(neighbor)) continue;

                BlockState neighborState = level.getBlockState(neighbor);

                if (neighborState.getBlock() instanceof StorageCableBlock) {
                    // Cable — continue BFS through it
                    visited.add(neighbor);
                    queue.add(neighbor);
                } else if (!(neighborState.getBlock() instanceof StorageHubBlock)) {
                    // Not a cable and not another hub — check for inventory capability
                    ResourceHandler<ItemResource> resourceHandler = level.getCapability(
                            Capabilities.Item.BLOCK, neighbor, dir.getOpposite());
                    if (resourceHandler != null) {
                        visited.add(neighbor);
                        handlers.add(resourceHandler);
                    }
                }
            }
        }

        return handlers;
    }
}
