package com.moostoet.pyrotech.datagen.tool;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.CoreItems;
import com.moostoet.pyrotech.core.Material;
import com.moostoet.pyrotech.core.PyrotechTags;
import com.moostoet.pyrotech.datagen.RecipeUnit;
import com.moostoet.pyrotech.tool.ToolItems;
import com.moostoet.pyrotech.tool.recipe.ToolRepairRecipe;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Tool's recipes: the 57 1.12 crafting JSONs, all of which make a tool item, and the clay
 * shears furnace recipe {@code VanillaFurnaceRecipesAdd} registered in code. Ids are the
 * 1.12 file names; the ore dictionary names become the tags core's provider names.
 */
public final class ToolRecipeProvider extends RecipeProvider implements RecipeUnit {

    private static final TagKey<Item> STICK = Tags.Items.RODS_WOODEN;
    private static final int SMELT_TICKS = 200;
    /** The 1.12 repair kit config, baked into every repair recipe (tool sign-off, item 2). */
    private static final float REPAIR_FRACTION = 0.25f;
    private static final int HAMMER_REPAIR_DAMAGE = 4;

    public ToolRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    public void buildRecipes(RecipeOutput output) {
        this.crude(output);
        this.shields(output);
        family(output, "bone", item(Material.BONE_SHARD), ToolItems.BONE_AXE.get(), ToolItems.BONE_HOE.get(),
            ToolItems.BONE_PICKAXE.get(), ToolItems.BONE_SHOVEL.get(), ToolItems.BONE_SWORD.get());
        durableFamily(output, "bone", Items.BONE, ToolItems.BONE_AXE_DURABLE.get(), ToolItems.BONE_HOE_DURABLE.get(),
            ToolItems.BONE_PICKAXE_DURABLE.get(), ToolItems.BONE_SHOVEL_DURABLE.get());
        family(output, "flint", item(Material.FLINT_SHARD), ToolItems.FLINT_AXE.get(), ToolItems.FLINT_HOE.get(),
            ToolItems.FLINT_PICKAXE.get(), ToolItems.FLINT_SHOVEL.get(), ToolItems.FLINT_SWORD.get());
        durableFamily(output, "flint", Items.FLINT, ToolItems.FLINT_AXE_DURABLE.get(), ToolItems.FLINT_HOE_DURABLE.get(),
            ToolItems.FLINT_PICKAXE_DURABLE.get(), ToolItems.FLINT_SHOVEL_DURABLE.get());
        family(output, "obsidian", item(Material.OBSIDIAN_SHARD), ToolItems.OBSIDIAN_AXE.get(), ToolItems.OBSIDIAN_HOE.get(),
            ToolItems.OBSIDIAN_PICKAXE.get(), ToolItems.OBSIDIAN_SHOVEL.get(), ToolItems.OBSIDIAN_SWORD.get());
        family(output, "redstone", item(Material.DENSE_REDSTONE), ToolItems.REDSTONE_AXE.get(), ToolItems.REDSTONE_HOE.get(),
            ToolItems.REDSTONE_PICKAXE.get(), ToolItems.REDSTONE_SHOVEL.get(), ToolItems.REDSTONE_SWORD.get());
        family(output, "quartz", item(Material.DENSE_QUARTZ), ToolItems.QUARTZ_AXE.get(), ToolItems.QUARTZ_HOE.get(),
            ToolItems.QUARTZ_PICKAXE.get(), ToolItems.QUARTZ_SHOVEL.get(), ToolItems.QUARTZ_SWORD.get());
        this.shears(output);
        this.repairKits(output);
        this.smelting(output);
    }

    // -- The crude tools: rocks, plant fibers, and a stick -----------------------

    private void crude(RecipeOutput output) {
        Item fibers = item(Material.PLANT_FIBERS);
        shaped(RecipeCategory.TOOLS, ToolItems.CRUDE_AXE.get()).pattern("RT").pattern("RS")
            .define('R', PyrotechTags.Items.ROCKS).define('T', fibers).define('S', STICK)
            .unlockedBy("has_rock", has(PyrotechTags.Items.ROCKS)).save(output, id("crude_axe"));
        shaped(RecipeCategory.TOOLS, ToolItems.CRUDE_HOE.get()).pattern("TR").pattern("S ")
            .define('R', PyrotechTags.Items.ROCKS).define('T', fibers).define('S', STICK)
            .unlockedBy("has_rock", has(PyrotechTags.Items.ROCKS)).save(output, id("crude_hoe"));
        shaped(RecipeCategory.TOOLS, ToolItems.CRUDE_PICKAXE.get()).pattern("RT").pattern("SR")
            .define('R', PyrotechTags.Items.ROCKS).define('T', fibers).define('S', STICK)
            .unlockedBy("has_rock", has(PyrotechTags.Items.ROCKS)).save(output, id("crude_pickaxe"));
        shaped(RecipeCategory.TOOLS, ToolItems.CRUDE_SHOVEL.get()).pattern("RT").pattern("S ")
            .define('R', PyrotechTags.Items.ROCKS).define('T', fibers).define('S', STICK)
            .unlockedBy("has_rock", has(PyrotechTags.Items.ROCKS)).save(output, id("crude_shovel"));
        shaped(RecipeCategory.TOOLS, ToolItems.CRUDE_FISHING_ROD.get()).pattern("F|").pattern("SR")
            .define('F', fibers).define('|', PyrotechTags.Items.TWINE).define('S', STICK).define('R', PyrotechTags.Items.ROCKS)
            .unlockedBy("has_twine", has(PyrotechTags.Items.TWINE)).save(output, id("crude_fishing_rod"));
    }

    private void shields(RecipeOutput output) {
        shaped(RecipeCategory.COMBAT, ToolItems.CRUDE_SHIELD.get()).pattern(" T ").pattern("TST").pattern(" T ")
            .define('S', ItemTags.WOODEN_SLABS).define('T', PyrotechTags.Items.TWINE)
            .unlockedBy("has_twine", has(PyrotechTags.Items.TWINE)).save(output, id("crude_shield"));
        shaped(RecipeCategory.COMBAT, ToolItems.DURABLE_SHIELD.get()).pattern(" S ").pattern("TST").pattern(" S ")
            .define('S', item(Material.BOARD_TARRED)).define('T', item(Material.LEATHER_STRAP))
            .unlockedBy(hasName(Material.BOARD_TARRED), has(Material.BOARD_TARRED)).save(output, id("durable_shield"));
    }

    // -- The tool families: a head material on sticks ---------------------------------

    private static void family(RecipeOutput output, String prefix, Item head, Item axe, Item hoe, Item pickaxe, Item shovel, Item sword) {
        shaped(RecipeCategory.TOOLS, axe).pattern("RR").pattern("RS").pattern(" S").define('R', head).define('S', STICK)
            .unlockedBy(getHasName(head), has(head)).save(output, id(prefix + "_axe"));
        shaped(RecipeCategory.TOOLS, hoe).pattern("RR").pattern(" S").pattern(" S").define('R', head).define('S', STICK)
            .unlockedBy(getHasName(head), has(head)).save(output, id(prefix + "_hoe"));
        shaped(RecipeCategory.TOOLS, pickaxe).pattern("RRR").pattern(" S ").pattern(" S ").define('R', head).define('S', STICK)
            .unlockedBy(getHasName(head), has(head)).save(output, id(prefix + "_pickaxe"));
        shaped(RecipeCategory.TOOLS, shovel).pattern("R").pattern("S").pattern("S").define('R', head).define('S', STICK)
            .unlockedBy(getHasName(head), has(head)).save(output, id(prefix + "_shovel"));
        shaped(RecipeCategory.COMBAT, sword).pattern("R").pattern("R").pattern("S").define('R', head).define('S', STICK)
            .unlockedBy(getHasName(head), has(head)).save(output, id(prefix + "_sword"));
    }

    /** The durable tools: whole bones or flints on a stone tool shaft, bound with durable leather cord. */
    private static void durableFamily(RecipeOutput output, String prefix, Item head, Item axe, Item hoe, Item pickaxe, Item shovel) {
        Item cord = item(Material.LEATHER_DURABLE_CORD);
        Item shaft = item(Material.STONE_TOOL_SHAFT);
        shaped(RecipeCategory.TOOLS, axe).pattern("RR ").pattern("RC ").pattern(" S ")
            .define('R', head).define('C', cord).define('S', shaft)
            .unlockedBy(getHasName(shaft), has(shaft)).save(output, id(prefix + "_axe_durable"));
        shaped(RecipeCategory.TOOLS, hoe).pattern("RR ").pattern(" C ").pattern(" S ")
            .define('R', head).define('C', cord).define('S', shaft)
            .unlockedBy(getHasName(shaft), has(shaft)).save(output, id(prefix + "_hoe_durable"));
        shaped(RecipeCategory.TOOLS, pickaxe).pattern("RRR").pattern(" C ").pattern(" S ")
            .define('R', head).define('C', cord).define('S', shaft)
            .unlockedBy(getHasName(shaft), has(shaft)).save(output, id(prefix + "_pickaxe_durable"));
        shaped(RecipeCategory.TOOLS, shovel).pattern(" R ").pattern(" C ").pattern(" S ")
            .define('R', head).define('C', cord).define('S', shaft)
            .unlockedBy(getHasName(shaft), has(shaft)).save(output, id(prefix + "_shovel_durable"));
    }

    // -- Shears and repair kits ---------------------------------------------------

    private void shears(RecipeOutput output) {
        shaped(RecipeCategory.TOOLS, ToolItems.UNFIRED_CLAY_SHEARS.get()).pattern(" M").pattern("M ")
            .define('M', Items.CLAY_BALL)
            .unlockedBy(getHasName(Items.CLAY_BALL), has(Items.CLAY_BALL)).save(output, id("unfired_clay_shears"));
        shardShears(output, "bone_shears", ToolItems.BONE_SHEARS.get(), Material.BONE_SHARD);
        shardShears(output, "flint_shears", ToolItems.FLINT_SHEARS.get(), Material.FLINT_SHARD);
        strapShears(output, "stone_shears", ToolItems.STONE_SHEARS.get(), Material.BRICK_STONE);
        strapShears(output, "gold_shears", ToolItems.GOLD_SHEARS.get(), Material.GOLD_SHARD);
        strapShears(output, "diamond_shears", ToolItems.DIAMOND_SHEARS.get(), Material.DIAMOND_SHARD);
        strapShears(output, "obsidian_shears", ToolItems.OBSIDIAN_SHEARS.get(), Material.OBSIDIAN_SHARD);
    }

    /** Two shards on a stone stick, tied with twine. */
    private static void shardShears(RecipeOutput output, String name, Item shears, Material shard) {
        shaped(RecipeCategory.TOOLS, shears).pattern("TS").pattern("MM")
            .define('M', item(shard)).define('S', PyrotechTags.Items.STONE_STICKS).define('T', PyrotechTags.Items.TWINE)
            .unlockedBy(hasName(shard), has(shard)).save(output, id(name));
    }

    /** Two blades held by a leather strap. */
    private static void strapShears(RecipeOutput output, String name, Item shears, Material blade) {
        shaped(RecipeCategory.TOOLS, shears).pattern("TM").pattern("M ")
            .define('M', item(blade)).define('T', item(Material.LEATHER_STRAP))
            .unlockedBy(hasName(blade), has(blade)).save(output, id(name));
    }

    private void repairKits(RecipeOutput output) {
        repairKit(output, "bone_tool_repair_kit", ToolItems.BONE_TOOL_REPAIR_KIT.get(), Material.BONE_SHARD);
        repairKit(output, "flint_tool_repair_kit", ToolItems.FLINT_TOOL_REPAIR_KIT.get(), Material.FLINT_SHARD);
        repair(output, "bone_axe_durable_repair", ToolItems.BONE_TOOL_REPAIR_KIT.get(), ToolItems.BONE_AXE_DURABLE.get());
        repair(output, "bone_hoe_durable_repair", ToolItems.BONE_TOOL_REPAIR_KIT.get(), ToolItems.BONE_HOE_DURABLE.get());
        repair(output, "bone_pickaxe_durable_repair", ToolItems.BONE_TOOL_REPAIR_KIT.get(), ToolItems.BONE_PICKAXE_DURABLE.get());
        repair(output, "bone_shovel_durable_repair", ToolItems.BONE_TOOL_REPAIR_KIT.get(), ToolItems.BONE_SHOVEL_DURABLE.get());
        repair(output, "flint_axe_durable_repair", ToolItems.FLINT_TOOL_REPAIR_KIT.get(), ToolItems.FLINT_AXE_DURABLE.get());
        repair(output, "flint_hoe_durable_repair", ToolItems.FLINT_TOOL_REPAIR_KIT.get(), ToolItems.FLINT_HOE_DURABLE.get());
        repair(output, "flint_pickaxe_durable_repair", ToolItems.FLINT_TOOL_REPAIR_KIT.get(), ToolItems.FLINT_PICKAXE_DURABLE.get());
        repair(output, "flint_shovel_durable_repair", ToolItems.FLINT_TOOL_REPAIR_KIT.get(), ToolItems.FLINT_SHOVEL_DURABLE.get());
    }

    /** Three shards, a stone stick, and a leather cord. */
    private static void repairKit(RecipeOutput output, String name, Item kit, Material shard) {
        shapeless(RecipeCategory.TOOLS, kit).requires(item(shard), 3).requires(PyrotechTags.Items.STONE_STICKS)
            .requires(item(Material.LEATHER_CORD))
            .unlockedBy(hasName(shard), has(shard)).save(output, id(name));
    }

    private static void repair(RecipeOutput output, String name, Item kit, Item tool) {
        ResourceLocation id = id(name);
        Advancement.Builder advancement = output.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(id))
            .addCriterion(getHasName(kit), has(kit))
            .rewards(AdvancementRewards.Builder.recipe(id))
            .requirements(AdvancementRequirements.Strategy.OR);
        ToolRepairRecipe recipe = new ToolRepairRecipe(Ingredient.of(kit), Ingredient.of(tool), REPAIR_FRACTION, HAMMER_REPAIR_DAMAGE);
        output.accept(id, recipe, advancement.build(id.withPrefix("recipes/" + RecipeCategory.TOOLS.getFolderName() + "/")));
    }

    private void smelting(RecipeOutput output) {
        Item unfired = ToolItems.UNFIRED_CLAY_SHEARS.get();
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(unfired), RecipeCategory.TOOLS, ToolItems.CLAY_SHEARS.get(), 0.1f, SMELT_TICKS)
            .unlockedBy(getHasName(unfired), has(unfired)).save(output, id("clay_shears_from_smelting"));
    }

    // -- Helpers ------------------------------------------------------------------

    private static ShapedRecipeBuilder shaped(RecipeCategory category, ItemLike result) {
        return ShapedRecipeBuilder.shaped(category, result);
    }

    private static ShapelessRecipeBuilder shapeless(RecipeCategory category, ItemLike result) {
        return ShapelessRecipeBuilder.shapeless(category, result);
    }

    private static Item item(Material material) {
        return CoreItems.material(material).get();
    }

    private static net.minecraft.advancements.Criterion<?> has(Material material) {
        return has(item(material));
    }

    private static String hasName(Material material) {
        return getHasName(item(material));
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, name);
    }
}
