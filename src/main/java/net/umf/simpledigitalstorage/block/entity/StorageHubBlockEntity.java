package net.umf.simpledigitalstorage.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.umf.simpledigitalstorage.gui.StorageHubMenu;
import net.umf.simpledigitalstorage.network.StorageNetworkScanner;
import net.umf.simpledigitalstorage.network.SyncNetworkItemsPacket;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Block entity for the Storage Hub.
 * <p>
 * Acts as a {@link MenuProvider} so that right-clicking the hub opens the storage GUI.
 * Caches the most recent network scan results so the menu constructor can read them
 * without scanning twice.
 */
public class StorageHubBlockEntity extends BlockEntity implements MenuProvider {

    /** Cached scan results from the most recent {@link #scanNetwork()} call. */
    private List<ResourceHandler<ItemResource>> lastScanResult = List.of();

    public StorageHubBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STORAGE_HUB.get(), pos, state);
    }

    /**
     * Runs a BFS scan of the cable network starting from this hub.
     * Results are cached internally and returned.
     */
    public List<ResourceHandler<ItemResource>> scanNetwork() {
        if (this.level != null && !this.level.isClientSide()) {
            this.lastScanResult = StorageNetworkScanner.scan(this.level, this.worldPosition);
        }
        return this.lastScanResult;
    }

    /**
     * Returns the results from the most recent {@link #scanNetwork()} call.
     */
    public List<ResourceHandler<ItemResource>> getLastScanResult() {
        return this.lastScanResult;
    }

    /**
     * Aggregates all items in the network into a map of ItemResource to their total quantities.
     */
    public Map<ItemResource, Long> getNetworkItems() {
        Map<ItemResource, Long> items = new LinkedHashMap<>();
        if (this.lastScanResult != null) {
            for (ResourceHandler<ItemResource> handler : this.lastScanResult) {
                int size = handler.size();
                for (int i = 0; i < size; i++) {
                    ItemResource res = handler.getResource(i);
                    long amount = handler.getAmountAsLong(i);
                    if (!res.isEmpty() && amount > 0) {
                        items.put(res, items.getOrDefault(res, 0L) + amount);
                    }
                }
            }
        }
        return items;
    }

    /**
     * Attempts to insert an ItemStack into the network.
     * @return The remainder that could not be inserted.
     */
    public ItemStack insertItem(ItemStack stack) {
        if (stack.isEmpty() || this.lastScanResult == null || this.lastScanResult.isEmpty()) return stack;
        
        try (Transaction tx = Transaction.openRoot()) {
            ItemResource resource = ItemResource.of(stack);
            long amount = stack.getCount();
            long insertedTotal = 0;
            
            for (ResourceHandler<ItemResource> handler : this.lastScanResult) {
                long inserted = handler.insert(resource, (int) (amount - insertedTotal), tx);
                insertedTotal += inserted;
                if (insertedTotal >= amount) break;
            }
            
            if (insertedTotal > 0) {
                tx.commit();
                stack.shrink((int) insertedTotal);
                syncToViewers();
            }
        }
        return stack;
    }

    /**
     * Attempts to extract a specific resource from the network.
     */
    public ItemStack extractItem(ItemResource resource, int amount) {
        if (this.lastScanResult == null || this.lastScanResult.isEmpty() || amount <= 0) return ItemStack.EMPTY;
        
        try (Transaction tx = Transaction.openRoot()) {
            long extractedTotal = 0;
            for (ResourceHandler<ItemResource> handler : this.lastScanResult) {
                long extracted = handler.extract(resource, (int) (amount - extractedTotal), tx);
                extractedTotal += extracted;
                if (extractedTotal >= amount) break;
            }
            
            if (extractedTotal > 0) {
                tx.commit();
                syncToViewers();
                return resource.toStack((int) extractedTotal);
            }
        }
        return ItemStack.EMPTY;
    }

    public void syncToViewers() {
        if (this.level != null && !this.level.isClientSide()) {
            SyncNetworkItemsPacket pkt = new SyncNetworkItemsPacket(this.getNetworkItems());
            for (Player player : this.level.players()) {
                if (player instanceof ServerPlayer sp && sp.containerMenu instanceof StorageHubMenu menu) {
                    if (menu.getHubPos().equals(this.worldPosition)) {
                        PacketDistributor.sendToPlayer(sp, pkt);
                    }
                }
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.simpledigitalstorage.storage_hub");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return StorageHubMenu.createServerMenu(containerId, playerInv, this.worldPosition);
    }
}
