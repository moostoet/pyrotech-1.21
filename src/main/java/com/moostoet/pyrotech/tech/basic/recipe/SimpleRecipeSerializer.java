package com.moostoet.pyrotech.tech.basic.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** A serializer that is nothing but its two codecs, which is every tech/basic serializer. */
public record SimpleRecipeSerializer<R extends Recipe<?>>(MapCodec<R> codec, StreamCodec<RegistryFriendlyByteBuf, R> streamCodec)
    implements RecipeSerializer<R> {
}
