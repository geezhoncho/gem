package com.yourname.magi.armor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

/**
 * An extra attribute bonus on an armor piece. The attribute is resolved lazily; if the supplier yields null
 * (attribute of an absent mod) the bonus is skipped, so armor stays valid without optional mods.
 */
public record ArmorBonus(Supplier<Attribute> attribute, double amount, AttributeModifier.Operation operation) {

    public static ArmorBonus of(Supplier<Attribute> attribute, double amount, AttributeModifier.Operation op) {
        return new ArmorBonus(attribute, amount, op);
    }

    /** Bonus on an attribute looked up by registry id (e.g. an Iron's Spells attribute). Null-safe. */
    public static ArmorBonus byId(String attributeId, double amount, AttributeModifier.Operation op) {
        ResourceLocation id = new ResourceLocation(attributeId);
        return new ArmorBonus(() -> ForgeRegistries.ATTRIBUTES.getValue(id), amount, op);
    }
}
