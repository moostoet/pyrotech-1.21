package com.moostoet.pyrotech.datagen.hunting;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.hunting.HuntingFluids;
import com.moostoet.pyrotech.library.fluid.PyrotechFluids;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * The one hunting blockstate the migration could not carry: tannin's liquid block, whose
 * model only names the particle texture, as vanilla water's does. The carcass and the
 * butcher's block keep their converted blockstates.
 */
public final class HuntingBlockStateProvider extends BlockStateProvider {

    public HuntingBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Pyrotech.MOD_ID, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Hunting Block States: " + Pyrotech.MOD_ID;
    }

    @Override
    protected void registerStatesAndModels() {
        PyrotechFluids.Entry tannin = HuntingFluids.TANNIN;
        this.simpleBlock(tannin.block().get(), this.models().getBuilder(tannin.name()).texture("particle", tannin.stillTexture()));
    }
}
