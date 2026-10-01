package com.yourname.magi.magic;

import com.yourname.magi.MagiMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Runs all active art timelines once per server tick. Sequences added during a tick start next tick. */
@Mod.EventBusSubscriber(modid = MagiMod.MODID)
public final class ArtScheduler {
    private static final int MAX_ACTIVE = 512;
    private static final List<ArtSequence> ACTIVE = new ArrayList<>();
    private static final List<ArtSequence> PENDING = new ArrayList<>();

    public static void add(ArtSequence sequence) {
        if (ACTIVE.size() + PENDING.size() < MAX_ACTIVE) PENDING.add(sequence);
    }

    public static boolean isChanneling(ServerPlayer player) {
        for (ArtSequence s : ACTIVE) if (s.blocksCasting() && s.caster() == player) return true;
        for (ArtSequence s : PENDING) if (s.blocksCasting() && s.caster() == player) return true;
        return false;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!PENDING.isEmpty()) {
            ACTIVE.addAll(PENDING);
            PENDING.clear();
        }
        Iterator<ArtSequence> it = ACTIVE.iterator();
        while (it.hasNext()) {
            ArtSequence s = it.next();
            boolean keep;
            try {
                keep = s.tick();
            } catch (Exception e) {
                MagiMod.LOGGER.error("Djinn art sequence failed and was stopped", e);
                keep = false;
            }
            if (!keep) it.remove();
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        ACTIVE.clear();
        PENDING.clear();
    }

    private ArtScheduler() {}
}
