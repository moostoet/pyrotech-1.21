package com.moostoet.pyrotech.datagen.core;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.core.CoreFluids;
import com.moostoet.pyrotech.core.block.BerryBushBlock;
import com.moostoet.pyrotech.core.block.DenseRedstoneOreBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * The blockstates the 1.12 assets could not carry over: slabs lost their {@code _double}
 * block in 1.13, walls swapped booleans for {@code none/low/tall} in 1.16, and the door
 * models were redesigned. Slice 3 adds the blocks whose 1.12 states no longer exist: the
 * variant cobblestone, the log pile's {@code axis=none}, the bushes' position-seeded
 * facing, the two-block redstone ores, and the straw bed's property-blind model. The rest
 * of core's blockstates stay static under {@code src/main/resources}; these are rebuilt
 * from the 1.21 templates over the migrated models wherever one still fits.
 */
public final class CoreBlockStateProvider extends BlockStateProvider {

    public CoreBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Pyrotech.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        this.slab(CoreBlocks.MASONRY_BRICK_SLAB.get(), "masonry_brick");
        this.slab(CoreBlocks.REFRACTORY_BRICK_SLAB.get(), "refractory_brick");

        this.stairs(CoreBlocks.MASONRY_BRICK_STAIRS.get(), "masonry_brick");
        this.stairs(CoreBlocks.REFRACTORY_BRICK_STAIRS.get(), "refractory_brick");

        this.wall(CoreBlocks.MASONRY_BRICK_WALL.get(), "masonry_brick");
        this.wall(CoreBlocks.REFRACTORY_BRICK_WALL.get(), "refractory_brick");

        // 1.12's BlockDoor rendered on the cutout layer; issue 27 puts the layer in the model.
        this.doorBlockWithRenderType((DoorBlock) CoreBlocks.REFRACTORY_DOOR.get(), "refractory",
            this.modLoc("block/door_refractory_lower"), this.modLoc("block/door_refractory_upper"), "cutout");
        this.doorBlockWithRenderType((DoorBlock) CoreBlocks.STONE_DOOR.get(), "stone",
            this.modLoc("block/door_stone_lower"), this.modLoc("block/door_stone_upper"), "cutout");

        // The 1.12 variant block's four generated models, one flat block each.
        this.simpleBlock(CoreBlocks.COBBLESTONE_ANDESITE.get(), this.existing("block/gen/cobblestone/cube_all"));
        this.simpleBlock(CoreBlocks.COBBLESTONE_DIORITE.get(), this.existing("block/gen/cobblestone/cube_all_2"));
        this.simpleBlock(CoreBlocks.COBBLESTONE_GRANITE.get(), this.existing("block/gen/cobblestone/cube_all_3"));
        this.simpleBlock(CoreBlocks.COBBLESTONE_LIMESTONE.get(), this.existing("block/gen/cobblestone/cube_all_4"));

        // 1.12's log had an axis=none bark variant; the pillar block has only three axes.
        this.axisBlock((RotatedPillarBlock) CoreBlocks.LOG_PILE.get(), this.existing("block/log_pile"), this.existing("block/log_pile"));

        // 1.12 chose the bush's facing from a position hash in getActualState. The four
        // rotations as a variant list do the same: the bakery picks one by position.
        this.bush(CoreBlocks.PYROBERRY_BUSH.get(), "pyroberry_bush");
        this.bush(CoreBlocks.GLOAMBERRY_BUSH.get(), "gloamberry_bush");

        // The 1.12 pair of blocks per size is one block with lit; each side keeps its
        // migrated models and their four random rotations.
        this.redstoneOre(CoreBlocks.DENSE_REDSTONE_ORE_LARGE.get(), "dense_redstone_ore_large", "dense_crystal_ore_large_", "abcdef");
        this.redstoneOre(CoreBlocks.DENSE_REDSTONE_ORE_SMALL.get(), "dense_redstone_ore_small", "dense_crystal_ore_small_", "abcde");
        this.redstoneOre(CoreBlocks.DENSE_REDSTONE_ORE_ROCKS.get(), "dense_redstone_ore_rocks", "rock_small_", "abcd");

        // One model for every facing, part, and occupancy, as the 1.12 blockstate had it.
        this.simpleBlock(CoreBlocks.STRAW_BED.get(), this.existing("block/straw_bed"));

        // Slice 4. A liquid block's model only names the particle texture, as vanilla water's does.
        for (CoreFluids.Entry fluid : CoreFluids.ALL) {
            this.simpleBlock(fluid.block().get(), this.models().getBuilder(fluid.name()).texture("particle", fluid.stillTexture()));
        }
    }

    private void bush(Block block, String name) {
        this.getVariantBuilder(block).forAllStates(state -> ConfiguredModel.allYRotations(
            this.existing("block/gen/" + name + "/bush_" + state.getValue(BerryBushBlock.AGE)), 0, false));
    }

    private void redstoneOre(Block block, String name, String modelPrefix, String suffixes) {
        this.getVariantBuilder(block).forAllStates(state -> {
            String folder = "block/gen/" + name + (state.getValue(DenseRedstoneOreBlock.LIT) ? "" : "_inactive") + "/" + modelPrefix;
            List<ConfiguredModel> models = new ArrayList<>();
            for (char suffix : suffixes.toCharArray()) {
                for (ConfiguredModel model : ConfiguredModel.allYRotations(this.existing(folder + suffix), 0, false)) {
                    models.add(model);
                }
            }
            return models.toArray(ConfiguredModel[]::new);
        });
    }

    /** The migrated {@code _slab} and {@code _slab_upper} models; the double is the full block. */
    private void slab(SlabBlock block, String base) {
        this.slabBlock(block,
            this.existing("block/" + base + "_slab"),
            this.existing("block/" + base + "_slab_upper"),
            this.existing("block/gen/" + base + "_block/cube_all"));
    }

    private void stairs(StairBlock block, String base) {
        this.stairsBlock(block,
            this.existing("block/" + base + "_stairs"),
            this.existing("block/" + base + "_inner_stairs"),
            this.existing("block/" + base + "_outer_stairs"));
    }

    /**
     * 1.16 added the tall wall side, so the migrated assets have a post and a side model but
     * no {@code _side_tall}; that one is generated from the same texture.
     */
    private void wall(WallBlock block, String base) {
        this.wallBlock(block,
            this.existing("block/" + base + "_wall_post"),
            this.existing("block/" + base + "_wall_side"),
            this.models().wallSideTall(base + "_wall_side_tall", this.modLoc("block/" + base)));
    }

    private ModelFile existing(String path) {
        return this.models().getExistingFile(this.modLoc(path));
    }
}
