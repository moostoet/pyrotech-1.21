package com.moostoet.pyrotech.datagen.core;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.PyrotechTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.tags.FluidTags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public final class CoreFluidTagsProvider extends FluidTagsProvider {

    public CoreFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                 ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Pyrotech.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(PyrotechTags.Fluids.DOUSING).addTag(FluidTags.WATER);
    }
}
