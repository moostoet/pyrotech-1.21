package com.moostoet.pyrotech.datagen.hunting;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.hunting.HuntingFluids;
import com.moostoet.pyrotech.hunting.HuntingTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.FluidTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/** The {@code #pyrotech:tannin} fluid tag (hunting sign-off, item 4). */
public final class HuntingFluidTagsProvider extends FluidTagsProvider {

    public HuntingFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                    ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Pyrotech.MOD_ID, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Hunting " + super.getName();
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(HuntingTags.Fluids.TANNIN).add(HuntingFluids.TANNIN.source().get(), HuntingFluids.TANNIN.flowing().get());
    }
}
