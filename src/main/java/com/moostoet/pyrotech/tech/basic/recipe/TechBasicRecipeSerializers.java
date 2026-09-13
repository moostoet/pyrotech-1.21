package com.moostoet.pyrotech.tech.basic.recipe;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** One serializer per tech/basic recipe type. The recipe files are datagen's. */
public final class TechBasicRecipeSerializers {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, Pyrotech.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<KilnRecipe>> PIT_KILN =
        RECIPE_SERIALIZERS.register("pit_kiln", () -> KilnRecipe.serializer(TechBasicRecipeTypes.PIT_KILN, TechBasicRecipeSerializers.PIT_KILN));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DryingRecipe>> CRUDE_DRYING_RACK =
        RECIPE_SERIALIZERS.register("crude_drying_rack",
            () -> DryingRecipe.serializer(TechBasicRecipeTypes.CRUDE_DRYING_RACK, TechBasicRecipeSerializers.CRUDE_DRYING_RACK));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DryingRecipe>> DRYING_RACK =
        RECIPE_SERIALIZERS.register("drying_rack",
            () -> DryingRecipe.serializer(TechBasicRecipeTypes.DRYING_RACK, TechBasicRecipeSerializers.DRYING_RACK));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ChoppingBlockRecipe>> CHOPPING_BLOCK =
        RECIPE_SERIALIZERS.register("chopping_block", ChoppingBlockRecipe::serializer);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AnvilRecipe>> ANVIL =
        RECIPE_SERIALIZERS.register("anvil", AnvilRecipe::serializer);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SoakingPotRecipe>> SOAKING_POT =
        RECIPE_SERIALIZERS.register("soaking_pot", SoakingPotRecipe::serializer);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CompactingBinRecipe>> COMPACTING_BIN =
        RECIPE_SERIALIZERS.register("compacting_bin",
            () -> CompactingBinRecipe.serializer(TechBasicRecipeTypes.COMPACTING_BIN, TechBasicRecipeSerializers.COMPACTING_BIN));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BarrelRecipe>> BARREL =
        RECIPE_SERIALIZERS.register("barrel", BarrelRecipe::serializer);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TanningRackRecipe>> TANNING_RACK =
        RECIPE_SERIALIZERS.register("tanning_rack", TanningRackRecipe::serializer);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CampfireRecipe>> CAMPFIRE =
        RECIPE_SERIALIZERS.register("campfire", CampfireRecipe::serializer);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CompostBinRecipe>> COMPOST_BIN =
        RECIPE_SERIALIZERS.register("compost_bin", CompostBinRecipe::serializer);

    private TechBasicRecipeSerializers() {
    }
}
