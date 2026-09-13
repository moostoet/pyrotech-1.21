package com.moostoet.pyrotech.hunting.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

/**
 * The 1.12 {@code LeatherArmorFireProtectionRecipe}: a shaped recipe whose leather armour
 * comes out with Fire Protection I and the 1.12 ash grey, refusing a piece that already has it.
 */
public final class LeatherFireProtectionRecipe extends ShapedRecipe {

    private static final int COLOR = 0x99A088;

    private final ItemStack result;

    public LeatherFireProtectionRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result) {
        super(group, category, pattern, result);
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return super.matches(input, level) && !this.assemble(input, level.registryAccess()).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return LeatherEnchanting.enchant(LeatherEnchanting.findArmor(input), Enchantments.FIRE_PROTECTION, COLOR, registries);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        ItemStack shown = LeatherEnchanting.enchant(this.result, Enchantments.FIRE_PROTECTION, COLOR, registries);
        return shown.isEmpty() ? this.result : shown;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return HuntingRecipeSerializers.LEATHER_FIRE_PROTECTION.get();
    }

    public static final class Serializer implements RecipeSerializer<LeatherFireProtectionRecipe> {

        private static final MapCodec<LeatherFireProtectionRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
            CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
            ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.pattern),
            ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
        ).apply(instance, LeatherFireProtectionRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, LeatherFireProtectionRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ShapedRecipe::getGroup,
            CraftingBookCategory.STREAM_CODEC, ShapedRecipe::category,
            ShapedRecipePattern.STREAM_CODEC, recipe -> recipe.pattern,
            ItemStack.STREAM_CODEC, recipe -> recipe.result,
            LeatherFireProtectionRecipe::new);

        @Override
        public MapCodec<LeatherFireProtectionRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, LeatherFireProtectionRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
