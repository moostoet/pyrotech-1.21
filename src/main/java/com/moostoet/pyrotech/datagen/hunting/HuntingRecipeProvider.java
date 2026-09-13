package com.moostoet.pyrotech.datagen.hunting;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.CoreItems;
import com.moostoet.pyrotech.core.Material;
import com.moostoet.pyrotech.core.PyrotechTags;
import com.moostoet.pyrotech.core.recipe.FluidContainerIngredient;
import com.moostoet.pyrotech.datagen.RecipeUnit;
import com.moostoet.pyrotech.datagen.ToolDamageShapelessRecipeBuilder;
import com.moostoet.pyrotech.hunting.HuntingBlocks;
import com.moostoet.pyrotech.hunting.HuntingItems;
import com.moostoet.pyrotech.hunting.HuntingTags;
import com.moostoet.pyrotech.hunting.item.KnifeItem;
import com.moostoet.pyrotech.hunting.recipe.LeatherDurableUpgradeRecipe;
import com.moostoet.pyrotech.hunting.recipe.LeatherFireProtectionRecipe;
import com.moostoet.pyrotech.hunting.recipe.LeatherRepairRecipe;
import com.moostoet.pyrotech.hunting.recipe.ShearingPeltRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Hunting's 59 recipes: the 51 JSONs of the 1.12 hunting folder, the four fireproof armour
 * JSONs from core's folder that consume hunting's chain, and the four leather kit and
 * armour recipes that were core's classes, with no condition (recipe sign-off). Ids are
 * the 1.12 file names; the ore dictionary names are the vanilla and Pyrotech tags.
 */
public final class HuntingRecipeProvider extends RecipeProvider implements RecipeUnit {

    private static final List<Material> KNIFE_HEADS = List.of(Material.BONE_SHARD, Material.FLINT_SHARD, Material.BRICK_STONE,
        Material.IRON_SHARD, Material.GOLD_SHARD, Material.DIAMOND_SHARD, Material.OBSIDIAN_SHARD);
    private static final List<String> KNIFE_MATERIALS = List.of("bone", "flint", "stone", "iron", "gold", "diamond", "obsidian");
    private static final Map<String, Item> LEATHER_ARMOR = Map.of(
        "helmet", Items.LEATHER_HELMET, "chestplate", Items.LEATHER_CHESTPLATE, "leggings", Items.LEATHER_LEGGINGS, "boots", Items.LEATHER_BOOTS);
    private static final Map<String, List<String>> FIREPROOF_PATTERNS = Map.of(
        "helmet", List.of("OOO", "OAO"),
        "chestplate", List.of("OAO", "OOO", "OOO"),
        "leggings", List.of("OOO", "OAO", "O O"),
        "boots", List.of("O O", "OAO"));
    private static final int SCRAPING_TOOL_DAMAGE = 2;
    private static final int REPAIR_TOOL_DAMAGE = 4;
    private static final float REPAIR_SHARE = 0.25f;

    public HuntingRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    public void buildRecipes(RecipeOutput output) {
        Ingredient huntersKnife = Ingredient.of(HuntingTags.Items.HUNTERS_KNIVES);
        Item fletching = item(Material.FLETCHING);

        arrow(output, "arrow_flint", HuntingItems.FLINT_ARROW.get(), item(Material.FLINT_SHARD), fletching);
        arrow(output, "arrow_bone", HuntingItems.BONE_ARROW.get(), item(Material.BONE_SHARD), fletching);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, HuntingBlocks.BUTCHERS_BLOCK.get()).pattern("WWW").pattern("B B").pattern("BPB")
            .define('W', ItemTags.PLANKS).define('B', item(Material.BOARD_TARRED)).define('P', item(Material.BOARD))
            .unlockedBy(getHasName(item(Material.BOARD_TARRED)), has(item(Material.BOARD_TARRED))).save(output, id("butchers_block"));

        for (int i = 0; i < KNIFE_HEADS.size(); i++) {
            Item head = item(KNIFE_HEADS.get(i));
            knife(output, "hunters_knife_" + KNIFE_MATERIALS.get(i), HuntingItems.HUNTERS_KNIVES.get(i), head, "  ^", " ^ ", "/S ");
            knife(output, "butchers_knife_" + KNIFE_MATERIALS.get(i), HuntingItems.BUTCHERS_KNIVES.get(i), head, " ^ ", "^/ ", "/S ");
        }

        ToolDamageShapelessRecipeBuilder.toolDamageShapeless(RecipeCategory.MISC, fletching, 3, huntersKnife, 0)
            .requires(Tags.Items.FEATHERS)
            .unlockedBy("has_feather", has(Tags.Items.FEATHERS)).save(output, id("fletching"));

        Item sheet = item(Material.LEATHER_SHEET);
        Item strap = item(Material.LEATHER_STRAP);
        Item cord = item(Material.LEATHER_CORD);
        Item durableSheet = item(Material.LEATHER_DURABLE_SHEET);
        Item durableStrap = item(Material.LEATHER_DURABLE_STRAP);
        Item durableCord = item(Material.LEATHER_DURABLE_CORD);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, HuntingItems.LEATHER_REPAIR_KIT.get())
            .requires(sheet, 2).requires(strap).requires(cord).requires(Items.STRING)
            .unlockedBy(getHasName(sheet), has(sheet)).save(output, id("leather_repair_kit"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, HuntingItems.LEATHER_DURABLE_REPAIR_KIT.get())
            .requires(durableSheet, 2).requires(durableStrap).requires(durableCord).requires(Items.STRING)
            .unlockedBy(getHasName(durableSheet), has(durableSheet)).save(output, id("leather_durable_repair_kit"));
        // The 1.12 recipe was a plain shapeless one, so the knife is consumed with the rest.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, HuntingItems.LEATHER_DURABLE_UPGRADE_KIT.get())
            .requires(durableSheet, 3).requires(durableStrap, 2).requires(durableCord, 2).requires(Items.STRING).requires(huntersKnife)
            .unlockedBy(getHasName(durableSheet), has(durableSheet)).save(output, id("leather_durable_upgrade_kit"));

        for (Map.Entry<String, Item> piece : LEATHER_ARMOR.entrySet()) {
            Item armor = piece.getValue();
            output.accept(id("leather_" + piece.getKey() + "_durable"), new LeatherDurableUpgradeRecipe("", CraftingBookCategory.EQUIPMENT,
                new ItemStack(armor), List.of(Ingredient.of(armor), Ingredient.of(HuntingItems.LEATHER_DURABLE_UPGRADE_KIT.get()))), null);
            output.accept(id("leather_repair_" + piece.getKey()), new LeatherRepairRecipe(Ingredient.of(armor), huntersKnife, REPAIR_TOOL_DAMAGE,
                Ingredient.of(HuntingTags.Items.LEATHER_REPAIR_KITS), REPAIR_SHARE), null);
            output.accept(id("leather_" + piece.getKey() + "_fireproof"), new LeatherFireProtectionRecipe("", CraftingBookCategory.EQUIPMENT,
                ShapedRecipePattern.of(Map.of('O', Ingredient.of(item(Material.SLAKED_LIME)), 'A', Ingredient.of(armor)),
                    FIREPROOF_PATTERNS.get(piece.getKey())),
                new ItemStack(armor)), null);
        }

        scraping(output, "scraping_pelt", HuntingTags.Items.SCRAPEABLE_HIDES, HuntingItems.HIDE_SCRAPED.get(), huntersKnife);
        scraping(output, "scraping_pelt_small", HuntingTags.Items.SMALL_SCRAPEABLE_HIDES, HuntingItems.HIDE_SMALL_SCRAPED.get(), huntersKnife);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, HuntingItems.HIDE_SCRAPED.get())
            .requires(HuntingTags.Items.SCRAPEABLE_HIDES).requires(HuntingTags.Items.SHARDS)
            .unlockedBy("has_shard", has(HuntingTags.Items.SHARDS)).save(output, id("scraping_pelt_shard"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, HuntingItems.HIDE_SMALL_SCRAPED.get())
            .requires(HuntingTags.Items.SMALL_SCRAPEABLE_HIDES).requires(HuntingTags.Items.SHARDS)
            .unlockedBy("has_shard", has(HuntingTags.Items.SHARDS)).save(output, id("scraping_pelt_small_shard"));

        Ingredient shears = Ingredient.of(Tags.Items.TOOLS_SHEAR);
        for (Map.Entry<DyeColor, DeferredItem<Item>> pelt : HuntingItems.SHEEP_PELTS.entrySet()) {
            String name = pelt.getValue().getId().getPath().substring("pelt_sheep_".length());
            output.accept(id("shearing_pelt_" + name), new ShearingPeltRecipe(Ingredient.of(pelt.getValue().get()), shears,
                new ItemStack(HuntingItems.HIDE_SHEEP_SHEARED.get()), new ItemStack(wool(pelt.getKey()))), null);
        }
        for (Map.Entry<Llama.Variant, DeferredItem<Item>> pelt : HuntingItems.LLAMA_PELTS.entrySet()) {
            output.accept(id("shearing_pelt_llama_" + pelt.getKey().getSerializedName()), new ShearingPeltRecipe(Ingredient.of(pelt.getValue().get()),
                shears, new ItemStack(HuntingItems.HIDE_LLAMA.get()), new ItemStack(wool(llamaWool(pelt.getKey())))), null);
        }

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, HuntingItems.HIDE_WASHED.get())
            .requires(HuntingItems.HIDE_SCRAPED.get()).requires(FluidContainerIngredient.of(Fluids.WATER))
            .unlockedBy(getHasName(HuntingItems.HIDE_SCRAPED.get()), has(HuntingItems.HIDE_SCRAPED.get())).save(output, id("washing_hide"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, HuntingItems.HIDE_SMALL_WASHED.get())
            .requires(HuntingItems.HIDE_SMALL_SCRAPED.get()).requires(FluidContainerIngredient.of(Fluids.WATER))
            .unlockedBy(getHasName(HuntingItems.HIDE_SMALL_SCRAPED.get()), has(HuntingItems.HIDE_SMALL_SCRAPED.get())).save(output, id("washing_hide_small"));
    }

    /** A stick up the middle, the shard at the top right, and the fletching at the bottom left. */
    private static void arrow(RecipeOutput output, String name, ItemLike arrow, Item shard, Item fletching) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, arrow).pattern("  ^").pattern(" / ").pattern("#  ")
            .define('^', shard).define('/', Tags.Items.RODS_WOODEN).define('#', fletching)
            .unlockedBy(getHasName(fletching), has(fletching)).save(output, id(name));
    }

    /** Two heads, a stick, and twine, in the hunter's diagonal or the butcher's hook. */
    private static void knife(RecipeOutput output, String name, DeferredItem<KnifeItem> knife, Item head, String top, String middle, String bottom) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, knife.get()).pattern(top).pattern(middle).pattern(bottom)
            .define('^', head).define('/', Items.STICK).define('S', PyrotechTags.Items.TWINE)
            .unlockedBy(getHasName(head), has(head)).save(output, id(name));
    }

    private static void scraping(RecipeOutput output, String name, net.minecraft.tags.TagKey<Item> hides, ItemLike result, Ingredient knife) {
        ToolDamageShapelessRecipeBuilder.toolDamageShapeless(RecipeCategory.MISC, result, 1, knife, SCRAPING_TOOL_DAMAGE)
            .requires(hides)
            .unlockedBy("has_hide", has(hides)).save(output, id(name));
    }

    private static Item wool(DyeColor color) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(color.getName() + "_wool"));
    }

    /** The 1.12 llama wool colours: creamy and white give white wool, gray light gray, brown brown. */
    private static DyeColor llamaWool(Llama.Variant variant) {
        return switch (variant) {
            case BROWN -> DyeColor.BROWN;
            case GRAY -> DyeColor.LIGHT_GRAY;
            default -> DyeColor.WHITE;
        };
    }

    private static Item item(Material material) {
        return CoreItems.material(material).get();
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, name);
    }
}
