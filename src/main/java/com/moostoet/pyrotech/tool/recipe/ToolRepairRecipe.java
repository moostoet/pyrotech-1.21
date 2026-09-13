package com.moostoet.pyrotech.tool.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.moostoet.pyrotech.core.PyrotechTags;
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
 * A repair kit crafted with a hammer and a damaged tool: the 1.12 bone and flint repair
 * recipes as one serializer, {@code pyrotech:tool_repair} (tool sign-off, item 5). The
 * result is the tool with {@code repair_fraction} of its durability back; the hammer takes
 * {@code hammer_damage}, the kit one use. A tool at full durability does not match.
 */
public final class ToolRepairRecipe implements CraftingRecipe {

    private static final Ingredient HAMMER = Ingredient.of(PyrotechTags.Items.HAMMERS);
    private static final int KIT_DAMAGE = 1;

    private final Ingredient kit;
    private final Ingredient tool;
    private final float repairFraction;
    private final int hammerDamage;

    public ToolRepairRecipe(Ingredient kit, Ingredient tool, float repairFraction, int hammerDamage) {
        this.kit = kit;
        this.tool = tool;
        this.repairFraction = repairFraction;
        this.hammerDamage = hammerDamage;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean hammer = false;
        boolean kit = false;
        boolean tool = false;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!hammer && HAMMER.test(stack)) {
                hammer = true;
            } else if (!kit && this.kit.test(stack)) {
                kit = true;
            } else if (!tool && this.tool.test(stack) && stack.getDamageValue() > 0) {
                tool = true;
            } else {
                return false;
            }
        }
        return hammer && kit && tool;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (this.tool.test(stack)) {
                ItemStack repaired = stack.copyWithCount(1);
                repaired.setDamageValue(Math.max(0, stack.getDamageValue() - (int) (stack.getMaxDamage() * this.repairFraction)));
                return repaired;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < remaining.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (HAMMER.test(stack)) {
                remaining.set(i, CraftingRemainders.damaged(stack, this.hammerDamage));
            } else if (this.kit.test(stack)) {
                remaining.set(i, CraftingRemainders.damaged(stack, KIT_DAMAGE));
            } else if (!this.tool.test(stack) && stack.hasCraftingRemainingItem()) {
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
        ItemStack[] tools = this.tool.getItems();
        return tools.length == 0 ? ItemStack.EMPTY : tools[0].copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, HAMMER, this.kit, this.tool);
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.EQUIPMENT;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ToolRecipeSerializers.TOOL_REPAIR.get();
    }

    public static final class Serializer implements RecipeSerializer<ToolRepairRecipe> {

        private static final MapCodec<ToolRepairRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("kit").forGetter(recipe -> recipe.kit),
            Ingredient.CODEC_NONEMPTY.fieldOf("tool").forGetter(recipe -> recipe.tool),
            Codec.FLOAT.fieldOf("repair_fraction").forGetter(recipe -> recipe.repairFraction),
            Codec.INT.fieldOf("hammer_damage").forGetter(recipe -> recipe.hammerDamage)
        ).apply(instance, ToolRepairRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, ToolRepairRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.kit,
            Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.tool,
            ByteBufCodecs.FLOAT, recipe -> recipe.repairFraction,
            ByteBufCodecs.VAR_INT, recipe -> recipe.hammerDamage,
            ToolRepairRecipe::new);

        @Override
        public MapCodec<ToolRepairRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ToolRepairRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
