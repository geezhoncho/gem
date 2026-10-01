package com.yourname.magi.magic;

import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/** Port for an external mana pool (Iron's Spells). Absent = arts pay in Djinn Energy. */
public interface ManaProvider {
    enum Result { SPENT, INSUFFICIENT, UNAVAILABLE }

    Result trySpend(ServerPlayer player, float amount);

    final class Registry {
        private static volatile ManaProvider active;

        public static void set(@Nullable ManaProvider provider) { active = provider; }

        @Nullable
        public static ManaProvider get() { return active; }

        private Registry() {}
    }
}
