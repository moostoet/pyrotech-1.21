package com.moostoet.pyrotech.datagen;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.datagen.bucket.BucketItemModelProvider;
import com.moostoet.pyrotech.datagen.bucket.BucketItemTagsProvider;
import com.moostoet.pyrotech.datagen.core.CoreAdvancementGenerator;
import com.moostoet.pyrotech.datagen.core.CoreBlockStateProvider;
import com.moostoet.pyrotech.datagen.core.CoreBlockTagsProvider;
import com.moostoet.pyrotech.datagen.core.CoreDataMapProvider;
import com.moostoet.pyrotech.datagen.core.CoreEntityTypeTagsProvider;
import com.moostoet.pyrotech.datagen.core.CoreFluidTagsProvider;
import com.moostoet.pyrotech.datagen.core.CoreItemModelProvider;
import com.moostoet.pyrotech.datagen.core.CoreItemTagsProvider;
import com.moostoet.pyrotech.datagen.core.CoreLootModifierProvider;
import com.moostoet.pyrotech.datagen.core.CoreLootTableProvider;
import com.moostoet.pyrotech.datagen.core.RecipeRemovalProvider;
import com.moostoet.pyrotech.datagen.storage.StorageItemModelProvider;
import com.moostoet.pyrotech.datagen.storage.StorageItemTagsProvider;
import com.moostoet.pyrotech.datagen.tool.ToolItemModelProvider;
import com.moostoet.pyrotech.datagen.tool.ToolItemTagsProvider;
import com.moostoet.pyrotech.datagen.worldgen.WorldgenBiomeTagsProvider;
import com.moostoet.pyrotech.datagen.worldgen.WorldgenBlockTagsProvider;
import com.moostoet.pyrotech.datagen.worldgen.WorldgenRegistryProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = Pyrotech.MOD_ID)
public final class PyrotechDatagen {

    private PyrotechDatagen() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        ExistingFileHelper existingFiles = event.getExistingFileHelper();

        generator.addProvider(event.includeClient(), new CoreBlockStateProvider(output, existingFiles));
        generator.addProvider(event.includeClient(), new CoreItemModelProvider(output, existingFiles));
        generator.addProvider(event.includeClient(), new ToolItemModelProvider(output, existingFiles));
        generator.addProvider(event.includeClient(), new BucketItemModelProvider(output, existingFiles));
        generator.addProvider(event.includeClient(), new StorageItemModelProvider(output, existingFiles));

        CoreBlockTagsProvider blockTags = new CoreBlockTagsProvider(output, lookup, existingFiles);
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(event.includeServer(),
            new CoreItemTagsProvider(output, lookup, blockTags.contentsGetter(), existingFiles));
        generator.addProvider(event.includeServer(),
            new ToolItemTagsProvider(output, lookup, blockTags.contentsGetter(), existingFiles));
        generator.addProvider(event.includeServer(),
            new BucketItemTagsProvider(output, lookup, blockTags.contentsGetter(), existingFiles));
        generator.addProvider(event.includeServer(),
            new StorageItemTagsProvider(output, lookup, blockTags.contentsGetter(), existingFiles));
        generator.addProvider(event.includeServer(), new WorldgenBlockTagsProvider(output, lookup, existingFiles));
        generator.addProvider(event.includeServer(), new WorldgenBiomeTagsProvider(output, lookup, existingFiles));
        generator.addProvider(event.includeServer(), new CoreFluidTagsProvider(output, lookup, existingFiles));
        generator.addProvider(event.includeServer(), new CoreEntityTypeTagsProvider(output, lookup, existingFiles));
        generator.addProvider(event.includeServer(), new CoreDataMapProvider(output, lookup));
        generator.addProvider(event.includeServer(), new RecipeRemovalProvider(output));
        generator.addProvider(event.includeServer(), new CoreLootModifierProvider(output, lookup));
        generator.addProvider(event.includeServer(), new WorldgenRegistryProvider(output, lookup));
        generator.addProvider(event.includeServer(), new PyrotechRecipeProvider(output, lookup));
        generator.addProvider(event.includeServer(), new CoreLootTableProvider(output, lookup));
        generator.addProvider(event.includeServer(),
            new AdvancementProvider(output, lookup, existingFiles, List.of(new CoreAdvancementGenerator())));
    }
}
