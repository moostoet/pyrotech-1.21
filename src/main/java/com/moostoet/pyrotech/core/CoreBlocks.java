package com.moostoet.pyrotech.core;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.block.ConnectedGlassBlock;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Core's blocks. Ids are the 1.12 registry names. Hardness and resistance carry the
 * 1.12 values converted to 1.21's scale: a 1.12 {@code setResistance(r)} is {@code r * 0.6}
 * here, and a block that only set hardness has resistance equal to its hardness.
 */
public final class CoreBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Pyrotech.MOD_ID);

    private static final List<DeferredItem<BlockItem>> BLOCK_ITEMS = new ArrayList<>();

    /**
     * The set type behind the refractory and stone doors. 1.12 built both on
     * {@code Material.ROCK}, and vanilla's {@code BlockDoor} read that material for two
     * things: a door only refuses to open by hand when the material is {@code IRON}, and
     * the open and close world events are the iron pair (1005 and 1011) for {@code IRON}
     * and the wooden pair (1006 and 1012) for everything else. {@code ROCK} is neither, so
     * both doors open by hand and play the wooden door sounds over a stone step and break
     * sound. Vanilla 1.21's {@link BlockSetType#STONE} is the same but for the door sounds,
     * which are the iron ones there, so core registers its own.
     */
    public static final BlockSetType DOOR_SET_TYPE = BlockSetType.register(new BlockSetType(
        Pyrotech.MOD_ID + ":stone",
        true,
        true,
        false,
        BlockSetType.PressurePlateSensitivity.MOBS,
        SoundType.STONE,
        SoundEvents.WOODEN_DOOR_CLOSE,
        SoundEvents.WOODEN_DOOR_OPEN,
        SoundEvents.WOODEN_TRAPDOOR_CLOSE,
        SoundEvents.WOODEN_TRAPDOOR_OPEN,
        SoundEvents.STONE_PRESSURE_PLATE_CLICK_OFF,
        SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON,
        SoundEvents.STONE_BUTTON_CLICK_OFF,
        SoundEvents.STONE_BUTTON_CLICK_ON));

    public static final DeferredBlock<Block> CHARCOAL_BLOCK = simple("charcoal_block", rock(MapColor.STONE, 5, 5));
    public static final DeferredBlock<Block> COAL_COKE_BLOCK = simple("coal_coke_block", rock(MapColor.STONE, 5, 5));
    public static final DeferredBlock<Block> CRAFTING_TABLE_TEMPLATE = simple("crafting_table_template",
        BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL).strength(5, 6).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> REFRACTORY_BRICK_BLOCK = simple("refractory_brick_block", rock(MapColor.SAND, 3, 6));
    public static final DeferredBlock<Block> MASONRY_BRICK_BLOCK = simple("masonry_brick_block", rock(MapColor.STONE, 1.5f, 6));
    public static final DeferredBlock<Block> LIMESTONE = simple("limestone", rock(MapColor.STONE, 1.5f, 6));
    public static final DeferredBlock<Block> FOSSIL_ORE = simple("fossil_ore", rock(MapColor.STONE, 3, 3));
    public static final DeferredBlock<Block> DENSE_COAL_ORE = simple("dense_coal_ore", rock(MapColor.STONE, 5, 5));
    public static final DeferredBlock<Block> DENSE_NETHER_COAL_ORE = simple("dense_nether_coal_ore", rock(MapColor.STONE, 10, 10));
    public static final DeferredBlock<Block> PLANKS_TARRED = simple("planks_tarred",
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2, 3));
    public static final DeferredBlock<Block> WOOL_TARRED = simple("wool_tarred",
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).sound(SoundType.WOOL).strength(0.8f));
    public static final DeferredBlock<Block> WOOD_TAR_BLOCK = simple("wood_tar_block",
        BlockBehaviour.Properties.of().mapColor(MapColor.CLAY).sound(SoundType.SLIME_BLOCK).strength(2));

    // The half slab's 1.5 / setResistance(5). 1.12's separate `_double` blocks carried
    // setResistance(10); one block cannot hold two, so the half's value stands for both.
    public static final DeferredBlock<SlabBlock> MASONRY_BRICK_SLAB =
        register("masonry_brick_slab", SlabBlock::new, rock(MapColor.STONE, 1.5f, 3));
    public static final DeferredBlock<SlabBlock> REFRACTORY_BRICK_SLAB =
        register("refractory_brick_slab", SlabBlock::new, rock(MapColor.SAND, 1.5f, 3));

    // 1.12's BlockStairs took hardness and resistance from the model block, so these are
    // the brick blocks' 1.5 / 10 and 3 / 10.
    public static final DeferredBlock<StairBlock> MASONRY_BRICK_STAIRS =
        register("masonry_brick_stairs", properties -> stairs(MASONRY_BRICK_BLOCK, properties),
            rock(MapColor.STONE, 1.5f, 6));
    public static final DeferredBlock<StairBlock> REFRACTORY_BRICK_STAIRS =
        register("refractory_brick_stairs", properties -> stairs(REFRACTORY_BRICK_BLOCK, properties),
            rock(MapColor.SAND, 3, 6));

    public static final DeferredBlock<WallBlock> MASONRY_BRICK_WALL =
        register("masonry_brick_wall", WallBlock::new, rock(MapColor.STONE, 1.5f, 6));
    public static final DeferredBlock<WallBlock> REFRACTORY_BRICK_WALL =
        register("refractory_brick_wall", WallBlock::new, rock(MapColor.SAND, 3, 6));

    // Hardness 3 and nothing else; a 1.12 block that only set hardness has the same
    // resistance. The map colours are the 1.12 getMapColor overrides.
    public static final DeferredBlock<DoorBlock> REFRACTORY_DOOR =
        register("refractory_door", properties -> new DoorBlock(DOOR_SET_TYPE, properties),
            rock(MapColor.COLOR_BROWN, 3, 3).noOcclusion());
    public static final DeferredBlock<DoorBlock> STONE_DOOR =
        register("stone_door", properties -> new DoorBlock(DOOR_SET_TYPE, properties),
            rock(MapColor.STONE, 3, 3).noOcclusion());

    // 1.12's Material.GLASS needed no tool, so no requiresCorrectToolForDrops here.
    public static final DeferredBlock<ConnectedGlassBlock> REFRACTORY_GLASS =
        register("refractory_glass", properties -> new ConnectedGlassBlock(false, properties), glass());
    public static final DeferredBlock<ConnectedGlassBlock> SLAG_GLASS =
        register("slag_glass", properties -> new ConnectedGlassBlock(true, properties), glass());

    private CoreBlocks() {
    }

    private static StairBlock stairs(DeferredBlock<Block> base, BlockBehaviour.Properties properties) {
        return new StairBlock(base.get().defaultBlockState(), properties);
    }

    /** 1.12's {@code Material.GLASS} with hardness 0.3: no tool needed, and not opaque. */
    private static BlockBehaviour.Properties glass() {
        return BlockBehaviour.Properties.of()
            .sound(SoundType.GLASS)
            .strength(0.3f)
            .noOcclusion()
            .isValidSpawn((state, level, pos, type) -> false)
            .isRedstoneConductor((state, level, pos) -> false)
            .isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false);
    }

    /** A 1.12 {@code Material.ROCK} block: stone sound, and a pickaxe needed for drops. */
    private static BlockBehaviour.Properties rock(MapColor color, float destroyTime, float resistance) {
        return BlockBehaviour.Properties.of()
            .mapColor(color)
            .sound(SoundType.STONE)
            .strength(destroyTime, resistance)
            .requiresCorrectToolForDrops();
    }

    private static DeferredBlock<Block> simple(String name, BlockBehaviour.Properties properties) {
        DeferredBlock<Block> block = BLOCKS.registerSimpleBlock(name, properties);
        BLOCK_ITEMS.add(CoreItems.ITEMS.registerSimpleBlockItem(block));
        return block;
    }

    private static <B extends Block> DeferredBlock<B> register(String name,
                                                               Function<BlockBehaviour.Properties, ? extends B> constructor,
                                                               BlockBehaviour.Properties properties) {
        DeferredBlock<B> block = BLOCKS.registerBlock(name, constructor, properties);
        BLOCK_ITEMS.add(CoreItems.ITEMS.registerSimpleBlockItem(block));
        return block;
    }

    static void addToTab(CreativeModeTab.Output output) {
        for (DeferredItem<BlockItem> item : BLOCK_ITEMS) {
            output.accept(item.get());
        }
    }
}
