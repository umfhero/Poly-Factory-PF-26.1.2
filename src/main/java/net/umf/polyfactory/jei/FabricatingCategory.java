package net.umf.polyfactory.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.umf.polyfactory.PolyFactory;
import net.umf.polyfactory.recipe.FabricatingRecipe;

/**
 * Renders Fabricator {@link FabricatingRecipe}s in JEI: an item input slot (plus a fluid input
 * slot when the recipe needs one), an output slot, and the energy cost/processing time for the
 * recipe - so a player checking the Fabricator's "uses" sees every recipe it can run along with
 * what each one costs, similar to Mekanism's machine categories.
 */
public class FabricatingCategory implements IRecipeCategory<FabricatingRecipe> {

    private static final int WIDTH = 120;
    private static final int HEIGHT = 72;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_GAP = 2;

    private static final int INPUT_X = 4;
    private static final int OUTPUT_X = WIDTH - SLOT_SIZE - 4;
    private static final int ITEM_INPUT_Y = 4;
    private static final int FLUID_INPUT_Y = ITEM_INPUT_Y + SLOT_SIZE + SLOT_GAP;
    private static final int OUTPUT_Y = ITEM_INPUT_Y + (FLUID_INPUT_Y + SLOT_SIZE - ITEM_INPUT_Y - SLOT_SIZE) / 2;

    private static final int ARROW_WIDTH = 24;
    private static final int ARROW_HEIGHT = 14;
    private static final int ARROW_X = INPUT_X + SLOT_SIZE + (OUTPUT_X - (INPUT_X + SLOT_SIZE) - ARROW_WIDTH) / 2;
    private static final int ARROW_Y = OUTPUT_Y + (SLOT_SIZE - ARROW_HEIGHT) / 2;
    private static final int ARROW_COLOR = 0xFF8B8B8B;

    private static final int TEXT_Y_ENERGY = FLUID_INPUT_Y + SLOT_SIZE + 6;
    private static final int TEXT_Y_TIME = TEXT_Y_ENERGY + 10;
    private static final int TEXT_COLOR = 0xFF404040;

    private final IDrawable icon;

    public FabricatingCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(PolyFactory.FABRICATOR_ITEM.get()));
    }

    @Override
    public IRecipeType<FabricatingRecipe> getRecipeType() {
        return PolyFactoryJeiPlugin.FABRICATING;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.polyfactory.fabricator");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FabricatingRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(INPUT_X, ITEM_INPUT_Y)
                .setStandardSlotBackground()
                .add(recipe.input());
        recipe.fluidIngredient().ifPresent(fluid -> builder.addInputSlot(INPUT_X, FLUID_INPUT_Y)
                .setStandardSlotBackground()
                .add(NeoForgeTypes.FLUID_STACK, new FluidStack(fluid, recipe.fluidAmount())));
        builder.addOutputSlot(OUTPUT_X, OUTPUT_Y)
                .setOutputSlotBackground()
                .add(sampleOutput(recipe));
    }

    @Override
    public void draw(FabricatingRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        drawArrow(guiGraphics, ARROW_X, ARROW_Y, ARROW_WIDTH, ARROW_HEIGHT, ARROW_COLOR);

        Font font = Minecraft.getInstance().font;
        Component energy = energyText(recipe);
        Component time = timeText(recipe);
        guiGraphics.text(font, energy, centeredX(font, energy), TEXT_Y_ENERGY, TEXT_COLOR, false);
        guiGraphics.text(font, time, centeredX(font, time), TEXT_Y_TIME, TEXT_COLOR, false);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, FabricatingRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (mouseX >= ARROW_X && mouseX < ARROW_X + ARROW_WIDTH && mouseY >= ARROW_Y && mouseY < ARROW_Y + ARROW_HEIGHT) {
            tooltip.add(Component.translatable("jei.polyfactory.fabricating.energy_per_tick", recipe.energyPerTick()));
            tooltip.add(Component.translatable("jei.polyfactory.fabricating.ticks", recipe.processingTime()));
        }
    }

    private static int totalEnergy(FabricatingRecipe recipe) {
        return recipe.energyPerTick() * recipe.processingTime();
    }

    private static Component energyText(FabricatingRecipe recipe) {
        return Component.translatable("jei.polyfactory.fabricating.energy", String.format("%,d", totalEnergy(recipe)));
    }

    private static Component timeText(FabricatingRecipe recipe) {
        double seconds = recipe.processingTime() / 20.0;
        return Component.translatable("jei.polyfactory.fabricating.time", String.format("%.1f", seconds));
    }

    private static int centeredX(Font font, Component text) {
        return (WIDTH - font.width(text)) / 2;
    }

    /** Ingredient holds the accepted items, not a fixed one, so assemble a sample for display. */
    private static ItemStack sampleOutput(FabricatingRecipe recipe) {
        return recipe.input().items()
                .findFirst()
                .map(holder -> recipe.assemble(new SingleRecipeInput(new ItemStack(holder))))
                .orElse(ItemStack.EMPTY);
    }

    /** A static, fully-filled version of the in-game progress arrow (see FabricatorScreen). */
    private static void drawArrow(GuiGraphicsExtractor g, int x, int y, int width, int height, int color) {
        int headWidth = (height + 1) / 2;
        int shaftWidth = width - headWidth;
        int shaftHeight = Math.max(2, (height / 6) * 2);
        int shaftTop = y + (height - shaftHeight) / 2;

        g.fill(x, shaftTop, x + shaftWidth, shaftTop + shaftHeight, color);
        for (int row = 0; row < height; row++) {
            int rowWidth = Math.min(row, height - 1 - row) + 1;
            g.fill(x + shaftWidth, y + row, x + shaftWidth + rowWidth, y + row + 1, color);
        }
    }
}
