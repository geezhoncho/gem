package com.yourname.magi.client;

import com.yourname.magi.capability.PlayerDjinnData;

/** Read-only client mirror of the local player's Djinn data. Never used for authority decisions. */
public final class ClientDjinnState {
    public static final PlayerDjinnData DATA = new PlayerDjinnData();
    public static volatile double regenPerTick = 0.1;
    public static volatile double maxEnergy = 0.0;

    public static double energy(long gameTime) {
        return DATA.currentEnergy(gameTime, regenPerTick, maxEnergy);
    }

    private ClientDjinnState() {}
}
