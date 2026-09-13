package com.moostoet.pyrotech.tech.basic.recipe;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The eleven tech/basic recipe types, one per machine, over ten recipe classes. */
public final class TechBasicRecipeTypes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Pyrotech.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<KilnRecipe>> PIT_KILN = type("pit_kiln");
    public static final DeferredHolder<RecipeType<?>, RecipeType<DryingRecipe>> CRUDE_DRYING_RACK = type("crude_drying_rack");
    public static final DeferredHolder<RecipeType<?>, RecipeType<DryingRecipe>> DRYING_RACK = type("drying_rack");
    public static final DeferredHolder<RecipeType<?>, RecipeType<ChoppingBlockRecipe>> CHOPPING_BLOCK = type("chopping_block");
    public static final DeferredHolder<RecipeType<?>, RecipeType<AnvilRecipe>> ANVIL = type("anvil");
    public static final DeferredHolder<RecipeType<?>, RecipeType<SoakingPotRecipe>> SOAKING_POT = type("soaking_pot");
    public static final DeferredHolder<RecipeType<?>, RecipeType<CompactingBinRecipe>> COMPACTING_BIN = type("compacting_bin");
    public static final DeferredHolder<RecipeType<?>, RecipeType<BarrelRecipe>> BARREL = type("barrel");
    public static final DeferredHolder<RecipeType<?>, RecipeType<TanningRackRecipe>> TANNING_RACK = type("tanning_rack");
    public static final DeferredHolder<RecipeType<?>, RecipeType<CampfireRecipe>> CAMPFIRE = type("campfire");
    public static final DeferredHolder<RecipeType<?>, RecipeType<CompostBinRecipe>> COMPOST_BIN = type("compost_bin");

    private TechBasicRecipeTypes() {
    }

    private static <R extends Recipe<?>> DeferredHolder<RecipeType<?>, RecipeType<R>> type(String name) {
        return RECIPE_TYPES.register(name, () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, name)));
    }
}
