package com.moostoet.pyrotech.hunting.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The 1.12 {@code LeatherArmorDurableUpgradeRecipe}: a shapeless recipe whose leather armour
 * comes out with Unbreaking I and the 1.12 brown, refusing a piece that already has it.
 */
public final class LeatherDurableUpgradeRecipe extends ShapelessRecipe {

    private static final int COLOR = 0x491D06;

    private final ItemStack result;
    private final List<Ingredient> inputs;

    public LeatherDurableUpgradeRecipe(String group, CraftingBookCategory category, ItemStack result, List<Ingredient> inputs) {
        super(group, category, result, NonNullList.copyOf(inputs));
        this.result = result;
        this.inputs = inputs;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return super.matches(input, level) && !this.assemble(input, level.registryAccess()).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return LeatherEnchanting.enchant(LeatherEnchanting.findArmor(input), Enchantments.UNBREAKING, COLOR, registries);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        ItemStack shown = LeatherEnchanting.enchant(this.result, Enchantments.UNBREAKING, COLOR, registries);
        return shown.isEmpty() ? this.result : shown;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HuntingRecipeSerializers.LEATHER_DURABLE_UPGRADE.get();
    }

    public static final class Serializer implements RecipeSerializer<LeatherDurableUpgradeRecipe> {

        private static final MapCodec<LeatherDurableUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(ShapelessRecipe::getGroup),
            CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapelessRecipe::category),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
            Ingredient.LIST_CODEC_NONEMPTY.fieldOf("ingredients").forGetter(recipe -> recipe.inputs)
        ).apply(instance, LeatherDurableUpgradeRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, LeatherDurableUpgradeRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ShapelessRecipe::getGroup,
            CraftingBookCategory.STREAM_CODEC, ShapelessRecipe::category,
            ItemStack.STREAM_CODEC, recipe -> recipe.result,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> recipe.inputs,
            LeatherDurableUpgradeRecipe::new);

        @Override
        public MapCodec<LeatherDurableUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, LeatherDurableUpgradeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
