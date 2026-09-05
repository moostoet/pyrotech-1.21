package com.moostoet.pyrotech.core;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.block.AshPileBlock;
import com.moostoet.pyrotech.core.block.BerryBushBlock;
import com.moostoet.pyrotech.core.block.ConnectedGlassBlock;
import com.moostoet.pyrotech.core.block.DenseQuartzOreBlock;
import com.moostoet.pyrotech.core.block.DenseRedstoneOreBlock;
import com.moostoet.pyrotech.core.block.DryCobBlock;
import com.moostoet.pyrotech.core.block.FreckleberryPlantBlock;
import com.moostoet.pyrotech.core.block.GloamberryBushBlock;
import com.moostoet.pyrotech.core.block.GrassRockBlock;
import com.moostoet.pyrotech.core.block.LivingTarBlock;
import com.moostoet.pyrotech.core.block.LogPileBlock;
import com.moostoet.pyrotech.core.block.MudBlock;
import com.moostoet.pyrotech.core.block.MudLayerBlock;
import com.moostoet.pyrotech.core.block.MulchedFarmlandBlock;
import com.moostoet.pyrotech.core.block.NetherrackRockBlock;
import com.moostoet.pyrotech.core.block.PyroberryBushBlock;
import com.moostoet.pyrotech.core.block.RockBlock;
import com.moostoet.pyrotech.core.block.StrawBedBlock;
import com.moostoet.pyrotech.core.block.ThatchBlock;
import com.moostoet.pyrotech.core.block.WetCobBlock;
import com.moostoet.pyrotech.core.block.WoodChipsPileBlock;
import com.moostoet.pyrotech.core.entity.ThrownRockEntity;
import com.moostoet.pyrotech.core.item.RockItem;
import com.moostoet.pyrotech.core.item.StrawBedItem;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
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
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

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

    // -- Slice 3: the behaviour blocks --------------------------------------

    /** The four cobblestones 1.12 kept as one variant block. Hardness 3 and setResistance(5). */
    public static final DeferredBlock<Block> COBBLESTONE_ANDESITE = simple("cobblestone_andesite", rock(MapColor.STONE, 3, 3));
    public static final DeferredBlock<Block> COBBLESTONE_DIORITE = simple("cobblestone_diorite", rock(MapColor.STONE, 3, 3));
    public static final DeferredBlock<Block> COBBLESTONE_GRANITE = simple("cobblestone_granite", rock(MapColor.STONE, 3, 3));
    public static final DeferredBlock<Block> COBBLESTONE_LIMESTONE = simple("cobblestone_limestone", rock(MapColor.STONE, 3, 3));

    /** Every rock item, so a throw can put them all on cooldown. Filled by {@link #rock}. */
    public static final List<DeferredItem<RockItem>> ROCK_ITEMS = new ArrayList<>();

    // 1.12's BlockRock variants: Material.PLANTS, hardness 0.1, and a sound per variant.
    public static final DeferredBlock<RockBlock> ROCK_STONE = rock("rock_stone", MapColor.STONE, SoundType.STONE);
    public static final DeferredBlock<RockBlock> ROCK_GRANITE = rock("rock_granite", MapColor.DIRT, SoundType.STONE);
    public static final DeferredBlock<RockBlock> ROCK_DIORITE = rock("rock_diorite", MapColor.QUARTZ, SoundType.STONE);
    public static final DeferredBlock<RockBlock> ROCK_ANDESITE = rock("rock_andesite", MapColor.STONE, SoundType.STONE);
    public static final DeferredBlock<RockBlock> ROCK_DIRT = rock("rock_dirt", MapColor.DIRT, SoundType.GRAVEL);
    public static final DeferredBlock<RockBlock> ROCK_SAND = rock("rock_sand", MapColor.SAND, SoundType.SAND);
    public static final DeferredBlock<RockBlock> ROCK_SANDSTONE = rock("rock_sandstone", MapColor.SAND, SoundType.STONE);
    public static final DeferredBlock<RockBlock> ROCK_WOOD_CHIPS = rock("rock_wood_chips", MapColor.WOOD, SoundType.GRAVEL);
    public static final DeferredBlock<RockBlock> ROCK_LIMESTONE = rock("rock_limestone", MapColor.STONE, SoundType.STONE);
    public static final DeferredBlock<RockBlock> ROCK_SAND_RED = rock("rock_sand_red", MapColor.COLOR_ORANGE, SoundType.SAND);
    public static final DeferredBlock<RockBlock> ROCK_SANDSTONE_RED = rock("rock_sandstone_red", MapColor.COLOR_ORANGE, SoundType.STONE);
    public static final DeferredBlock<RockBlock> ROCK_MUD = rock("rock_mud", MapColor.DIRT, SoundType.STONE);
    public static final DeferredBlock<RockBlock> ROCK_GRASS = rock("rock_grass", GrassRockBlock::new, CoreEntities.ROCK_GRASS,
        rockProperties(MapColor.GRASS, SoundType.GRASS).randomTicks());
    public static final DeferredBlock<RockBlock> ROCK_NETHERRACK = rock("rock_netherrack", NetherrackRockBlock::new, CoreEntities.ROCK_NETHERRACK,
        rockProperties(MapColor.NETHER, SoundType.STONE).randomTicks());

    // The bushes have no item; their seeds place them. Sound and hardness follow the age.
    public static final DeferredBlock<BerryBushBlock> PYROBERRY_BUSH = BLOCKS.registerBlock("pyroberry_bush", PyroberryBushBlock::new, bush());
    public static final DeferredBlock<BerryBushBlock> GLOAMBERRY_BUSH = BLOCKS.registerBlock("gloamberry_bush", GloamberryBushBlock::new, bush());
    public static final DeferredBlock<FreckleberryPlantBlock> FRECKLEBERRY_PLANT = BLOCKS.registerBlock("freckleberry_plant", FreckleberryPlantBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak()
            .sound(SoundType.CROP).pushReaction(PushReaction.DESTROY).offsetType(BlockBehaviour.OffsetType.XZ));

    public static final DeferredBlock<MudBlock> MUD = register("mud", MudBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).sound(SoundType.SLIME_BLOCK).strength(0.4f));
    public static final DeferredBlock<MudLayerBlock> MUD_LAYER = register("mud_layer", MudLayerBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).sound(SoundType.SLIME_BLOCK).strength(0.1f)
            .noOcclusion().replaceable().pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<WetCobBlock> COB_WET = register("cob_wet", WetCobBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).sound(SoundType.SLIME_BLOCK).strength(0.4f).randomTicks());
    // Hardness 1.2 and setResistance(10), pickaxe level 1.
    public static final DeferredBlock<DryCobBlock> COB_DRY = register("cob_dry", DryCobBlock::new, rock(MapColor.DIRT, 1.2f, 6).randomTicks());

    public static final DeferredBlock<LogPileBlock> LOG_PILE = register("log_pile", LogPileBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(2));
    // The piles drop through their own harvest code, one level at a time, so no loot table.
    public static final DeferredBlock<AshPileBlock> PILE_ASH = register("pile_ash", AshPileBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).sound(SoundType.SAND).strength(0.1f, 0).noOcclusion().noLootTable());
    public static final DeferredBlock<WoodChipsPileBlock> PILE_WOOD_CHIPS = register("pile_wood_chips", WoodChipsPileBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.GRAVEL).strength(0.25f, 0).noOcclusion().noLootTable());

    public static final DeferredBlock<MulchedFarmlandBlock> FARMLAND_MULCHED = register("farmland_mulched", MulchedFarmlandBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).sound(SoundType.GRAVEL).strength(0.7f).randomTicks()
            .isViewBlocking((state, level, pos) -> true).isSuffocating((state, level, pos) -> true));

    // 1.12 Material.CLOTH with the default hardness of zero and the sand map colour.
    public static final DeferredBlock<StrawBedBlock> STRAW_BED = BLOCKS.registerBlock("straw_bed", StrawBedBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.SAND).sound(SoundType.GRASS).instabreak().noOcclusion().pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> STRAW_BED_ITEM = blockItem(STRAW_BED,
        properties -> new StrawBedItem(STRAW_BED.get(), properties.stacksTo(1)));

    // Hardness 3, setResistance(5), pickaxe level 2. Large and small stand two to fourteen
    // sixteenths wide; the rocks lie flat with no collision, like a rock.
    private static final VoxelShape ORE_CLUSTER_SHAPE = Block.box(2, 0, 2, 14, 16, 14);
    private static final VoxelShape ORE_ROCKS_SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public static final DeferredBlock<DenseQuartzOreBlock> DENSE_QUARTZ_ORE_LARGE = register("dense_quartz_ore_large",
        properties -> new DenseQuartzOreBlock(ORE_CLUSTER_SHAPE, UniformInt.of(5, 14), properties), ore());
    public static final DeferredBlock<DenseQuartzOreBlock> DENSE_QUARTZ_ORE_SMALL = register("dense_quartz_ore_small",
        properties -> new DenseQuartzOreBlock(ORE_CLUSTER_SHAPE, UniformInt.of(2, 5), properties), ore());
    public static final DeferredBlock<DenseQuartzOreBlock> DENSE_QUARTZ_ORE_ROCKS = register("dense_quartz_ore_rocks",
        properties -> new DenseQuartzOreBlock(ORE_ROCKS_SHAPE, UniformInt.of(1, 2), properties), ore().noCollission());

    public static final DeferredBlock<DenseRedstoneOreBlock> DENSE_REDSTONE_ORE_LARGE = register("dense_redstone_ore_large",
        properties -> new DenseRedstoneOreBlock(ORE_CLUSTER_SHAPE, UniformInt.of(5, 14), 8, 3, properties), redstoneOre(11));
    public static final DeferredBlock<DenseRedstoneOreBlock> DENSE_REDSTONE_ORE_SMALL = register("dense_redstone_ore_small",
        properties -> new DenseRedstoneOreBlock(Block.box(4, 0, 4, 12, 8, 12), UniformInt.of(2, 4), 4, 2, properties), redstoneOre(9));
    public static final DeferredBlock<DenseRedstoneOreBlock> DENSE_REDSTONE_ORE_ROCKS = register("dense_redstone_ore_rocks",
        properties -> new DenseRedstoneOreBlock(ORE_ROCKS_SHAPE, UniformInt.of(1, 2), 2, 1, properties), redstoneOre(7).noCollission());

    public static final DeferredBlock<LivingTarBlock> LIVING_TAR = register("living_tar", LivingTarBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).sound(SoundType.SLIME_BLOCK).strength(0.8f).requiresCorrectToolForDrops());
    public static final DeferredBlock<ThatchBlock> THATCH = register("thatch", ThatchBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).sound(SoundType.GRASS).strength(0.5f));

    private CoreBlocks() {
    }

    /** A 1.12 {@code Material.PLANTS} rock: hardness 0.1, no collision, replaceable, pushed apart by pistons. */
    private static BlockBehaviour.Properties rockProperties(MapColor color, SoundType sound) {
        return BlockBehaviour.Properties.of()
            .mapColor(color)
            .sound(sound)
            .strength(0.1f)
            .noCollission()
            .noOcclusion()
            .replaceable()
            .pushReaction(PushReaction.DESTROY);
    }

    private static DeferredBlock<RockBlock> rock(String name, MapColor color, SoundType sound) {
        boolean woodChips = name.equals("rock_wood_chips");
        return rock(name, properties -> new RockBlock(woodChips, properties), CoreEntities.ROCK, rockProperties(color, sound));
    }

    private static DeferredBlock<RockBlock> rock(String name, Function<BlockBehaviour.Properties, ? extends RockBlock> constructor,
                                                Supplier<EntityType<ThrownRockEntity>> thrown, BlockBehaviour.Properties properties) {
        DeferredBlock<RockBlock> block = BLOCKS.registerBlock(name, constructor, properties);
        DeferredItem<RockItem> item = CoreItems.ITEMS.registerItem(name, p -> new RockItem(block.get(), thrown, p));
        ROCK_ITEMS.add(item);
        BLOCK_ITEMS.add(cast(item));
        return block;
    }

    /** 1.12 {@code BlockBushBase}: Material.WOOD, hardness 1, random ticks, and the sixteenth offset. */
    private static BlockBehaviour.Properties bush() {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .strength(1)
            .randomTicks()
            .noOcclusion()
            .dynamicShape()
            .offsetType(BlockBehaviour.OffsetType.XZ);
    }

    private static BlockBehaviour.Properties ore() {
        return rock(MapColor.STONE, 3, 3).noOcclusion();
    }

    private static BlockBehaviour.Properties redstoneOre(int litLight) {
        return ore().lightLevel(state -> state.getValue(DenseRedstoneOreBlock.LIT) ? litLight : 0);
    }

    private static DeferredItem<BlockItem> blockItem(DeferredBlock<? extends Block> block,
                                                     Function<Item.Properties, ? extends BlockItem> constructor) {
        DeferredItem<BlockItem> item = cast(CoreItems.ITEMS.registerItem(block.getId().getPath(), constructor));
        BLOCK_ITEMS.add(item);
        return item;
    }

    @SuppressWarnings("unchecked")
    private static DeferredItem<BlockItem> cast(DeferredItem<? extends BlockItem> item) {
        return (DeferredItem<BlockItem>) item;
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
