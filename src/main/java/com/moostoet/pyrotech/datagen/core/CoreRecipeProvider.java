package com.moostoet.pyrotech.datagen.core;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.core.CoreFluids;
import com.moostoet.pyrotech.core.CoreItems;
import com.moostoet.pyrotech.core.Material;
import com.moostoet.pyrotech.core.PyrotechTags;
import com.moostoet.pyrotech.core.recipe.FluidContainerIngredient;
import com.moostoet.pyrotech.datagen.RecipeUnit;
import com.moostoet.pyrotech.datagen.ToolDamageShapelessRecipeBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;

import java.util.concurrent.CompletableFuture;

/**
 * Core's recipes: the 1.12 crafting JSON whose type, ingredients, and result are all
 * core's or vanilla's (the placement rule of the recipe architecture sign-off), the
 * furnace recipes {@code VanillaFurnaceRecipesAdd} registered in code, and the vanilla
 * replacements the removal list owes: the nine boats and the two torches. Ids are the
 * 1.12 file names; a smelting recipe is named {@code <result>_from_smelting}.
 * <p>
 * The 1.12 ore dictionary names become tags: {@code stickWood} is {@code c:rods/wooden},
 * {@code plankWood} and {@code logWood} the vanilla planks and logs, {@code rock} is
 * {@code #pyrotech:rocks}, {@code stickStone} is {@code #pyrotech:stone_sticks}, and
 * {@code twine} is {@code #pyrotech:twine}. A name that held one item is that item.
 */
public final class CoreRecipeProvider extends RecipeProvider implements RecipeUnit {

    private static final TagKey<Item> STICK = Tags.Items.RODS_WOODEN;
    private static final int SMELT_TICKS = 200;

    public CoreRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    public void buildRecipes(RecipeOutput output) {
        this.materials(output);
        this.blocks(output);
        this.tools(output);
        this.vanillaReplacements(output);
        this.foods(output);
        this.plants(output);
        this.smelting(output);
    }

    // -- The material items ------------------------------------------------

    private void materials(RecipeOutput output) {
        shapeless(RecipeCategory.MISC, item(Material.CLAY_LUMP), 4).requires(Items.CLAY_BALL)
            .unlockedBy(getHasName(Items.CLAY_BALL), has(Items.CLAY_BALL)).save(output, id("clay_lump"));
        shapeless(RecipeCategory.MISC, Items.CLAY_BALL, 1).requires(item(Material.CLAY_LUMP), 4)
            .unlockedBy(hasName(Material.CLAY_LUMP), has(Material.CLAY_LUMP)).save(output, id("clay_ball"));
        shapeless(RecipeCategory.MISC, Items.CLAY_BALL, 1).requires(Items.DIRT, 2).requires(water())
            .unlockedBy(getHasName(Items.DIRT), has(Items.DIRT)).save(output, id("clay"));
        shaped(RecipeCategory.MISC, item(Material.UNFIRED_BRICK), 1).pattern("CC").define('C', item(Material.CLAY_LUMP))
            .unlockedBy(hasName(Material.CLAY_LUMP), has(Material.CLAY_LUMP)).save(output, id("unfired_brick"));

        shaped(RecipeCategory.MISC, item(Material.REFRACTORY_CLAY_BALL), 5)
            .pattern("CAC").pattern("LFL").pattern("CAC")
            .define('A', item(Material.PIT_ASH))
            .define('F', item(Material.FLINT_CLAY_BALL))
            .define('L', item(Material.SLAKED_LIME))
            .define('C', Items.CLAY_BALL)
            .unlockedBy(hasName(Material.FLINT_CLAY_BALL), has(Material.FLINT_CLAY_BALL)).save(output, id("refractory_clay_ball"));
        shaped(RecipeCategory.MISC, item(Material.REFRACTORY_CLAY_BALL), 1).pattern("CC").pattern("CC")
            .define('C', item(Material.REFRACTORY_CLAY_LUMP))
            .unlockedBy(hasName(Material.REFRACTORY_CLAY_LUMP), has(Material.REFRACTORY_CLAY_LUMP))
            .save(output, id("refractory_clay_ball_from_refractory_clay_lump"));
        shapeless(RecipeCategory.MISC, item(Material.REFRACTORY_CLAY_LUMP), 4).requires(item(Material.REFRACTORY_CLAY_BALL))
            .unlockedBy(hasName(Material.REFRACTORY_CLAY_BALL), has(Material.REFRACTORY_CLAY_BALL)).save(output, id("refractory_clay_lump"));
        shapeless(RecipeCategory.MISC, item(Material.REFRACTORY_CLAY_LUMP), 2).requires(item(Material.UNFIRED_REFRACTORY_BRICK))
            .unlockedBy(hasName(Material.UNFIRED_REFRACTORY_BRICK), has(Material.UNFIRED_REFRACTORY_BRICK))
            .save(output, id("refractory_clay_lump_from_unfired_refractory_brick"));
        shaped(RecipeCategory.MISC, item(Material.UNFIRED_REFRACTORY_BRICK), 1).pattern("CC").define('C', item(Material.REFRACTORY_CLAY_LUMP))
            .unlockedBy(hasName(Material.REFRACTORY_CLAY_LUMP), has(Material.REFRACTORY_CLAY_LUMP)).save(output, id("refractory_brick_unfired"));

        shapeless(RecipeCategory.MISC, item(Material.PLANT_FIBERS), 1).requires(CoreBlocks.ROCK_GRASS.get())
            .unlockedBy(getHasName(CoreBlocks.ROCK_GRASS.get()), has(CoreBlocks.ROCK_GRASS.get())).save(output, id("plant_fibers"));
        shapeless(RecipeCategory.MISC, item(Material.PLANT_FIBERS), 1).requires(Items.SHORT_GRASS)
            .unlockedBy(getHasName(Items.SHORT_GRASS), has(Items.SHORT_GRASS)).save(output, id("plant_fibers_from_tallgrass"));
        shapeless(RecipeCategory.MISC, item(Material.PLANT_FIBERS_DRIED), 2).requires(item(Material.STRAW))
            .unlockedBy(hasName(Material.STRAW), has(Material.STRAW)).save(output, id("dried_plant_fibers"));
        shapeless(RecipeCategory.MISC, item(Material.TWINE), 3).requires(item(Material.PLANT_FIBERS_DRIED), 3)
            .unlockedBy(hasName(Material.PLANT_FIBERS_DRIED), has(Material.PLANT_FIBERS_DRIED)).save(output, id("twine"));
        shapeless(RecipeCategory.MISC, item(Material.STRAW), 1).requires(item(Material.PLANT_FIBERS_DRIED), 2).requires(PyrotechTags.Items.TWINE)
            .unlockedBy(hasName(Material.PLANT_FIBERS_DRIED), has(Material.PLANT_FIBERS_DRIED)).save(output, id("straw"));
        shapeless(RecipeCategory.MISC, item(Material.STRAW), 4).requires(CoreBlocks.THATCH.get())
            .unlockedBy(getHasName(CoreBlocks.THATCH.get()), has(CoreBlocks.THATCH.get())).save(output, id("straw_from_thatch"));
        ToolDamageShapelessRecipeBuilder.toolDamageShapeless(RecipeCategory.MISC, item(Material.STRAW), 1, Ingredient.of(Tags.Items.TOOLS_SHEAR), 1)
            .requires(Tags.Items.CROPS_WHEAT)
            .unlockedBy("has_wheat", has(Tags.Items.CROPS_WHEAT)).save(output, id("shearing_wheat"));
        shaped(RecipeCategory.MISC, item(Material.KINDLING), 1).pattern("BBB").pattern("BEB").pattern("BBB")
            .define('E', PyrotechTags.Items.TWINE).define('B', STICK)
            .unlockedBy("has_twine", has(PyrotechTags.Items.TWINE)).save(output, id("kindling"));
        shaped(RecipeCategory.MISC, item(Material.STONE_TOOL_SHAFT), 1).pattern("SLC")
            .define('S', PyrotechTags.Items.STONE_STICKS)
            .define('L', item(Material.LEATHER_STRAP))
            .define('C', item(Material.LEATHER_CORD))
            .unlockedBy("has_stone_stick", has(PyrotechTags.Items.STONE_STICKS)).save(output, id("stone_tool_shaft"));

        shaped(RecipeCategory.MISC, CoreItems.FURNACE_CORE.get(), 16).pattern("III").pattern("BBB").pattern("III")
            .define('I', Tags.Items.INGOTS_IRON).define('B', Items.BLAZE_ROD)
            .unlockedBy(getHasName(Items.BLAZE_ROD), has(Items.BLAZE_ROD)).save(output, id("furnace_core"));
        shaped(RecipeCategory.MISC, CoreBlocks.CRAFTING_TABLE_TEMPLATE.get(), 1).pattern("III").pattern("I I").pattern("III")
            .define('I', Tags.Items.INGOTS_IRON)
            .unlockedBy("has_iron_ingot", has(Tags.Items.INGOTS_IRON)).save(output, id("crafting_table_template"));
    }

    // -- The blocks --------------------------------------------------------

    private void blocks(RecipeOutput output) {
        cobbled(output, "cobblestone", Items.COBBLESTONE, CoreBlocks.ROCK_STONE.get());
        cobbled(output, "cobbled_andesite", CoreBlocks.COBBLESTONE_ANDESITE.get(), CoreBlocks.ROCK_ANDESITE.get());
        cobbled(output, "cobbled_diorite", CoreBlocks.COBBLESTONE_DIORITE.get(), CoreBlocks.ROCK_DIORITE.get());
        cobbled(output, "cobbled_granite", CoreBlocks.COBBLESTONE_GRANITE.get(), CoreBlocks.ROCK_GRANITE.get());
        cobbled(output, "cobbled_limestone", CoreBlocks.COBBLESTONE_LIMESTONE.get(), CoreBlocks.ROCK_LIMESTONE.get());
        shaped(RecipeCategory.BUILDING_BLOCKS, Items.COBBLESTONE_SLAB, 1).pattern("RRR").pattern("RCR")
            .define('R', CoreBlocks.ROCK_STONE.get()).define('C', item(Material.CLAY_LUMP))
            .unlockedBy(getHasName(CoreBlocks.ROCK_STONE.get()), has(CoreBlocks.ROCK_STONE.get())).save(output, id("cobblestone_slab"));

        shaped(RecipeCategory.MISC, CoreBlocks.ROCK_MUD.get(), 8).pattern("DDD").pattern("DFD").pattern("DDD")
            .define('D', CoreBlocks.ROCK_DIRT.get()).define('F', water())
            .unlockedBy(getHasName(CoreBlocks.ROCK_DIRT.get()), has(CoreBlocks.ROCK_DIRT.get())).save(output, id("mud"));
        shapeless(RecipeCategory.BUILDING_BLOCKS, CoreBlocks.COB_WET.get(), 1)
            .requires(item(Material.PLANT_FIBERS_DRIED)).requires(CoreBlocks.ROCK_MUD.get(), 3)
            .unlockedBy(getHasName(CoreBlocks.ROCK_MUD.get()), has(CoreBlocks.ROCK_MUD.get())).save(output, id("cob_2x2a"));
        shapeless(RecipeCategory.BUILDING_BLOCKS, CoreBlocks.COB_WET.get(), 16)
            .requires(CoreBlocks.THATCH.get()).requires(CoreBlocks.MUD.get(), 3)
            .unlockedBy(getHasName(CoreBlocks.MUD.get()), has(CoreBlocks.MUD.get())).save(output, id("cob_2x2b"));
        shaped(RecipeCategory.BUILDING_BLOCKS, CoreBlocks.COB_WET.get(), 4).pattern("DDD").pattern("DFD").pattern("DDD")
            .define('D', CoreBlocks.ROCK_MUD.get()).define('F', item(Material.STRAW))
            .unlockedBy(getHasName(CoreBlocks.ROCK_MUD.get()), has(CoreBlocks.ROCK_MUD.get())).save(output, id("cob_3x3"));

        shaped(RecipeCategory.BUILDING_BLOCKS, CoreBlocks.THATCH.get(), 1).pattern("AA").pattern("AA").define('A', item(Material.STRAW))
            .unlockedBy(hasName(Material.STRAW), has(Material.STRAW)).save(output, id("thatch"));
        shaped(RecipeCategory.DECORATIONS, CoreBlocks.STRAW_BED.get(), 1).pattern("AAA").define('A', item(Material.STRAW))
            .unlockedBy(hasName(Material.STRAW), has(Material.STRAW)).save(output, id("straw_bed"));
        shaped(RecipeCategory.BUILDING_BLOCKS, CoreBlocks.LOG_PILE.get(), 1).pattern("AAA").pattern("AAA").pattern("AAA").define('A', ItemTags.LOGS)
            .unlockedBy("has_log", has(ItemTags.LOGS)).save(output, id("log_pile"));

        brickSet(output, "masonry_brick", item(Material.BRICK_STONE), CoreBlocks.MASONRY_BRICK_BLOCK.get(),
            CoreBlocks.MASONRY_BRICK_STAIRS.get(), CoreBlocks.MASONRY_BRICK_WALL.get(), CoreBlocks.STONE_DOOR.get(), "stone_door");
        brickSet(output, "refractory_brick", item(Material.REFRACTORY_BRICK), CoreBlocks.REFRACTORY_BRICK_BLOCK.get(),
            CoreBlocks.REFRACTORY_BRICK_STAIRS.get(), CoreBlocks.REFRACTORY_BRICK_WALL.get(), CoreBlocks.REFRACTORY_DOOR.get(), "refractory_door");
        shaped(RecipeCategory.BUILDING_BLOCKS, CoreBlocks.REFRACTORY_GLASS.get(), 1).pattern(" A ").pattern("AGA").pattern(" A ")
            .define('A', item(Material.REFRACTORY_BRICK)).define('G', Tags.Items.GLASS_BLOCKS)
            .unlockedBy(hasName(Material.REFRACTORY_BRICK), has(Material.REFRACTORY_BRICK)).save(output, id("refractory_glass"));
    }

    private static void cobbled(RecipeOutput output, String name, ItemLike result, ItemLike rock) {
        shaped(RecipeCategory.BUILDING_BLOCKS, result, 1).pattern("RRR").pattern("RCR").pattern("RRR")
            .define('C', item(Material.CLAY_LUMP)).define('R', rock)
            .unlockedBy(getHasName(rock), has(rock)).save(output, id(name));
    }

    /** The 1.12 brick set: block from four bricks, stairs and wall from the block, and a door from six bricks. */
    private static void brickSet(RecipeOutput output, String name, Item brick, ItemLike block, ItemLike stairs, ItemLike wall,
                                 ItemLike door, String doorName) {
        Criterion<?> hasBrick = has(brick);
        shaped(RecipeCategory.BUILDING_BLOCKS, block, 1).pattern("AA").pattern("AA").define('A', brick)
            .unlockedBy(getHasName(brick), hasBrick).save(output, id(name + "_block"));
        shaped(RecipeCategory.BUILDING_BLOCKS, stairs, 4).pattern("#  ").pattern("## ").pattern("###").define('#', block)
            .unlockedBy(getHasName(block), has(block)).save(output, id(name + "_stairs"));
        shaped(RecipeCategory.DECORATIONS, wall, 6).pattern("###").pattern("###").define('#', block)
            .unlockedBy(getHasName(block), has(block)).save(output, id(name + "_wall"));
        shaped(RecipeCategory.REDSTONE, door, 1).pattern("AA").pattern("AA").pattern("AA").define('A', brick)
            .unlockedBy(getHasName(brick), hasBrick).save(output, id(doorName));
    }

    // -- The hammers and the vanilla tools -------------------------------------

    private void tools(RecipeOutput output) {
        hammer(output, "crude_hammer", CoreItems.CRUDE_HAMMER.get());
        hammer(output, "stone_hammer", CoreItems.STONE_HAMMER.get(), Ingredient.of(item(Material.BRICK_STONE)), Ingredient.of(PyrotechTags.Items.TWINE), has(Material.BRICK_STONE));
        hammer(output, "bone_hammer", CoreItems.BONE_HAMMER.get(), Ingredient.of(item(Material.BONE_SHARD)), Ingredient.of(PyrotechTags.Items.TWINE), has(Material.BONE_SHARD));
        hammer(output, "flint_hammer", CoreItems.FLINT_HAMMER.get(), Ingredient.of(item(Material.FLINT_SHARD)), Ingredient.of(PyrotechTags.Items.TWINE), has(Material.FLINT_SHARD));
        hammer(output, "iron_hammer", CoreItems.IRON_HAMMER.get(), Ingredient.of(Tags.Items.INGOTS_IRON), Ingredient.of(item(Material.TWINE_DURABLE)), has(Tags.Items.INGOTS_IRON));
        hammer(output, "gold_hammer", CoreItems.GOLD_HAMMER.get(), Ingredient.of(Tags.Items.INGOTS_GOLD), Ingredient.of(item(Material.TWINE_DURABLE)), has(Tags.Items.INGOTS_GOLD));
        hammer(output, "diamond_hammer", CoreItems.DIAMOND_HAMMER.get(), Ingredient.of(Items.DIAMOND), Ingredient.of(item(Material.TWINE_DURABLE)), has(Items.DIAMOND));
        hammer(output, "obsidian_hammer", CoreItems.OBSIDIAN_HAMMER.get(), Ingredient.of(item(Material.OBSIDIAN_SHARD)), Ingredient.of(item(Material.TWINE_DURABLE)), has(Material.OBSIDIAN_SHARD));
        durableHammer(output, "bone_hammer_durable", CoreItems.BONE_HAMMER_DURABLE.get(), Items.BONE);
        durableHammer(output, "flint_hammer_durable", CoreItems.FLINT_HAMMER_DURABLE.get(), Items.FLINT);

        Item brick = item(Material.BRICK_STONE);
        Criterion<?> hasBrick = has(Material.BRICK_STONE);
        shaped(RecipeCategory.TOOLS, Items.STONE_AXE, 1).pattern("RR").pattern("RS").pattern(" S").define('R', brick).define('S', STICK)
            .unlockedBy(getHasName(brick), hasBrick).save(output, id("stone_axe"));
        shaped(RecipeCategory.TOOLS, Items.STONE_HOE, 1).pattern("RR").pattern(" S").pattern(" S").define('R', brick).define('S', STICK)
            .unlockedBy(getHasName(brick), hasBrick).save(output, id("stone_hoe"));
        shaped(RecipeCategory.TOOLS, Items.STONE_PICKAXE, 1).pattern("RRR").pattern(" S ").pattern(" S ").define('R', brick).define('S', STICK)
            .unlockedBy(getHasName(brick), hasBrick).save(output, id("stone_pickaxe"));
        shaped(RecipeCategory.TOOLS, Items.STONE_SHOVEL, 1).pattern("R").pattern("S").pattern("S").define('R', brick).define('S', STICK)
            .unlockedBy(getHasName(brick), hasBrick).save(output, id("stone_shovel"));
        shaped(RecipeCategory.COMBAT, Items.STONE_SWORD, 1).pattern("R").pattern("R").pattern("S").define('R', brick).define('S', STICK)
            .unlockedBy(getHasName(brick), hasBrick).save(output, id("stone_sword"));

        shaped(RecipeCategory.TOOLS, Items.SHEARS, 1).pattern("TM").pattern("M ")
            .define('M', item(Material.IRON_SHARD)).define('T', item(Material.LEATHER_STRAP))
            .unlockedBy(hasName(Material.IRON_SHARD), has(Material.IRON_SHARD)).save(output, id("shears"));
        shaped(RecipeCategory.COMBAT, Items.BOW, 1).pattern(" S|").pattern("S |").pattern(" S|")
            .define('|', item(Material.TWINE_DURABLE)).define('S', STICK)
            .unlockedBy(hasName(Material.TWINE_DURABLE), has(Material.TWINE_DURABLE)).save(output, id("bow"));
        shaped(RecipeCategory.COMBAT, Items.ARROW, 1).pattern("  ^").pattern(" / ").pattern("#  ")
            .define('/', STICK).define('#', item(Material.FLETCHING)).define('^', item(Material.IRON_SHARD))
            .unlockedBy(hasName(Material.FLETCHING), has(Material.FLETCHING)).save(output, id("arrow"));
    }

    /** The crude hammer: two rocks over a stick and plant fibers. */
    private static void hammer(RecipeOutput output, String name, Item hammer) {
        shaped(RecipeCategory.TOOLS, hammer, 1).pattern("RR").pattern("ST")
            .define('R', PyrotechTags.Items.ROCKS).define('T', item(Material.PLANT_FIBERS)).define('S', STICK)
            .unlockedBy(hasName(Material.PLANT_FIBERS), has(Material.PLANT_FIBERS)).save(output, id(name));
    }

    /** The 1.12 hammer shape: two heads and a binding around a stick, with a stick handle. */
    private static void hammer(RecipeOutput output, String name, Item hammer, Ingredient head, Ingredient binding, Criterion<?> unlock) {
        shaped(RecipeCategory.TOOLS, hammer, 1).pattern(" RF").pattern(" SR").pattern("S  ")
            .define('R', head).define('F', binding).define('S', STICK)
            .unlockedBy("has_head", unlock).save(output, id(name));
    }

    /** The durable hammers: two heads and a durable cord around a stone tool shaft. */
    private static void durableHammer(RecipeOutput output, String name, Item hammer, Item head) {
        shaped(RecipeCategory.TOOLS, hammer, 1).pattern(" R ").pattern(" CR").pattern("S  ")
            .define('R', head).define('S', item(Material.STONE_TOOL_SHAFT)).define('C', item(Material.LEATHER_DURABLE_CORD))
            .unlockedBy(hasName(Material.STONE_TOOL_SHAFT), has(Material.STONE_TOOL_SHAFT)).save(output, id(name));
    }

    // -- The vanilla items behind a Pyrotech gate --------------------------------

    private void vanillaReplacements(RecipeOutput output) {
        boat(output, "boat_oak", Items.OAK_BOAT, Items.OAK_PLANKS);
        boat(output, "boat_spruce", Items.SPRUCE_BOAT, Items.SPRUCE_PLANKS);
        boat(output, "boat_birch", Items.BIRCH_BOAT, Items.BIRCH_PLANKS);
        boat(output, "boat_jungle", Items.JUNGLE_BOAT, Items.JUNGLE_PLANKS);
        boat(output, "boat_acacia", Items.ACACIA_BOAT, Items.ACACIA_PLANKS);
        boat(output, "boat_dark_oak", Items.DARK_OAK_BOAT, Items.DARK_OAK_PLANKS);
        // The three boats vanilla added after 1.12 (progression skips sign-off, decision 1).
        boat(output, "boat_cherry", Items.CHERRY_BOAT, Items.CHERRY_PLANKS);
        boat(output, "boat_mangrove", Items.MANGROVE_BOAT, Items.MANGROVE_PLANKS);
        boat(output, "boat_bamboo", Items.BAMBOO_RAFT, Items.BAMBOO_PLANKS);

        // Coal only, as 1.12 had it: charcoal comes from the Pyrotech route.
        shaped(RecipeCategory.DECORATIONS, Items.TORCH, 4).pattern("W").pattern("|")
            .define('W', Items.COAL).define('|', STICK)
            .unlockedBy(getHasName(Items.COAL), has(Items.COAL)).save(output, id("torch_vanilla_coal"));
        shaped(RecipeCategory.DECORATIONS, Items.SOUL_TORCH, 4).pattern("W").pattern("|").pattern("S")
            .define('W', Items.COAL).define('|', STICK).define('S', ItemTags.SOUL_FIRE_BASE_BLOCKS)
            .unlockedBy(getHasName(Items.COAL), has(Items.COAL)).save(output, id("soul_torch_vanilla_coal"));

        shaped(RecipeCategory.DECORATIONS, Items.CRAFTING_TABLE, 1).pattern("PPP").pattern("PTP").pattern("PPP")
            .define('T', CoreBlocks.CRAFTING_TABLE_TEMPLATE.get()).define('P', ItemTags.PLANKS)
            .unlockedBy(getHasName(CoreBlocks.CRAFTING_TABLE_TEMPLATE.get()), has(CoreBlocks.CRAFTING_TABLE_TEMPLATE.get()))
            .save(output, id("crafting_table"));
        shaped(RecipeCategory.DECORATIONS, Items.FURNACE, 1).pattern("SSS").pattern("SFS").pattern("SSS")
            .define('S', Tags.Items.STONES).define('F', CoreItems.FURNACE_CORE.get())
            .unlockedBy(getHasName(CoreItems.FURNACE_CORE.get()), has(CoreItems.FURNACE_CORE.get())).save(output, id("furnace"));
        shaped(RecipeCategory.DECORATIONS, Items.CHEST, 1).pattern("PPP").pattern("PIP").pattern("PPP")
            .define('I', Tags.Items.INGOTS_IRON).define('P', ItemTags.PLANKS)
            .unlockedBy("has_iron_ingot", has(Tags.Items.INGOTS_IRON)).save(output, id("chest"));
        shapeless(RecipeCategory.MISC, Items.STICK, 2).requires(ItemTags.SAPLINGS)
            .unlockedBy("has_sapling", has(ItemTags.SAPLINGS)).save(output, id("stick"));
        shapeless(RecipeCategory.MISC, Items.FIRE_CHARGE, 1).requires(Items.GUNPOWDER).requires(Items.BLAZE_POWDER).requires(Items.COAL)
            .unlockedBy(getHasName(Items.BLAZE_POWDER), has(Items.BLAZE_POWDER)).save(output, id("fire_charge"));

        Item sheet = item(Material.LEATHER_SHEET);
        Item strap = item(Material.LEATHER_STRAP);
        Item cord = item(Material.LEATHER_CORD);
        Criterion<?> hasSheet = has(Material.LEATHER_SHEET);
        shapeless(RecipeCategory.MISC, Items.BOOK, 1).requires(Items.PAPER, 3).requires(sheet)
            .unlockedBy(getHasName(sheet), hasSheet).save(output, id("book"));
        shaped(RecipeCategory.DECORATIONS, Items.ITEM_FRAME, 1).pattern("###").pattern("#X#").pattern("###")
            .define('#', Items.STICK).define('X', sheet)
            .unlockedBy(getHasName(sheet), hasSheet).save(output, id("item_frame"));
        shaped(RecipeCategory.TOOLS, Items.LEAD, 1).pattern("~~ ").pattern("~S ").pattern("  ~")
            .define('~', cord).define('S', strap)
            .unlockedBy(getHasName(cord), has(Material.LEATHER_CORD)).save(output, id("lead"));
        shaped(RecipeCategory.TRANSPORTATION, Items.SADDLE, 1).pattern(" ~ ").pattern("LLL").pattern("SWS")
            .define('L', sheet).define('S', strap).define('~', cord).define('W', ItemTags.WOOL)
            .unlockedBy(getHasName(sheet), hasSheet).save(output, id("saddle"));
        shaped(RecipeCategory.COMBAT, Items.LEATHER_HELMET, 1).pattern("LLL").pattern("S S")
            .define('L', sheet).define('S', strap)
            .unlockedBy(getHasName(sheet), hasSheet).save(output, id("leather_helmet"));
        shaped(RecipeCategory.COMBAT, Items.LEATHER_CHESTPLATE, 1).pattern("S S").pattern("LLL").pattern("LLL")
            .define('L', sheet).define('S', strap)
            .unlockedBy(getHasName(sheet), hasSheet).save(output, id("leather_chestplate"));
        shaped(RecipeCategory.COMBAT, Items.LEATHER_LEGGINGS, 1).pattern("LCL").pattern("L L").pattern("S S")
            .define('L', sheet).define('S', strap).define('C', cord)
            .unlockedBy(getHasName(sheet), hasSheet).save(output, id("leather_leggings"));
        shaped(RecipeCategory.COMBAT, Items.LEATHER_BOOTS, 1).pattern("C C").pattern("L L")
            .define('L', sheet).define('C', cord)
            .unlockedBy(getHasName(sheet), hasSheet).save(output, id("leather_boots"));
    }

    /** The 1.12 boat: two planks over two tarred planks and a tarred board. */
    private static void boat(RecipeOutput output, String name, Item boat, Item planks) {
        shaped(RecipeCategory.TRANSPORTATION, boat, 1).pattern("P P").pattern("T-T")
            .define('P', planks).define('T', CoreBlocks.PLANKS_TARRED.get()).define('-', item(Material.BOARD_TARRED))
            .unlockedBy(getHasName(CoreBlocks.PLANKS_TARRED.get()), has(CoreBlocks.PLANKS_TARRED.get())).save(output, id(name));
    }

    // -- The foods and the plants -------------------------------------------

    private void foods(RecipeOutput output) {
        Item flour = item(Material.FLOUR);
        Item dough = item(Material.DOUGH);
        shapeless(RecipeCategory.FOOD, dough, 3).requires(flour, 3).requires(water())
            .unlockedBy(getHasName(flour), has(Material.FLOUR)).save(output, id("dough"));
        shapeless(RecipeCategory.FOOD, item(Material.BREAD_DOUGH), 1).requires(dough, 3)
            .unlockedBy(getHasName(dough), has(Material.DOUGH)).save(output, id("bread_dough"));
        shapeless(RecipeCategory.FOOD, item(Material.COOKIE_DOUGH), 1).requires(dough, 2).requires(Items.COCOA_BEANS)
            .unlockedBy(getHasName(dough), has(Material.DOUGH)).save(output, id("cookie_dough"));
        shaped(RecipeCategory.FOOD, Items.CAKE, 1).pattern("MMM").pattern("SES").pattern("FFF")
            .define('M', Tags.Items.BUCKETS_MILK).define('S', Items.SUGAR).define('E', Tags.Items.EGGS).define('F', flour)
            .unlockedBy(getHasName(flour), has(Material.FLOUR)).save(output, id("cake"));
    }

    private void plants(RecipeOutput output) {
        shapeless(RecipeCategory.MISC, CoreItems.PYROBERRY_SEEDS.get(), 1).requires(CoreItems.PYROBERRIES.get(), 3)
            .unlockedBy(getHasName(CoreItems.PYROBERRIES.get()), has(CoreItems.PYROBERRIES.get())).save(output, id("pyroberry_seeds"));
        shapeless(RecipeCategory.MISC, CoreItems.GLOAMBERRY_SEEDS.get(), 1).requires(CoreItems.GLOAMBERRIES.get(), 3)
            .unlockedBy(getHasName(CoreItems.GLOAMBERRIES.get()), has(CoreItems.GLOAMBERRIES.get())).save(output, id("gloamberry_seeds"));
        shapeless(RecipeCategory.MISC, CoreItems.FRECKLEBERRY_SEEDS.get(), 3).requires(CoreItems.FRECKLEBERRIES.get())
            .unlockedBy(getHasName(CoreItems.FRECKLEBERRIES.get()), has(CoreItems.FRECKLEBERRIES.get())).save(output, id("freckleberry_seeds"));

        wine(output, "pyroberry_wine", CoreItems.PYROBERRY_WINE.get(), CoreFluids.PYROBERRY_WINE.source().get());
        wine(output, "gloamberry_wine", CoreItems.GLOAMBERRY_WINE.get(), CoreFluids.GLOAMBERRY_WINE.source().get());
        wine(output, "freckleberry_wine", CoreItems.FRECKLEBERRY_WINE.get(), CoreFluids.FRECKLEBERRY_WINE.source().get());

        shapeless(RecipeCategory.COMBAT, CoreItems.PYROBERRY_COCKTAIL.get(), 1)
            .requires(CoreItems.PYROBERRY_WINE.get()).requires(Items.STRING).requires(Items.GUNPOWDER)
            .unlockedBy(getHasName(CoreItems.PYROBERRY_WINE.get()), has(CoreItems.PYROBERRY_WINE.get())).save(output, id("pyroberry_cocktail"));
        shapeless(RecipeCategory.BREWING, Items.MAGMA_CREAM, 1).requires(CoreItems.PYROBERRIES.get(), 3).requires(Items.SLIME_BALL)
            .unlockedBy(getHasName(CoreItems.PYROBERRIES.get()), has(CoreItems.PYROBERRIES.get())).save(output, id("magma_cream"));
    }

    /** Three bottles and a bucket of the wine make three bottles of it. */
    private static void wine(RecipeOutput output, String name, Item wine, Fluid fluid) {
        shapeless(RecipeCategory.FOOD, wine, 3).requires(Items.GLASS_BOTTLE, 3).requires(FluidContainerIngredient.of(fluid))
            .unlockedBy(getHasName(Items.GLASS_BOTTLE), has(Items.GLASS_BOTTLE)).save(output, id(name));
    }

    // -- The furnace recipes ------------------------------------------------

    private void smelting(RecipeOutput output) {
        smelt(output, CoreBlocks.COBBLESTONE_ANDESITE.get(), Items.ANDESITE, 0.1f);
        smelt(output, CoreBlocks.COBBLESTONE_GRANITE.get(), Items.GRANITE, 0.1f);
        smelt(output, CoreBlocks.COBBLESTONE_DIORITE.get(), Items.DIORITE, 0.1f);
        smelt(output, CoreBlocks.COBBLESTONE_LIMESTONE.get(), CoreBlocks.LIMESTONE.get(), 0.1f);
        smelt(output, item(Material.UNFIRED_BRICK), Items.BRICK, 0.4f);
        smelt(output, item(Material.UNFIRED_REFRACTORY_BRICK), item(Material.REFRACTORY_BRICK), 0.1f);
        smelt(output, Items.COBBLESTONE_SLAB, Items.STONE_SLAB, 0.1f);
        smelt(output, Items.GRAVEL, Items.COBBLESTONE, 0.1f);
        smelt(output, CoreBlocks.ROCK_WOOD_CHIPS.get(), item(Material.CHARCOAL_FLAKES), 0.1f);
        smelt(output, item(Material.DUST_LIMESTONE), item(Material.QUICKLIME), 0.1f);
        smelt(output, Items.APPLE, CoreItems.APPLE_BAKED.get(), 0.1f);
        smelt(output, Items.CARROT, CoreItems.CARROT_ROASTED.get(), 0.1f);
        smelt(output, Items.EGG, CoreItems.EGG_ROASTED.get(), 0.1f);
        smelt(output, Items.BROWN_MUSHROOM, CoreItems.MUSHROOM_BROWN_ROASTED.get(), 0.1f);
        smelt(output, Items.RED_MUSHROOM, CoreItems.MUSHROOM_RED_ROASTED.get(), 0.1f);
        smelt(output, Items.BEETROOT, CoreItems.BEETROOT_ROASTED.get(), 0.1f);
        smelt(output, item(Material.BREAD_DOUGH), Items.BREAD, 0.1f);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(item(Material.COOKIE_DOUGH)), RecipeCategory.FOOD, new ItemStack(Items.COOKIE, 8), 0.1f, SMELT_TICKS)
            .unlockedBy(hasName(Material.COOKIE_DOUGH), has(Material.COOKIE_DOUGH)).save(output, id(getSmeltingRecipeName(Items.COOKIE)));
    }

    private static void smelt(RecipeOutput output, ItemLike ingredient, ItemLike result, float experience) {
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ingredient), RecipeCategory.MISC, result, experience, SMELT_TICKS)
            .unlockedBy(getHasName(ingredient), has(ingredient)).save(output, id(getSmeltingRecipeName(result)));
    }

    // -- Helpers ------------------------------------------------------------

    private static ShapedRecipeBuilder shaped(RecipeCategory category, ItemLike result, int count) {
        return ShapedRecipeBuilder.shaped(category, result, count);
    }

    private static ShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike result, int count) {
        return ShapelessRecipeBuilder.shapeless(category, result, count);
    }

    private static Item item(Material material) {
        return CoreItems.material(material).get();
    }

    private static Criterion<?> has(Material material) {
        return has(item(material));
    }

    private static String hasName(Material material) {
        return getHasName(item(material));
    }

    private static Ingredient water() {
        return FluidContainerIngredient.of(Fluids.WATER);
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, name);
    }
}
