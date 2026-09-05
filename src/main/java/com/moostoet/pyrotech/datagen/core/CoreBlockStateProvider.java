package com.moostoet.pyrotech.datagen.core;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.CoreBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * The blockstates the 1.12 assets could not carry over: slabs lost their {@code _double}
 * block in 1.13, walls swapped booleans for {@code none/low/tall} in 1.16, and the door
 * models were redesigned. The rest of core's blockstates stay static under
 * {@code src/main/resources}; only these four shapes are rebuilt from the 1.21 templates,
 * over the migrated models wherever one still fits.
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
