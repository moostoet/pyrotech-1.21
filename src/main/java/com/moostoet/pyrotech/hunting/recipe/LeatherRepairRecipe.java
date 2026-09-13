package com.moostoet.pyrotech.hunting.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.moostoet.pyrotech.core.item.CraftingRemainders;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * The 1.12 {@code LeatherRepairRecipe}: a damaged piece of leather armour, a hunter's knife,
 * and a leather repair kit give the armour back with a share of its durability restored.
 * The knife takes the recipe's tool damage and the kit one use.
 */
public final class LeatherRepairRecipe implements CraftingRecipe {

    private final Ingredient armor;
    private final Ingredient tool;
    private final int toolDamage;
    private final Ingredient kit;
    private final float repair;

    public LeatherRepairRecipe(Ingredient armor, Ingredient tool, int toolDamage, Ingredient kit, float repair) {
        this.armor = armor;
        this.tool = tool;
        this.toolDamage = toolDamage;
        this.kit = kit;
        this.repair = repair;
    }

    private ItemStack findArmor(CraftingInput input) {
        for (ItemStack stack : input.items()) {
            if (this.armor.test(stack)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != 3) {
            return false;
        }
        boolean armorFound = false;
        boolean toolFound = false;
        boolean kitFound = false;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) {
                continue;
            }
            if (!armorFound && this.armor.test(stack) && stack.getDamageValue() > 0) {
                armorFound = true;
            } else if (!toolFound && this.tool.test(stack)) {
                toolFound = true;
            } else if (!kitFound && this.kit.test(stack)) {
                kitFound = true;
            } else {
                return false;
            }
        }
        return armorFound && toolFound && kitFound;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack armor = this.findArmor(input);
        if (armor.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack repaired = armor.copyWithCount(1);
        repaired.setDamageValue(Math.max(0, repaired.getDamageValue() - (int) (repaired.getMaxDamage() * this.repair)));
        return repaired;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < remaining.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (this.tool.test(stack)) {
                remaining.set(i, CraftingRemainders.damaged(stack, this.toolDamage));
            } else if (this.kit.test(stack)) {
                remaining.set(i, CraftingRemainders.damaged(stack, 1));
            } else if (stack.hasCraftingRemainingItem()) {
                remaining.set(i, stack.getCraftingRemainingItem());
            }
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        ItemStack[] armors = this.armor.getItems();
        return armors.length == 0 ? ItemStack.EMPTY : armors[0].copyWithCount(1);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, this.armor, this.tool, this.kit);
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.EQUIPMENT;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HuntingRecipeSerializers.LEATHER_REPAIR.get();
    }

    public static final class Serializer implements RecipeSerializer<LeatherRepairRecipe> {

        private static final MapCodec<LeatherRepairRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("armor").forGetter(recipe -> recipe.armor),
            Ingredient.CODEC_NONEMPTY.fieldOf("tool").forGetter(recipe -> recipe.tool),
            Codec.INT.optionalFieldOf("tool_damage", 1).forGetter(recipe -> recipe.toolDamage),
            Ingredient.CODEC_NONEMPTY.fieldOf("kit").forGetter(recipe -> recipe.kit),
            Codec.floatRange(0, 1).fieldOf("repair").forGetter(recipe -> recipe.repair)
        ).apply(instance, LeatherRepairRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, LeatherRepairRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.armor,
            Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.tool,
            ByteBufCodecs.VAR_INT, recipe -> recipe.toolDamage,
            Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.kit,
            ByteBufCodecs.FLOAT, recipe -> recipe.repair,
            LeatherRepairRecipe::new);

        @Override
        public MapCodec<LeatherRepairRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, LeatherRepairRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
