package com.yourname.magi.capability;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.util.LazyOptional;

public final class MagiCapabilities {
    public static final Capability<PlayerDjinnData> PLAYER_DJINN =
            CapabilityManager.get(new CapabilityToken<>() {});

    public static LazyOptional<PlayerDjinnData> get(Player player) {
        return player.getCapability(PLAYER_DJINN);
    }

    private MagiCapabilities() {}
}
