package com.yourname.magi.registry;

import com.yourname.magi.MagiMod;
import com.yourname.magi.entity.SpectralBladeEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MagiEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MagiMod.MODID);

    public static final RegistryObject<EntityType<SpectralBladeEntity>> SPECTRAL_BLADE = ENTITIES.register("spectral_blade",
            () -> EntityType.Builder.<SpectralBladeEntity>of(SpectralBladeEntity::new, MobCategory.MISC)
                    .sized(2.0F, 8.0F).noSave().fireImmune().clientTrackingRange(20).updateInterval(40)
                    .build("spectral_blade"));

    private MagiEntities() {}
}
