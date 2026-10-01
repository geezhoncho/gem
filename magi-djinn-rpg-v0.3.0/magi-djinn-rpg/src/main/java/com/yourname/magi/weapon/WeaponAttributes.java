package com.yourname.magi.weapon;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.ForgeMod;

import java.util.UUID;

final class WeaponAttributes {
    private static final UUID REACH_UUID = UUID.fromString("5c3b1c9e-6f1e-4c0a-9a53-2b7d0d3f8a11");

    /** Adds an entity-reach modifier to an existing main-hand modifier map. */
    static Multimap<Attribute, AttributeModifier> withReach(Multimap<Attribute, AttributeModifier> base, double reach) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> b = ImmutableMultimap.builder();
        b.putAll(base);
        b.put(ForgeMod.ENTITY_REACH.get(),
                new AttributeModifier(REACH_UUID, "Magi weapon reach", reach, AttributeModifier.Operation.ADDITION));
        return b.build();
    }

    private WeaponAttributes() {}
}
