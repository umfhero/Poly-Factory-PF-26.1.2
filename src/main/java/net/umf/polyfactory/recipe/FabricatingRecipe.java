package net.umf.polyfactory.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.level.material.Fluid;

import java.util.Optional;

/**
 * Input -> output recipe processed by the Fabricator over {@link #processingTime()} ticks,
 * draining {@link #energyPerTick()} FE from its internal buffer every tick while running. May also
 * require a {@link #fluidIngredient()} of at least {@link #fluidAmount()} mB sitting in the
 * Fabricator's tank, consumed alongside the item ingredient once a craft finishes.
 */
public class FabricatingRecipe extends SingleItemRecipe {
    public static final MapCodec<FabricatingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                            Recipe.CommonInfo.MAP_CODEC.forGetter((FabricatingRecipe o) -> o.commonInfo),
                            Ingredient.CODEC.fieldOf("ingredient").forGetter(FabricatingRecipe::input),
                            ItemStackTemplate.CODEC.fieldOf("result").forGetter(FabricatingRecipe::result),
                            Codec.INT.optionalFieldOf("processing_time", 100).forGetter(FabricatingRecipe::processingTime),
                            Codec.INT.optionalFieldOf("energy_per_tick", 20).forGetter(FabricatingRecipe::energyPerTick),
                            BuiltInRegistries.FLUID.holderByNameCodec().optionalFieldOf("fluid_ingredient")
                                    .forGetter(FabricatingRecipe::fluidIngredient),
                            Codec.INT.optionalFieldOf("fluid_amount", 0).forGetter(FabricatingRecipe::fluidAmount)
                    )
                    .apply(i, FabricatingRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, FabricatingRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC,
            (FabricatingRecipe o) -> o.commonInfo,
            Ingredient.CONTENTS_STREAM_CODEC,
            FabricatingRecipe::input,
            ItemStackTemplate.STREAM_CODEC,
            FabricatingRecipe::result,
            ByteBufCodecs.INT,
            FabricatingRecipe::processingTime,
            ByteBufCodecs.INT,
            FabricatingRecipe::energyPerTick,
            ByteBufCodecs.optional(ByteBufCodecs.holderRegistry(Registries.FLUID)),
            FabricatingRecipe::fluidIngredient,
            ByteBufCodecs.INT,
            FabricatingRecipe::fluidAmount,
            FabricatingRecipe::new
    );

    public static final RecipeSerializer<FabricatingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final int processingTime;
    private final int energyPerTick;
    private final Optional<Holder<Fluid>> fluidIngredient;
    private final int fluidAmount;

    public FabricatingRecipe(
            Recipe.CommonInfo commonInfo,
            Ingredient ingredient,
            ItemStackTemplate result,
            int processingTime,
            int energyPerTick,
            Optional<Holder<Fluid>> fluidIngredient,
            int fluidAmount
    ) {
        super(commonInfo, ingredient, result);
        this.processingTime = processingTime;
        this.energyPerTick = energyPerTick;
        this.fluidIngredient = fluidIngredient;
        this.fluidAmount = fluidAmount;
    }

    public int processingTime() {
        return this.processingTime;
    }

    public int energyPerTick() {
        return this.energyPerTick;
    }

    public Optional<Holder<Fluid>> fluidIngredient() {
        return this.fluidIngredient;
    }

    public int fluidAmount() {
        return this.fluidAmount;
    }

    @Override
    public RecipeSerializer<FabricatingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<FabricatingRecipe> getType() {
        return ModRecipes.FABRICATING_TYPE.get();
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.FURNACE_MISC;
    }
}
