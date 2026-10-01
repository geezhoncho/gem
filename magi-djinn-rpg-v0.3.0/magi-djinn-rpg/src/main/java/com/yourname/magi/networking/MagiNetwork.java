package com.yourname.magi.networking;

import com.yourname.magi.MagiMod;
import com.yourname.magi.capability.PlayerDjinnData;
import com.yourname.magi.config.MagiConfig;
import com.yourname.magi.djinn.DjinnDefinition;
import com.yourname.magi.djinn.DjinnManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class MagiNetwork {
    /** Bump whenever packets change; mismatched client/server versions are refused cleanly at login. */
    private static final String PROTOCOL = "3";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            MagiMod.id("main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static int nextId = 0;

    public static void register() {
        CHANNEL.registerMessage(nextId++, SyncDjinnDataPacket.class,
                SyncDjinnDataPacket::encode, SyncDjinnDataPacket::decode, SyncDjinnDataPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextId++, EquipDjinnPacket.class,
                EquipDjinnPacket::encode, EquipDjinnPacket::decode, EquipDjinnPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(nextId++, SyncDjinnDefinitionsPacket.class,
                SyncDjinnDefinitionsPacket::encode, SyncDjinnDefinitionsPacket::decode, SyncDjinnDefinitionsPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(nextId++, CastArtPacket.class,
                CastArtPacket::encode, CastArtPacket::decode, CastArtPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(nextId++, ArtFxPacket.class,
                ArtFxPacket::encode, ArtFxPacket::decode, ArtFxPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void syncDjinnData(ServerPlayer player, PlayerDjinnData data) {
        CompoundTag tag = data.serializeNBT();
        ResourceLocation eq = data.getEquipped();
        DjinnDefinition def = eq == null ? null : DjinnManager.get(eq);
        tag.putDouble("RegenPerTick", MagiConfig.DJINN_ENERGY_REGEN_PER_SECOND.get() / 20.0);
        tag.putDouble("MaxEnergy", def == null ? 0.0 : def.maxEnergy());
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncDjinnDataPacket(tag));
    }

    public static void sendDefinitions(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), SyncDjinnDefinitionsPacket.fromServer());
    }

    private MagiNetwork() {}
}
