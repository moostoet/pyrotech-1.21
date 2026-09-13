package com.moostoet.pyrotech.datagen.tech.basic;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.tech.basic.TechBasicBlocks;
import com.moostoet.pyrotech.tech.basic.block.BarrelBlock;
import com.moostoet.pyrotech.tech.basic.block.CampfireBlock;
import com.moostoet.pyrotech.tech.basic.block.CampfireVariant;
import com.moostoet.pyrotech.tech.basic.block.DryingRackBlock;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * The three blockstates the migration could not carry (interaction and tile sync
 * prototype): the campfire without its {@code item} variant, the two drying racks as
 * two blocks with the normal rack's {@code stacked} property, and the barrel with its
 * {@code sealed} property. The other twelve keep their converted blockstates.
 */
public final class TechBasicBlockStateProvider extends BlockStateProvider {

    private static final String ASH_LETTERS = "abcdefgh";

    public TechBasicBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Pyrotech.MOD_ID, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Tech Basic Block States: " + Pyrotech.MOD_ID;
    }

    @Override
    protected void registerStatesAndModels() {
        this.campfire();
        this.dryingRacks();
        this.getVariantBuilder(TechBasicBlocks.BARREL.get())
            .partialState().with(BarrelBlock.SEALED, false).modelForState().modelFile(this.existing("block/barrel")).addModel()
            .partialState().with(BarrelBlock.SEALED, true).modelForState().modelFile(this.existing("block/barrel_sealed")).addModel();
    }

    /** The 1.12 multipart: tinder, fire, and an ash pile that grows a model a level. */
    private void campfire() {
        MultiPartBlockStateBuilder builder = this.getMultipartBuilder(TechBasicBlocks.CAMPFIRE.get());
        builder.part().modelFile(this.campfireModel("tinder_2")).uvLock(true).addModel().condition(CampfireBlock.VARIANT, CampfireVariant.NORMAL).end();
        builder.part().modelFile(this.campfireModel("tinder")).uvLock(true).addModel().condition(CampfireBlock.VARIANT, CampfireVariant.LIT).end();
        builder.part().modelFile(this.campfireModel("campfire_fire")).uvLock(true).addModel().condition(CampfireBlock.VARIANT, CampfireVariant.LIT).end();
        builder.part().modelFile(this.campfireModel("campfire_ash_a")).uvLock(true).addModel().condition(CampfireBlock.VARIANT, CampfireVariant.ASH).end();
        for (int ash = 1; ash <= 8; ash++) {
            String letter = String.valueOf(ASH_LETTERS.charAt(ash - 1));
            builder.part().modelFile(this.campfireModel("campfire_ash_" + letter + "_3")).uvLock(true).addModel()
                .condition(CampfireBlock.VARIANT, CampfireVariant.NORMAL).condition(CampfireBlock.ASH, ash).end();
            builder.part().modelFile(this.campfireModel("campfire_ash_" + letter + "_2")).uvLock(true).addModel()
                .condition(CampfireBlock.VARIANT, CampfireVariant.LIT).condition(CampfireBlock.ASH, ash).end();
            builder.part().modelFile(this.campfireModel("campfire_ash_" + letter)).uvLock(true).addModel()
                .condition(CampfireBlock.VARIANT, CampfireVariant.ASH).condition(CampfireBlock.ASH, ash).end();
        }
    }

    /** The converted rotations: the models face south unrotated, so the wall they hang on turns them. */
    private void dryingRacks() {
        Block crude = TechBasicBlocks.DRYING_RACK_CRUDE.get();
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            this.getVariantBuilder(crude).partialState().with(DryingRackBlock.FACING, facing)
                .modelForState().modelFile(this.existing("block/drying_rack_crude")).rotationY(rackRotation(facing)).addModel();
        }
        MultiPartBlockStateBuilder rack = this.getMultipartBuilder(TechBasicBlocks.DRYING_RACK.get());
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            rack.part().modelFile(this.existing("block/drying_rack")).rotationY(rackRotation(facing)).addModel()
                .condition(DryingRackBlock.FACING, facing).end();
            rack.part().modelFile(this.existing("block/drying_rack_stacked")).rotationY(rackRotation(facing)).addModel()
                .condition(DryingRackBlock.FACING, facing).condition(DryingRackBlock.STACKED, true).end();
        }
    }

    private static int rackRotation(Direction facing) {
        return switch (facing) {
            case WEST -> 90;
            case NORTH -> 180;
            case EAST -> 270;
            default -> 0;
        };
    }

    private ModelFile campfireModel(String name) {
        return this.existing("block/gen/campfire/" + name);
    }

    private ModelFile existing(String path) {
        return this.models().getExistingFile(this.modLoc(path));
    }
}
