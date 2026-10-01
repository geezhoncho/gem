package com.yourname.magi.magic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Everything an art needs when it fires. power = spell power x Djinn level x config. */
public record ArtContext(ServerLevel level, ServerPlayer caster, int djinnLevel, float power) {}
