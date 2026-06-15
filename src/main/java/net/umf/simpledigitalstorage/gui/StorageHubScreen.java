package net.umf.simpledigitalstorage.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.umf.simpledigitalstorage.network.ExtractItemPacket;

import java.util.*;

/**
 * Client-side GUI screen for the Storage Hub.
 * <p>
 * Renders a custom scrollable grid for the network items and handles filtering/sorting.
 */
public class StorageHubScreen extends AbstractContainerScreen<StorageHubMenu> {

    private Map<ItemResource, Long> networkItems = new LinkedHashMap<>();
    private List<Map.Entry<ItemResource, Long>> filteredItems = new ArrayList<>();

    private EditBox searchBox;
    private int scrollOffset = 0;
    private boolean sortByName = true;

    private static final int NETWORK_ROWS = 5;
    private static final int NETWORK_COLS = 9;

    public StorageHubScreen(StorageHubMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, 176, 114 + NETWORK_ROWS * 18 + 24); // Added 24px for search bar
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        
        this.searchBox = new EditBox(this.font, this.leftPos + 8, this.topPos + 6, 120, 12, Component.translatable("gui.simpledigitalstorage.search"));
        this.searchBox.setResponder(this::onSearchChange);
        this.addRenderableWidget(this.searchBox);
        
        updateFilters();
    }

    public void updateNetworkItems(Map<ItemResource, Long> items) {
        this.networkItems = items;
        updateFilters();
    }

    private void onSearchChange(String query) {
        this.scrollOffset = 0;
        updateFilters();
    }

    private void updateFilters() {
        String query = this.searchBox.getValue().toLowerCase(Locale.ROOT);
        this.filteredItems.clear();

        for (Map.Entry<ItemResource, Long> entry : this.networkItems.entrySet()) {
            if (query.isEmpty() || entry.getKey().getItem().getName(entry.getKey().toStack()).getString().toLowerCase(Locale.ROOT).contains(query)) {
                this.filteredItems.add(entry);
            }
        }

        this.filteredItems.sort((a, b) -> {
            if (sortByName) {
                String nameA = a.getKey().getItem().getName(a.getKey().toStack()).getString();
                String nameB = b.getKey().getItem().getName(b.getKey().toStack()).getString();
                return nameA.compareToIgnoreCase(nameB);
            } else {
                return Long.compare(b.getValue(), a.getValue()); // Descending
            }
        });
    }

    // Removed @Override so we don't conflict with any intermediate classes if the signature differs.
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxOffset = Math.max(0, (this.filteredItems.size() + NETWORK_COLS - 1) / NETWORK_COLS - NETWORK_ROWS);
        if (scrollY > 0 && this.scrollOffset > 0) {
            this.scrollOffset--;
            return true;
        } else if (scrollY < 0 && this.scrollOffset < maxOffset) {
            this.scrollOffset++;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();

        // Check Sort Button click (placeholder area next to search box)
        if (mouseX >= this.leftPos + 132 && mouseX <= this.leftPos + 168 && mouseY >= this.topPos + 4 && mouseY <= this.topPos + 20) {
            this.sortByName = !this.sortByName;
            updateFilters();
            return true;
        }

        // Check virtual grid click
        int gridX = this.leftPos + 7;
        int gridY = this.topPos + 24;
        
        if (mouseX >= gridX && mouseX < gridX + NETWORK_COLS * 18 && mouseY >= gridY && mouseY < gridY + NETWORK_ROWS * 18) {
            int col = (int) ((mouseX - gridX) / 18);
            int row = (int) ((mouseY - gridY) / 18);
            int index = (this.scrollOffset + row) * NETWORK_COLS + col;
            
            if (index < this.filteredItems.size()) {
                Map.Entry<ItemResource, Long> entry = this.filteredItems.get(index);
                ItemResource resource = entry.getKey();
                
                boolean shiftClick = event.hasShiftDown();
                int extractAmount = button == 1 ? resource.toStack().getMaxStackSize() / 2 : resource.toStack().getMaxStackSize();
                if (extractAmount == 0) extractAmount = 1;
                if (shiftClick) extractAmount = resource.toStack().getMaxStackSize();

                ClientPacketDistributor.sendToServer(new ExtractItemPacket(this.menu.getHubPos(), resource, extractAmount, shiftClick));
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractContents(net.minecraft.client.gui.GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        renderPanel(graphics, leftPos, topPos, imageWidth, imageHeight);
        
        // Draw sort button
        graphics.fill(leftPos + 132, topPos + 4, leftPos + 168, topPos + 20, 0xFF8B8B8B);
        graphics.text(this.font, sortByName ? "Name" : "Qty", leftPos + 138, topPos + 8, 0xFFFFFF, false);

        int gridX = this.leftPos + 8;
        int gridY = this.topPos + 24;

        // Render virtual slots
        for (int row = 0; row < NETWORK_ROWS; row++) {
            for (int col = 0; col < NETWORK_COLS; col++) {
                int x = gridX + col * 18;
                int y = gridY + row * 18;
                renderSlotFrame(graphics, x - 1, y - 1);
                
                int index = (this.scrollOffset + row) * NETWORK_COLS + col;
                if (index < this.filteredItems.size()) {
                    Map.Entry<ItemResource, Long> entry = this.filteredItems.get(index);
                    ItemStack stack = entry.getKey().toStack();
                    graphics.item(stack, x, y);
                    
                    // Render quantity
                    long qty = entry.getValue();
                    String qtyStr = qty > 999 ? (qty / 1000) + "k" : String.valueOf(qty);
                    graphics.pose().pushMatrix();
                    graphics.text(this.font, qtyStr, x + 17 - this.font.width(qtyStr), y + 9, 0xFFFFFF, true);
                    graphics.pose().popMatrix();
                }
            }
        }

        // Vanilla slots
        for (Slot slot : this.menu.slots) {
            renderSlotFrame(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
        }

        super.extractContents(graphics, mouseX, mouseY, partialTick);
        
        // Tooltip
        if (mouseX >= gridX && mouseX < gridX + NETWORK_COLS * 18 && mouseY >= gridY && mouseY < gridY + NETWORK_ROWS * 18) {
            int col = (int) ((mouseX - gridX) / 18);
            int row = (int) ((mouseY - gridY) / 18);
            int index = (this.scrollOffset + row) * NETWORK_COLS + col;
            if (index < this.filteredItems.size()) {
                ItemStack stack = this.filteredItems.get(index).getKey().toStack();
                graphics.setTooltipForNextFrame(this.font, this.getTooltipFromContainerItem(stack), stack.getTooltipImage(), stack, mouseX, mouseY, stack.get(DataComponents.TOOLTIP_STYLE));
            }
        }
    }

    private void renderPanel(net.minecraft.client.gui.GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFFC6C6C6);
        g.fill(x, y, x + w - 1, y + 1, 0xFFFFFFFF);
        g.fill(x, y + 1, x + 1, y + h - 1, 0xFFFFFFFF);
        g.fill(x + 1, y + h - 1, x + w, y + h, 0xFF555555);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, 0xFF555555);
        g.fill(x + w - 1, y, x + w, y + 1, 0xFFC6C6C6);
        g.fill(x, y + h - 1, x + 1, y + h, 0xFFC6C6C6);
    }

    private void renderSlotFrame(net.minecraft.client.gui.GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + 17, y + 1, 0xFF373737);
        g.fill(x, y + 1, x + 1, y + 17, 0xFF373737);
        g.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        g.fill(x + 17, y + 1, x + 18, y + 17, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B8B8B);
    }
}
