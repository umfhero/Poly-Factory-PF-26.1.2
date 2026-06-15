package net.umf.simpledigitalstorage.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.umf.simpledigitalstorage.SimpleDigitalStorage;
import net.umf.simpledigitalstorage.block.entity.StorageHubBlockEntity;
import net.umf.simpledigitalstorage.network.SyncNetworkItemsPacket;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Server-side container for the Storage Hub GUI.
 * On the server, network slots are no longer managed by vanilla Container slots.
 * Instead, they are synced via custom packets and rendered in a custom virtual grid.
 * This menu only contains the player's 36 inventory slots and intercepts quick moves.
 */
public class StorageHubMenu extends AbstractContainerMenu {



    private final BlockPos hubPos;
    private final ContainerLevelAccess access;
    private StorageHubMenu(int containerId, Inventory playerInv, BlockPos hubPos) {
        super(ModMenuTypes.STORAGE_HUB_MENU.get(), containerId);
        this.hubPos = hubPos;
        this.access = ContainerLevelAccess.create(playerInv.player.level(), hubPos);

        // --- Player inventory (3 rows × 9 columns) ---
        // Y offset is chosen to place the inventory below our custom grid.
        int playerInvY = 18 + 6 * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9,
                        8 + col * 18,
                        playerInvY + row * 18));
            }
        }

        // --- Hotbar (1 row × 9 columns) ---
        int hotbarY = playerInvY + 3 * 18 + 4;
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, hotbarY));
        }
    }

    // ----- Factory methods -----

    /**
     * Creates the server-side menu. Called from {@link StorageHubBlockEntity#createMenu}.
     */
    public static StorageHubMenu createServerMenu(int containerId, Inventory playerInv, BlockPos hubPos) {
        StorageHubMenu menu = new StorageHubMenu(containerId, playerInv, hubPos);
        BlockEntity be = playerInv.player.level().getBlockEntity(hubPos);
        if (be instanceof StorageHubBlockEntity hub && playerInv.player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new SyncNetworkItemsPacket(hub.getNetworkItems()));
        }
        return menu;
    }

    /**
     * Client-side factory called by the menu type registration.
     * Reads the hub position and network slot count from the network buffer.
     */
    public static StorageHubMenu clientFactory(int containerId, Inventory playerInv,
                                                RegistryFriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        return new StorageHubMenu(containerId, playerInv, pos);
    }

    public BlockPos getHubPos() {
        return hubPos;
    }

    // ----- AbstractContainerMenu overrides -----

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, SimpleDigitalStorage.STORAGE_HUB.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();

            // All slots are player inventory slots (0 to 35).
            // Shift-clicking tries to send the item to the Storage Network via the Hub.
            if (!player.level().isClientSide()) {
                BlockEntity be = player.level().getBlockEntity(this.hubPos);
                if (be instanceof StorageHubBlockEntity hub) {
                    ItemStack remainder = hub.insertItem(stackInSlot);
                    slot.set(remainder);
                    if (!remainder.isEmpty()) {
                        slot.setChanged();
                    }
                    return result;
                }
            }
        }

        return result;
    }
}
