package com.yourname.magi.armor;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.yourname.magi.config.MagiConfig;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Armor with extra attribute bonuses. Extends ArmorItem so Apotheosis/Epic Fight recognise it as armor.
 * Vanilla armor/toughness/knockback-resistance stay in the material; only extras are added here.
 */
public class MagiArmorItem extends ArmorItem {
    private final List<ArmorBonus> bonuses;
    private volatile Multimap<Attribute, AttributeModifier> cached;

    public MagiArmorItem(ArmorMaterial material, Type type, Properties properties, List<ArmorBonus> bonuses) {
        super(material, type, properties);
        this.bonuses = List.copyOf(bonuses);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> base = super.getDefaultAttributeModifiers(slot);
        if (slot != getType().getSlot() || bonuses.isEmpty()) return base;

        Multimap<Attribute, AttributeModifier> c = cached;
        if (c != null) return c;

        boolean configLoaded = MagiConfig.SPEC.isLoaded();
        double mult = configLoaded ? MagiConfig.ARMOR_BONUS_MULTIPLIER.get() : 1.0;

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.putAll(base);
        for (ArmorBonus bonus : bonuses) {
            Attribute attr = bonus.attribute().get();
            if (attr == null) continue; // optional mod absent
            String key = String.valueOf(ForgeRegistries.ATTRIBUTES.getKey(attr));
            UUID uuid = UUID.nameUUIDFromBytes(
                    ("magi_armor|" + getMaterial().getName() + "|" + getType().getName() + "|" + key)
                            .getBytes(StandardCharsets.UTF_8));
            builder.put(attr, new AttributeModifier(uuid, "Magi armor bonus", bonus.amount() * mult, bonus.operation()));
        }
        Multimap<Attribute, AttributeModifier> built = builder.build();
        if (configLoaded) cached = built; // don't cache a pre-config value
        return built;
    }
}
