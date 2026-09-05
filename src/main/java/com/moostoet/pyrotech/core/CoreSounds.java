package com.moostoet.pyrotech.core;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.stream.IntStream;

/** The sound events core's {@code sounds.json} declares: the dense redstone ore crackles and tool's redstone tool sounds. */
public final class CoreSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, Pyrotech.MOD_ID);

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> DENSE_REDSTONE_ORE_ACTIVATE = series("dense_redstone_ore_activate_", 8);
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> REDSTONE_TOOL_ACTIVATE = series("redstone_tool_activate_", 6);

    private CoreSounds() {
    }

    public static SoundEvent randomDenseRedstoneOreActivate(RandomSource random) {
        return DENSE_REDSTONE_ORE_ACTIVATE.get(random.nextInt(DENSE_REDSTONE_ORE_ACTIVATE.size())).get();
    }

    private static List<DeferredHolder<SoundEvent, SoundEvent>> series(String prefix, int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> register(String.format("%s%02d", prefix, i)))
            .toList();
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, name)));
    }
}
