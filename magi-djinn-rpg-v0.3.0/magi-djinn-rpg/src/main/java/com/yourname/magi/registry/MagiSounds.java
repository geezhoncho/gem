package com.yourname.magi.registry;

import com.yourname.magi.MagiMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Own sound events (with subtitles) mapped to vanilla audio in sounds.json; a resource pack can replace them. */
public final class MagiSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MagiMod.MODID);

    public static final RegistryObject<SoundEvent> LIGHTNING_SPEAR = reg("art.lightning_spear");
    public static final RegistryObject<SoundEvent> CHAIN_LIGHTNING = reg("art.chain_lightning");
    public static final RegistryObject<SoundEvent> THUNDER_CAGE = reg("art.thunder_cage");
    public static final RegistryObject<SoundEvent> SAIQA_CHARGE = reg("art.saiqa_charge");
    public static final RegistryObject<SoundEvent> SAIQA_RELEASE = reg("art.saiqa_release");
    public static final RegistryObject<SoundEvent> EXTREME_CAST = reg("art.extreme_cast");
    public static final RegistryObject<SoundEvent> EXTREME_IMPACT = reg("art.extreme_impact");
    public static final RegistryObject<SoundEvent> LIGHTNING_DASH = reg("art.lightning_dash");

    private static RegistryObject<SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(MagiMod.id(name)));
    }

    private MagiSounds() {}
}
