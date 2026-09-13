package com.moostoet.pyrotech.datagen.tech.basic;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.bucket.BucketItems;
import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.core.CoreFluids;
import com.moostoet.pyrotech.core.CoreItems;
import com.moostoet.pyrotech.core.Material;
import com.moostoet.pyrotech.core.PyrotechTags;
import com.moostoet.pyrotech.datagen.RecipeUnit;
import com.moostoet.pyrotech.datagen.ToolDamageShapelessRecipeBuilder;
import com.moostoet.pyrotech.hunting.HuntingItems;
import com.moostoet.pyrotech.hunting.HuntingTags;
import com.moostoet.pyrotech.tech.basic.TechBasicBlocks;
import com.moostoet.pyrotech.tech.basic.TechBasicItems;
import com.moostoet.pyrotech.tech.basic.recipe.AnvilRecipe;
import com.moostoet.pyrotech.tech.basic.recipe.AnvilTier;
import com.moostoet.pyrotech.tech.basic.recipe.AnvilToolType;
import com.moostoet.pyrotech.tech.basic.recipe.BarrelRecipe;
import com.moostoet.pyrotech.tech.basic.recipe.ChoppingBlockRecipe;
import com.moostoet.pyrotech.tech.basic.recipe.CompactingBinRecipe;
import com.moostoet.pyrotech.tech.basic.recipe.CompostBinRecipe;
import com.moostoet.pyrotech.tech.basic.recipe.CompostValues;
import com.moostoet.pyrotech.tech.basic.recipe.DryingRecipe;
import com.moostoet.pyrotech.tech.basic.recipe.KilnRecipe;
import com.moostoet.pyrotech.tech.basic.recipe.SoakingPotRecipe;
import com.moostoet.pyrotech.tech.basic.recipe.TanningRackRecipe;
import com.moostoet.pyrotech.tech.basic.recipe.TechBasicRecipeSerializers;
import com.moostoet.pyrotech.tech.basic.recipe.TechBasicRecipeTypes;
import com.moostoet.pyrotech.tool.ToolItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Tech/basic's recipes: the nineteen 1.12 crafting JSONs by their file names, and the
 * eleven adders as one method each, with the chain table's copies baked in (recipe
 * architecture sign-off): the drying rack inherits the crude rack, the ironclad anvil
 * the granite one, and the obsidian anvil the ironclad, all at a multiplier of one. The
 * post-1.12 additions are the five new woods on the chopping block, the two stone slab
 * and smooth stone routes, and the five raw ore and copper decompressions.
 */
public final class TechBasicRecipeProvider extends RecipeProvider implements RecipeUnit {

    private static final int MINUTE = 60 * 20;
    private static final int PIT_KILN_BURN_TICKS = 14 * MINUTE;
    private static final float PIT_KILN_FAILURE_CHANCE = 0.33f;
    private static final int TOOL_DAMAGE = 1;
    private static final Map<String, TagKey<Item>> PLANK_LOGS = Map.ofEntries(
        Map.entry("oak", ItemTags.OAK_LOGS), Map.entry("spruce", ItemTags.SPRUCE_LOGS), Map.entry("birch", ItemTags.BIRCH_LOGS),
        Map.entry("jungle", ItemTags.JUNGLE_LOGS), Map.entry("acacia", ItemTags.ACACIA_LOGS), Map.entry("dark_oak", ItemTags.DARK_OAK_LOGS),
        Map.entry("mangrove", ItemTags.MANGROVE_LOGS), Map.entry("cherry", ItemTags.CHERRY_LOGS), Map.entry("bamboo", ItemTags.BAMBOO_BLOCKS),
        Map.entry("crimson", ItemTags.CRIMSON_STEMS), Map.entry("warped", ItemTags.WARPED_STEMS));

    public TechBasicRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    public void buildRecipes(RecipeOutput output) {
        this.crafting(output);
        this.pitKiln(output);
        this.dryingRacks(output);
        this.choppingBlock(output);
        this.anvils(output);
        this.compactingBin(output);
        this.compostBin(output);
        this.soakingPot(output);
        this.barrel(output);
        this.tanningRack(output);
    }

    // -- Crafting ------------------------------------------------------------

    private void crafting(RecipeOutput output) {
        Item smoothSlab = Items.SMOOTH_STONE_SLAB;
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TechBasicBlocks.ANVIL_GRANITE.get()).pattern("G").pattern("S")
            .define('G', Items.POLISHED_GRANITE).define('S', smoothSlab)
            .unlockedBy(getHasName(Items.POLISHED_GRANITE), has(Items.POLISHED_GRANITE)).save(output, id("anvil_granite"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TechBasicBlocks.ANVIL_IRON_PLATED.get()).pattern("III").pattern("GGG").pattern("SSS")
            .define('I', Tags.Items.INGOTS_IRON).define('G', Items.POLISHED_GRANITE).define('S', smoothSlab)
            .unlockedBy("has_iron_ingot", has(Tags.Items.INGOTS_IRON)).save(output, id("anvil_iron_plated"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TechBasicBlocks.ANVIL_OBSIDIAN.get()).pattern("GGG").pattern("III")
            .define('G', Items.OBSIDIAN).define('I', Tags.Items.INGOTS_IRON)
            .unlockedBy(getHasName(Items.OBSIDIAN), has(Items.OBSIDIAN)).save(output, id("anvil_obsidian"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TechBasicBlocks.BARREL.get()).pattern("B B").pattern("B B").pattern("BPB")
            .define('B', item(Material.BOARD_TARRED)).define('P', CoreBlocks.PLANKS_TARRED.get())
            .unlockedBy(hasName(Material.BOARD_TARRED), has(Material.BOARD_TARRED)).save(output, id("barrel"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TechBasicItems.BARREL_LID.get()).pattern("BPB")
            .define('B', item(Material.BOARD_TARRED)).define('P', CoreBlocks.PLANKS_TARRED.get())
            .unlockedBy(hasName(Material.BOARD_TARRED), has(Material.BOARD_TARRED)).save(output, id("barrel_lid"));
        ToolDamageShapelessRecipeBuilder.toolDamageShapeless(RecipeCategory.DECORATIONS, TechBasicBlocks.CHOPPING_BLOCK.get(), 1,
                Ingredient.of(ItemTags.AXES), TOOL_DAMAGE)
            .requires(ItemTags.LOGS_THAT_BURN)
            .unlockedBy("has_log", has(ItemTags.LOGS_THAT_BURN)).save(output, id("chopping_block"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TechBasicBlocks.COMPACTING_BIN.get()).pattern("T T").pattern("P P").pattern("TST")
            .define('P', ItemTags.PLANKS).define('S', ItemTags.WOODEN_SLABS).define('T', smoothSlab)
            .unlockedBy(getHasName(smoothSlab), has(smoothSlab)).save(output, id("compacting_bin"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TechBasicBlocks.COMPOST_BIN.get()).pattern("TLT").pattern("LWL").pattern("TLT")
            .define('T', PyrotechTags.Items.TWINE).define('L', Items.LADDER).define('W', ItemTags.LOGS_THAT_BURN)
            .unlockedBy(getHasName(Items.LADDER), has(Items.LADDER)).save(output, id("compost_bin"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, item(Material.PLANT_FIBERS_DRIED), 2).requires(TechBasicBlocks.KILN_PIT.get())
            .unlockedBy(getHasName(TechBasicBlocks.KILN_PIT.get()), has(TechBasicBlocks.KILN_PIT.get()))
            .save(output, id("dried_plant_fibers_from_pit_kiln"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TechBasicBlocks.DRYING_RACK.get()).pattern("SFS").pattern("FLF").pattern("SFS")
            .define('S', Tags.Items.RODS_WOODEN).define('F', PyrotechTags.Items.TWINE).define('L', Items.LADDER)
            .unlockedBy(getHasName(Items.LADDER), has(Items.LADDER)).save(output, id("drying_rack"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TechBasicBlocks.DRYING_RACK_CRUDE.get()).pattern("SS").pattern("FF")
            .define('S', Tags.Items.RODS_WOODEN).define('F', item(Material.PLANT_FIBERS))
            .unlockedBy(hasName(Material.PLANT_FIBERS), has(Material.PLANT_FIBERS)).save(output, id("drying_rack_crude"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TechBasicBlocks.KILN_PIT.get()).pattern("SS")
            .define('S', item(Material.PLANT_FIBERS_DRIED))
            .unlockedBy(hasName(Material.PLANT_FIBERS_DRIED), has(Material.PLANT_FIBERS_DRIED)).save(output, id("kiln_pit"));
        ShapedRecipeBuilder.shaped(RecipeCategory.FOOD, TechBasicItems.MARSHMALLOW.get(), 8).pattern("SSS").pattern("SMS").pattern("SSS")
            .define('S', Items.SUGAR).define('M', Tags.Items.BUCKETS_MILK)
            .unlockedBy(getHasName(Items.SUGAR), has(Items.SUGAR)).save(output, id("marshmallow"));
        ToolDamageShapelessRecipeBuilder.toolDamageShapeless(RecipeCategory.MISC, TechBasicItems.MARSHMALLOW_STICK_EMPTY.get(), 1,
                Ingredient.of(PyrotechTags.Items.SHARP_TOOLS), TOOL_DAMAGE)
            .requires(Tags.Items.RODS_WOODEN)
            .unlockedBy("has_stick", has(Tags.Items.RODS_WOODEN)).save(output, id("marshmallow_stick"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TechBasicBlocks.SOAKING_POT.get()).pattern("A A").pattern("BAB").pattern("BAB")
            .define('A', item(Material.BRICK_STONE)).define('B', item(Material.BOARD))
            .unlockedBy(hasName(Material.BRICK_STONE), has(Material.BRICK_STONE)).save(output, id("soaking_pot"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TechBasicBlocks.TANNING_RACK.get()).pattern("sSs").pattern("SsS").pattern("LSL")
            .define('L', ItemTags.LOGS_THAT_BURN).define('S', Tags.Items.RODS_WOODEN).define('s', PyrotechTags.Items.TWINE)
            .unlockedBy("has_twine", has(PyrotechTags.Items.TWINE)).save(output, id("tanning_rack"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, TechBasicItems.TINDER.get()).pattern("ST").pattern("TS")
            .define('S', item(Material.PLANT_FIBERS_DRIED)).define('T', Tags.Items.RODS_WOODEN)
            .unlockedBy(hasName(Material.PLANT_FIBERS_DRIED), has(Material.PLANT_FIBERS_DRIED)).save(output, id("tinder"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TechBasicBlocks.WORKTABLE.get()).pattern("B").pattern("L")
            .define('B', ItemTags.WOODEN_SLABS).define('L', ItemTags.LOGS_THAT_BURN)
            .unlockedBy("has_log", has(ItemTags.LOGS_THAT_BURN)).save(output, id("worktable"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, TechBasicBlocks.WORKTABLE_STONE.get()).pattern("RSR").pattern("RWR").pattern("RBR")
            .define('R', item(Material.BRICK_STONE)).define('W', TechBasicBlocks.WORKTABLE.get())
            .define('B', CoreBlocks.MASONRY_BRICK_BLOCK.get()).define('S', Items.POLISHED_ANDESITE)
            .unlockedBy(getHasName(TechBasicBlocks.WORKTABLE.get()), has(TechBasicBlocks.WORKTABLE.get())).save(output, id("worktable_stone"));
    }

    // -- Pit kiln ------------------------------------------------------------

    private void pitKiln(RecipeOutput output) {
        List<ItemStack> pottery = List.of(stack(Material.PIT_ASH), stack(Material.POTTERY_SHARD), stack(Material.POTTERY_FRAGMENTS));
        this.kiln(output, "bucket_clay", BucketItems.BUCKET_CLAY_UNFIRED.get(), new ItemStack(BucketItems.BUCKET_CLAY.get()), pottery);
        this.kiln(output, "bucket_refractory", BucketItems.BUCKET_REFRACTORY_UNFIRED.get(), new ItemStack(BucketItems.BUCKET_REFRACTORY.get()), pottery);
        this.kiln(output, "clay_shears", ToolItems.UNFIRED_CLAY_SHEARS.get(), new ItemStack(ToolItems.CLAY_SHEARS.get()), pottery);
        this.kiln(output, "brick", item(Material.UNFIRED_BRICK), new ItemStack(Items.BRICK), pottery);
        this.kiln(output, "cob", CoreBlocks.COB_WET.get(), new ItemStack(CoreBlocks.COB_DRY.get()),
            List.of(new ItemStack(CoreBlocks.ROCK_DIRT.get()), new ItemStack(CoreBlocks.ROCK_DIRT.get(), 2)));
        this.kiln(output, "charcoal_flakes", CoreBlocks.ROCK_WOOD_CHIPS.get(), stack(Material.CHARCOAL_FLAKES), List.of(stack(Material.PIT_ASH)));
        this.kiln(output, "stone_slab", Items.COBBLESTONE_SLAB, new ItemStack(Items.SMOOTH_STONE_SLAB),
            List.of(stack(Material.PIT_ASH), new ItemStack(CoreBlocks.ROCK_STONE.get(), 3)));
        // Tech/basic sign-off, item 1: smooth stone has no 1.12 route, so the kiln fires it from stone.
        this.kiln(output, "smooth_stone", Items.STONE, new ItemStack(Items.SMOOTH_STONE),
            List.of(stack(Material.PIT_ASH), new ItemStack(CoreBlocks.ROCK_STONE.get(), 5)));
        this.kiln(output, "stone", Items.COBBLESTONE, new ItemStack(Items.STONE),
            List.of(stack(Material.PIT_ASH), new ItemStack(CoreBlocks.ROCK_STONE.get(), 5)));
        this.kiln(output, "stone_andesite", CoreBlocks.COBBLESTONE_ANDESITE.get(), new ItemStack(Items.ANDESITE),
            List.of(stack(Material.PIT_ASH), new ItemStack(CoreBlocks.ROCK_ANDESITE.get(), 5)));
        this.kiln(output, "stone_granite", CoreBlocks.COBBLESTONE_GRANITE.get(), new ItemStack(Items.GRANITE),
            List.of(stack(Material.PIT_ASH), new ItemStack(CoreBlocks.ROCK_GRANITE.get(), 5)));
        this.kiln(output, "stone_diorite", CoreBlocks.COBBLESTONE_DIORITE.get(), new ItemStack(Items.DIORITE),
            List.of(stack(Material.PIT_ASH), new ItemStack(CoreBlocks.ROCK_DIORITE.get(), 5)));
        this.kiln(output, "limestone", CoreBlocks.COBBLESTONE_LIMESTONE.get(), new ItemStack(CoreBlocks.LIMESTONE.get()),
            List.of(stack(Material.PIT_ASH), new ItemStack(CoreBlocks.ROCK_LIMESTONE.get(), 5)));
        this.kiln(output, "hardened_clay", Items.CLAY, new ItemStack(Items.TERRACOTTA), pottery);
        for (DyeColor color : DyeColor.values()) {
            Item glazed = vanilla(color.getName() + "_glazed_terracotta");
            this.kiln(output, color.getName() + "_glazed_terracotta", vanilla(color.getName() + "_terracotta"), new ItemStack(glazed), pottery);
        }
    }

    private void kiln(RecipeOutput output, String name, ItemLike input, ItemStack result, List<ItemStack> failureItems) {
        output.accept(id("pit_kiln/" + name), new KilnRecipe(Ingredient.of(input), result, PIT_KILN_BURN_TICKS, PIT_KILN_FAILURE_CHANCE,
            failureItems, TechBasicRecipeTypes.PIT_KILN, TechBasicRecipeSerializers.PIT_KILN), null);
    }

    // -- Drying racks --------------------------------------------------------

    /** The five crude rack recipes, and the drying rack's copies at the chain table's multiplier of one. */
    private void dryingRacks(RecipeOutput output) {
        record Drying(String name, Ingredient input, ItemStack result, int ticks) {
        }
        List<Drying> crude = List.of(
            new Drying("straw", Ingredient.of(Tags.Items.CROPS_WHEAT), stack(Material.STRAW), 12 * MINUTE),
            new Drying("plant_fibers_dried", Ingredient.of(item(Material.PLANT_FIBERS)), stack(Material.PLANT_FIBERS_DRIED), 8 * MINUTE),
            new Drying("plant_fibers_dried_from_sapling", Ingredient.of(ItemTags.SAPLINGS), stack(Material.PLANT_FIBERS_DRIED), 10 * MINUTE),
            new Drying("sponge", Ingredient.of(Items.WET_SPONGE), new ItemStack(Items.SPONGE), 8 * MINUTE),
            new Drying("paper", Ingredient.of(item(Material.PULP)), new ItemStack(Items.PAPER), 5 * MINUTE));
        for (Drying recipe : crude) {
            output.accept(id("crude_drying_rack/" + recipe.name()), new DryingRecipe(recipe.input(), recipe.result(), recipe.ticks(),
                TechBasicRecipeTypes.CRUDE_DRYING_RACK, TechBasicRecipeSerializers.CRUDE_DRYING_RACK), null);
            output.accept(id("drying_rack/crude_drying_rack/" + recipe.name()), new DryingRecipe(recipe.input(), recipe.result(), recipe.ticks(),
                TechBasicRecipeTypes.DRYING_RACK, TechBasicRecipeSerializers.DRYING_RACK), null);
        }
    }

    // -- Chopping block ------------------------------------------------------

    /** One recipe per vanilla plank recipe, on the wood's log tag (post-1.12 skips sign-off, item 1). */
    private void choppingBlock(RecipeOutput output) {
        for (Map.Entry<String, TagKey<Item>> wood : PLANK_LOGS.entrySet()) {
            String planks = wood.getKey() + "_planks";
            output.accept(id("chopping_block/" + planks), new ChoppingBlockRecipe(Ingredient.of(wood.getValue()), new ItemStack(vanilla(planks)),
                ChoppingBlockRecipe.DEFAULT_CHOPS, ChoppingBlockRecipe.DEFAULT_QUANTITIES), null);
        }
    }

    // -- Anvils --------------------------------------------------------------

    private record Anvil(String name, Ingredient input, ItemStack result, int hits, AnvilToolType tool) {
    }

    private void anvils(RecipeOutput output) {
        List<Anvil> granite = new ArrayList<>();
        this.hammerRecipes(granite);
        this.pickaxeRecipes(granite);
        for (Anvil recipe : granite) {
            output.accept(id("anvil/" + recipe.name()), anvil(recipe, AnvilTier.GRANITE), null);
            output.accept(id("ironclad_anvil/anvil/" + recipe.name()), anvil(recipe, AnvilTier.IRONCLAD), null);
            output.accept(id("obsidian_anvil/ironclad_anvil/anvil/" + recipe.name()), anvil(recipe, AnvilTier.OBSIDIAN), null);
        }
    }

    private static AnvilRecipe anvil(Anvil recipe, AnvilTier tier) {
        return new AnvilRecipe(recipe.input(), recipe.result(), Math.max(1, recipe.hits()), recipe.tool(), List.of(tier));
    }

    private void hammerRecipes(List<Anvil> recipes) {
        recipes.add(hammer("flour_from_wheat", Ingredient.of(Tags.Items.CROPS_WHEAT), stack(Material.FLOUR), 2));
        recipes.add(hammer("redstone_dust_from_dense_redstone", Ingredient.of(item(Material.DENSE_REDSTONE)), new ItemStack(Items.REDSTONE, 2), 2));
        recipes.add(hammer("crushed_flint_from_flint_shard", Ingredient.of(item(Material.FLINT_SHARD)), stack(Material.DUST_FLINT, 3), 2));
        recipes.add(hammer("lapis_lazuli_from_lapis_block", Ingredient.of(Items.LAPIS_BLOCK), new ItemStack(Items.LAPIS_LAZULI, 9), 8));
        recipes.add(hammer("redstone_from_redstone_block", Ingredient.of(Items.REDSTONE_BLOCK), new ItemStack(Items.REDSTONE, 9), 8));
        recipes.add(hammer("sand_pile_from_glass_shard", Ingredient.of(item(Material.GLASS_SHARD)), new ItemStack(CoreBlocks.ROCK_SAND.get()), 4));
        recipes.add(hammer("sand_pile_from_pottery_pieces", Ingredient.of(item(Material.POTTERY_FRAGMENTS), item(Material.POTTERY_SHARD)),
            new ItemStack(CoreBlocks.ROCK_SAND.get()), 4));
        recipes.add(hammer("iron_shard_from_iron_nugget", Ingredient.of(Tags.Items.NUGGETS_IRON), stack(Material.IRON_SHARD), 4));
        recipes.add(hammer("iron_nugget_from_iron_shard", Ingredient.of(item(Material.IRON_SHARD)), new ItemStack(Items.IRON_NUGGET), 4));
        recipes.add(hammer("gold_shard_from_gold_nugget", Ingredient.of(Tags.Items.NUGGETS_GOLD), stack(Material.GOLD_SHARD), 4));
        recipes.add(hammer("gold_nugget_from_gold_shard", Ingredient.of(item(Material.GOLD_SHARD)), new ItemStack(Items.GOLD_NUGGET), 4));
        recipes.add(hammer("charcoal_flakes", Ingredient.of(Items.CHARCOAL), stack(Material.CHARCOAL_FLAKES, 8), 4));
        recipes.add(hammer("coal_pieces", Ingredient.of(Items.COAL), stack(Material.COAL_PIECES, 8), 4));
        recipes.add(hammer("bone_shard", Ingredient.of(item(Material.BONE_SHARD)), new ItemStack(Items.BONE_MEAL), 4));
        recipes.add(hammer("stone_to_cobbled", Ingredient.of(Items.STONE), new ItemStack(Items.COBBLESTONE), 8));
        recipes.add(hammer("andesite_to_cobbled", Ingredient.of(Items.ANDESITE), new ItemStack(CoreBlocks.COBBLESTONE_ANDESITE.get()), 8));
        recipes.add(hammer("diorite_to_cobbled", Ingredient.of(Items.DIORITE), new ItemStack(CoreBlocks.COBBLESTONE_DIORITE.get()), 8));
        recipes.add(hammer("granite_to_cobbled", Ingredient.of(Items.GRANITE), new ItemStack(CoreBlocks.COBBLESTONE_GRANITE.get()), 8));
        recipes.add(hammer("limestone_to_cobbled", Ingredient.of(CoreBlocks.LIMESTONE.get()), new ItemStack(CoreBlocks.COBBLESTONE_LIMESTONE.get()), 8));
        recipes.add(hammer("cobblestone_to_rocks", Ingredient.of(Items.COBBLESTONE), new ItemStack(CoreBlocks.ROCK_STONE.get(), 8), 8));
        recipes.add(hammer("cobbled_andesite_to_rocks", Ingredient.of(CoreBlocks.COBBLESTONE_ANDESITE.get()),
            new ItemStack(CoreBlocks.ROCK_ANDESITE.get(), 8), 8));
        recipes.add(hammer("cobbled_diorite_to_rocks", Ingredient.of(CoreBlocks.COBBLESTONE_DIORITE.get()),
            new ItemStack(CoreBlocks.ROCK_DIORITE.get(), 8), 8));
        recipes.add(hammer("cobbled_granite_to_rocks", Ingredient.of(CoreBlocks.COBBLESTONE_GRANITE.get()),
            new ItemStack(CoreBlocks.ROCK_GRANITE.get(), 8), 8));
        recipes.add(hammer("cobbled_limestone_to_rocks", Ingredient.of(CoreBlocks.COBBLESTONE_LIMESTONE.get()),
            new ItemStack(CoreBlocks.ROCK_LIMESTONE.get(), 8), 8));
        recipes.add(hammer("limestone_rocks_to_crushed_limestone", Ingredient.of(CoreBlocks.ROCK_LIMESTONE.get()), stack(Material.DUST_LIMESTONE), 4));
    }

    private void pickaxeRecipes(List<Anvil> recipes) {
        recipes.add(pickaxe("quartz_from_dense_quartz", Ingredient.of(item(Material.DENSE_QUARTZ)), new ItemStack(Items.QUARTZ, 2), 4));
        recipes.add(pickaxe("flint_shard_from_flint", Ingredient.of(Items.FLINT), stack(Material.FLINT_SHARD, 3), 4));
        recipes.add(pickaxe("bone_shard_from_bone", Ingredient.of(Items.BONE), stack(Material.BONE_SHARD, 3), 4));
        recipes.add(pickaxe("brick_stone", Ingredient.of(Items.STONE_BRICK_SLAB), stack(Material.BRICK_STONE, 2), 4));
        recipes.add(pickaxe("stick_stone", Ingredient.of(item(Material.BRICK_STONE)), stack(Material.STICK_STONE, 4), 4));
        recipes.add(pickaxe("refractory_brick_slab", Ingredient.of(CoreBlocks.REFRACTORY_BRICK_BLOCK.get()),
            new ItemStack(CoreBlocks.REFRACTORY_BRICK_SLAB.get(), 2), 8));
        recipes.add(pickaxe("masonry_brick_slab", Ingredient.of(CoreBlocks.MASONRY_BRICK_BLOCK.get()),
            new ItemStack(CoreBlocks.MASONRY_BRICK_SLAB.get(), 2), 8));
        recipes.add(pickaxe("stone_brick_slab", Ingredient.of(Items.STONE_BRICKS), new ItemStack(Items.STONE_BRICK_SLAB, 2), 8));
        recipes.add(pickaxe("cobblestone_slab", Ingredient.of(Items.COBBLESTONE), new ItemStack(Items.COBBLESTONE_SLAB, 2), 8));
        // The 1.12 stone slab is 1.14's smooth stone slab; the stone-textured slab gets its own route (tech/basic sign-off, item 1).
        recipes.add(pickaxe("stone_slab", Ingredient.of(Items.STONE), new ItemStack(Items.SMOOTH_STONE_SLAB, 2), 8));
        recipes.add(pickaxe("stone_slab_rough", Ingredient.of(Items.STONE), new ItemStack(Items.STONE_SLAB, 2), 8));
        recipes.add(pickaxe("sandstone_slab", Ingredient.of(Items.SANDSTONE, Items.CHISELED_SANDSTONE, Items.CUT_SANDSTONE),
            new ItemStack(Items.SANDSTONE_SLAB, 2), 8));
        recipes.add(pickaxe("red_sandstone_slab", Ingredient.of(Items.RED_SANDSTONE, Items.CHISELED_RED_SANDSTONE, Items.CUT_RED_SANDSTONE),
            new ItemStack(Items.RED_SANDSTONE_SLAB, 2), 8));
        recipes.add(pickaxe("brick_slab", Ingredient.of(Items.BRICKS), new ItemStack(Items.BRICK_SLAB, 2), 8));
        recipes.add(pickaxe("nether_brick_slab", Ingredient.of(Items.NETHER_BRICKS), new ItemStack(Items.NETHER_BRICK_SLAB, 2), 8));
        recipes.add(pickaxe("quartz_slab", Ingredient.of(Items.QUARTZ_BLOCK, Items.CHISELED_QUARTZ_BLOCK, Items.QUARTZ_PILLAR),
            new ItemStack(Items.QUARTZ_SLAB, 2), 8));
        recipes.add(pickaxe("purpur_slab", Ingredient.of(Items.PURPUR_BLOCK), new ItemStack(Items.PURPUR_SLAB, 2), 8));
        recipes.add(pickaxe("bone_shard_from_block", Ingredient.of(Items.BONE_BLOCK), stack(Material.BONE_SHARD, 8), 8));
        recipes.add(pickaxe("iron_shard", Ingredient.of(Items.IRON_INGOT), stack(Material.IRON_SHARD, 9), 8));
        recipes.add(pickaxe("iron_ingot", Ingredient.of(Items.IRON_BLOCK), new ItemStack(Items.IRON_INGOT, 9), 8));
        recipes.add(pickaxe("gold_shard", Ingredient.of(Items.GOLD_INGOT), stack(Material.GOLD_SHARD, 9), 8));
        recipes.add(pickaxe("gold_ingot", Ingredient.of(Items.GOLD_BLOCK), new ItemStack(Items.GOLD_INGOT, 9), 8));
        recipes.add(pickaxe("diamond_shard", Ingredient.of(Items.DIAMOND), stack(Material.DIAMOND_SHARD, 9), 16));
        recipes.add(pickaxe("coal_block_to_coal", Ingredient.of(Items.COAL_BLOCK), new ItemStack(Items.COAL, 9), 8));
        recipes.add(pickaxe("coal_coke_block_to_coal_coke", Ingredient.of(CoreBlocks.COAL_COKE_BLOCK.get()), stack(Material.COAL_COKE, 9), 8));
        recipes.add(pickaxe("charcoal_block_to_charcoal", Ingredient.of(CoreBlocks.CHARCOAL_BLOCK.get()), new ItemStack(Items.CHARCOAL, 9), 8));
        // The five decompressions core removes from the crafting table (bloomery sign-off, item 7).
        recipes.add(pickaxe("raw_iron", Ingredient.of(Items.RAW_IRON_BLOCK), new ItemStack(Items.RAW_IRON, 9), 8));
        recipes.add(pickaxe("raw_gold", Ingredient.of(Items.RAW_GOLD_BLOCK), new ItemStack(Items.RAW_GOLD, 9), 8));
        recipes.add(pickaxe("raw_copper", Ingredient.of(Items.RAW_COPPER_BLOCK), new ItemStack(Items.RAW_COPPER, 9), 8));
        recipes.add(pickaxe("copper_ingot", Ingredient.of(Items.COPPER_BLOCK), new ItemStack(Items.COPPER_INGOT, 9), 8));
        recipes.add(pickaxe("copper_ingot_from_waxed_copper_block", Ingredient.of(Items.WAXED_COPPER_BLOCK), new ItemStack(Items.COPPER_INGOT, 9), 8));
    }

    private static Anvil hammer(String name, Ingredient input, ItemStack result, int hits) {
        return new Anvil(name, input, result, hits, AnvilToolType.HAMMER);
    }

    private static Anvil pickaxe(String name, Ingredient input, ItemStack result, int hits) {
        return new Anvil(name, input, result, hits, AnvilToolType.PICKAXE);
    }

    // -- Compacting bin ------------------------------------------------------

    private void compactingBin(RecipeOutput output) {
        this.compacting(output, "netherrack", Ingredient.of(CoreBlocks.ROCK_NETHERRACK.get()), Items.NETHERRACK, 8);
        this.compacting(output, "ash_pile", Ingredient.of(item(Material.PIT_ASH)), CoreBlocks.PILE_ASH.get(), 8);
        this.compacting(output, "lapis_block", Ingredient.of(Items.LAPIS_LAZULI), Items.LAPIS_BLOCK, 9);
        this.compacting(output, "redstone_block", Ingredient.of(Items.REDSTONE), Items.REDSTONE_BLOCK, 9);
        this.compacting(output, "charcoal_block_from_flakes", Ingredient.of(item(Material.CHARCOAL_FLAKES)), CoreBlocks.CHARCOAL_BLOCK.get(), 72);
        this.compacting(output, "gravel", Ingredient.of(CoreBlocks.ROCK_STONE.get(), CoreBlocks.ROCK_DIORITE.get(),
            CoreBlocks.ROCK_GRANITE.get(), CoreBlocks.ROCK_ANDESITE.get()), Items.GRAVEL, 8);
        this.compacting(output, "dirt", Ingredient.of(CoreBlocks.ROCK_DIRT.get()), Items.DIRT, 8);
        this.compacting(output, "mud", Ingredient.of(CoreBlocks.ROCK_MUD.get()), CoreBlocks.MUD.get(), 8);
        this.compacting(output, "charcoal_block", Ingredient.of(Items.CHARCOAL), CoreBlocks.CHARCOAL_BLOCK.get(), 9);
        this.compacting(output, "sand", Ingredient.of(CoreBlocks.ROCK_SAND.get()), Items.SAND, 8);
        this.compacting(output, "sand_red", Ingredient.of(CoreBlocks.ROCK_SAND_RED.get()), Items.RED_SAND, 8);
        this.compacting(output, "grass", Ingredient.of(CoreBlocks.ROCK_GRASS.get()), Items.GRASS_BLOCK, 8);
        this.compacting(output, "clay", Ingredient.of(Items.CLAY_BALL), Items.CLAY, 4);
        this.compacting(output, "snow", Ingredient.of(Items.SNOWBALL), Items.SNOW_BLOCK, 4);
        this.compacting(output, "bone_block", Ingredient.of(Items.BONE_MEAL), Items.BONE_BLOCK, 8);
        this.compacting(output, "pile_wood_chips", Ingredient.of(CoreBlocks.ROCK_WOOD_CHIPS.get()), CoreBlocks.PILE_WOOD_CHIPS.get(), 8);
    }

    private void compacting(RecipeOutput output, String name, Ingredient input, ItemLike result, int amount) {
        output.accept(id("compacting_bin/" + name), new CompactingBinRecipe(input, new ItemStack(result), amount,
            CompactingBinRecipe.DEFAULT_TOOL_USES, TechBasicRecipeTypes.COMPACTING_BIN, TechBasicRecipeSerializers.COMPACTING_BIN), null);
    }

    // -- Compost bin ---------------------------------------------------------

    /** The 1.12 mulch list: a value of one each, except the pumpkin at four; the wood's leaves, saplings, and flowers as tags. */
    private void compostBin(RecipeOutput output) {
        for (ItemLike item : List.of(Items.WHEAT, Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS, Items.STRING,
            Items.SLIME_BALL, Items.SUGAR_CANE, Items.PAPER, Items.FEATHER, Items.EGG, Items.NETHER_WART, Items.COBWEB, Items.DEAD_BUSH,
            Items.BROWN_MUSHROOM, Items.RED_MUSHROOM, Items.CACTUS, Items.VINE, Items.LILY_PAD, Items.TALL_GRASS, Items.LARGE_FERN,
            Items.SHORT_GRASS, Items.FERN, CoreBlocks.ROCK_WOOD_CHIPS.get(), item(Material.PIT_ASH), item(Material.PLANT_FIBERS),
            item(Material.PLANT_FIBERS_DRIED), item(Material.TWINE), item(Material.STRAW))) {
            this.compost(output, path(item), Ingredient.of(item), 1);
        }
        this.compost(output, "pumpkin", Ingredient.of(Items.PUMPKIN), 4);
        this.compost(output, "leaves", Ingredient.of(ItemTags.LEAVES), 1);
        this.compost(output, "saplings", Ingredient.of(ItemTags.SAPLINGS), 1);
        this.compost(output, "small_flowers", Ingredient.of(ItemTags.SMALL_FLOWERS), 1);
        this.compost(output, "tall_flowers", Ingredient.of(ItemTags.TALL_FLOWERS), 1);
    }

    private void compost(RecipeOutput output, String name, Ingredient input, int value) {
        output.accept(id("compost_bin/" + name),
            new CompostBinRecipe(input, value, new ItemStack(CoreItems.MULCH.get(), CompostValues.MULCH_PER_RECIPE)), null);
    }

    // -- Soaking pot ---------------------------------------------------------

    /** The seventeen non-tar recipes; the twelve tar ones are refractory's (tech/basic sign-off, item 13). */
    private void soakingPot(RecipeOutput output) {
        Fluid clay = CoreFluids.LIQUID_CLAY.source().get();
        this.soak(output, "clay_blasting", Ingredient.of(Items.GUNPOWDER), SizedFluidIngredient.of(clay, 125), stack(Material.CLAY_BLASTING), false, 5 * MINUTE);
        this.soak(output, "dough", Ingredient.of(item(Material.FLOUR)), water(125), stack(Material.DOUGH), false, 7 * MINUTE);
        this.soak(output, "hide_tanned", Ingredient.of(HuntingItems.HIDE_WASHED.get()), SizedFluidIngredient.of(HuntingTags.Fluids.TANNIN, 500),
            new ItemStack(HuntingItems.HIDE_TANNED.get()), false, 10 * MINUTE);
        this.soak(output, "hide_small_tanned", Ingredient.of(HuntingItems.HIDE_SMALL_WASHED.get()), SizedFluidIngredient.of(HuntingTags.Fluids.TANNIN, 250),
            new ItemStack(HuntingItems.HIDE_SMALL_TANNED.get()), false, 5 * MINUTE);
        this.soak(output, "hide_small_washed", Ingredient.of(HuntingItems.HIDE_SMALL_SCRAPED.get()), water(250),
            new ItemStack(HuntingItems.HIDE_SMALL_WASHED.get()), true, 2 * MINUTE);
        this.soak(output, "hide_washed", Ingredient.of(HuntingItems.HIDE_SCRAPED.get()), water(250),
            new ItemStack(HuntingItems.HIDE_WASHED.get()), true, 2 * MINUTE);
        this.soak(output, "sponge", Ingredient.of(Items.SPONGE), water(1000), new ItemStack(Items.WET_SPONGE), false, 1);
        this.soak(output, "mud", Ingredient.of(Items.DIRT, Items.COARSE_DIRT, Items.PODZOL), water(250), new ItemStack(CoreBlocks.MUD.get()), false, 7 * MINUTE);
        this.soak(output, "mud_clump", Ingredient.of(CoreBlocks.ROCK_DIRT.get()), water(125), new ItemStack(CoreBlocks.ROCK_MUD.get()), false, 4 * MINUTE);
        this.soak(output, "flint_clay", Ingredient.of(item(Material.DUST_FLINT)), SizedFluidIngredient.of(clay, 250), stack(Material.FLINT_CLAY_BALL), false, 5 * MINUTE);
        this.soak(output, "pulp_from_reeds", Ingredient.of(Items.SUGAR_CANE), water(125), stack(Material.PULP), false, 4 * MINUTE);
        this.soak(output, "pulp_from_wood_chips", Ingredient.of(CoreBlocks.ROCK_WOOD_CHIPS.get()), water(500), stack(Material.PULP), false, 7 * MINUTE);
        this.soak(output, "slaked_lime", Ingredient.of(item(Material.QUICKLIME)), water(125), stack(Material.SLAKED_LIME), false, 7 * MINUTE);
        this.soak(output, "podzol", Ingredient.of(Items.COARSE_DIRT), water(250), new ItemStack(Items.PODZOL), false, 7 * MINUTE);
        this.soak(output, "mossy_stone_bricks", Ingredient.of(Items.STONE_BRICKS), water(250), new ItemStack(Items.MOSSY_STONE_BRICKS), false, 7 * MINUTE);
        this.soak(output, "mossy_cobblestone", Ingredient.of(Items.COBBLESTONE), water(250), new ItemStack(Items.MOSSY_COBBLESTONE), false, 7 * MINUTE);
        this.soak(output, "white_wool", Ingredient.of(ItemTags.WOOL), water(250), new ItemStack(Items.WHITE_WOOL), false, 4 * MINUTE);
    }

    private void soak(RecipeOutput output, String name, Ingredient input, SizedFluidIngredient fluid, ItemStack result, boolean campfire, int ticks) {
        output.accept(id("soaking_pot/" + name), new SoakingPotRecipe(input, fluid, result, campfire, ticks), null);
    }

    private static SizedFluidIngredient water(int amount) {
        return SizedFluidIngredient.of(Fluids.WATER, amount);
    }

    // -- Barrel --------------------------------------------------------------

    private void barrel(RecipeOutput output) {
        Ingredient leaves = Ingredient.of(ItemTags.LEAVES);
        output.accept(id("barrel/tannin"), new BarrelRecipe(List.of(leaves, leaves, leaves, leaves), water(1000),
            new FluidStack(com.moostoet.pyrotech.hunting.HuntingFluids.TANNIN.source().get(), 1000), 10 * MINUTE), null);
        this.wine(output, "pyroberry_wine", CoreItems.PYROBERRIES.get(), CoreFluids.PYROBERRY_WINE.source().get());
        this.wine(output, "gloamberry_wine", CoreItems.GLOAMBERRIES.get(), CoreFluids.GLOAMBERRY_WINE.source().get());
        this.wine(output, "freckleberry_wine", CoreItems.FRECKLEBERRIES.get(), CoreFluids.FRECKLEBERRY_WINE.source().get());
    }

    private void wine(RecipeOutput output, String name, Item berries, Fluid wine) {
        Ingredient berry = Ingredient.of(berries);
        output.accept(id("barrel/" + name), new BarrelRecipe(List.of(berry, berry, berry, Ingredient.of(Items.SUGAR)), water(1000),
            new FluidStack(wine, 1000), 15 * MINUTE), null);
    }

    // -- Tanning rack --------------------------------------------------------

    private void tanningRack(RecipeOutput output) {
        output.accept(id("tanning_rack/leather"), new TanningRackRecipe(Ingredient.of(HuntingItems.HIDE_TANNED.get()), new ItemStack(Items.LEATHER),
            Optional.of(new ItemStack(HuntingItems.HIDE_WASHED.get())), 10 * MINUTE), null);
        output.accept(id("tanning_rack/leather_small"), new TanningRackRecipe(Ingredient.of(HuntingItems.HIDE_SMALL_TANNED.get()),
            stack(Material.LEATHER_SMALL), Optional.of(new ItemStack(HuntingItems.HIDE_SMALL_WASHED.get())), 10 * MINUTE), null);
    }

    // -- Helpers -------------------------------------------------------------

    private static Item item(Material material) {
        return CoreItems.material(material).get();
    }

    private static ItemStack stack(Material material) {
        return new ItemStack(item(material));
    }

    private static ItemStack stack(Material material, int count) {
        return new ItemStack(item(material), count);
    }

    private static String hasName(Material material) {
        return getHasName(item(material));
    }

    private static net.minecraft.advancements.Criterion<?> has(Material material) {
        return has(item(material));
    }

    private static Item vanilla(String name) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(name));
    }

    private static String path(ItemLike item) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, name);
    }
}
