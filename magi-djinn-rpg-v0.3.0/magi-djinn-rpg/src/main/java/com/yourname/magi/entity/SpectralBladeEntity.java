package com.yourname.magi.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Purely visual: the giant spectral greatsword and magic circles of Bararaq Inqerad-Saiqa.
 * Its animation is a pure function of its age, so server and client agree without extra packets.
 * Gameplay (damage, storm, crater) lives in the art's timeline, not here. Never saved to disk.
 */
public class SpectralBladeEntity extends Entity {
    public static final int DESCEND_START = 40, IMPACT = 80, FADE_START = 175, LIFETIME = 200;
    public static final float SKY_HEIGHT = 46.0F;

    public SpectralBladeEntity(EntityType<? extends SpectralBladeEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** Height of the blade tip above this entity (the impact point) at a given age. */
    public static float tipHeight(float age) {
        if (age < DESCEND_START) return SKY_HEIGHT;
        if (age >= IMPACT) return -4.0F;
        float f = (age - DESCEND_START) / (IMPACT - DESCEND_START);
        return SKY_HEIGHT - (SKY_HEIGHT + 4.0F) * f * f;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount >= LIFETIME) discard();
    }

    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushable() { return false; }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return new ClientboundAddEntityPacket(this); }
    @Override public boolean shouldRenderAtSqrDistance(double distSqr) { return distSqr < 320.0 * 320.0; }
}
