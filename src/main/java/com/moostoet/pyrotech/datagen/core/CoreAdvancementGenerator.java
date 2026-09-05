package com.moostoet.pyrotech.datagen.core;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.CoreItems;
import com.moostoet.pyrotech.core.advancement.PickupModItemTrigger;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.function.Consumer;

/** The root advancement, granted on the first Pyrotech item picked up. The rest follow their modules. */
public final class CoreAdvancementGenerator implements AdvancementProvider.AdvancementGenerator {

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver, ExistingFileHelper existingFileHelper) {
        Advancement.Builder.advancement()
            .display(CoreItems.BOOK.get(),
                Component.translatable("itemGroup.pyrotech"),
                Component.translatable("pyrotech.desc"),
                ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, "textures/block/refractory_brick.png"),
                AdvancementType.TASK, true, false, false)
            .addCriterion("mod_item", PickupModItemTrigger.TriggerInstance.pickedUp())
            .save(saver, Pyrotech.MOD_ID + ":root");
    }
}
