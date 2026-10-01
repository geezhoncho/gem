package com.yourname.magi.weapon;

import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/** Scimitar / saber / dagger / spear. Extends SwordItem so other mods classify it as a melee sword. */
public class MagiSwordItem extends SwordItem {
    private final double reach;
    private volatile Multimap<Attribute, AttributeModifier> cachedMainHand;

    public MagiSwordItem(Tier tier, int damage, float speed, Properties properties, double reach) {
        super(tier, damage, speed, properties);
        this.reach = reach;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> base = super.getDefaultAttributeModifiers(slot);
        if (slot != EquipmentSlot.MAINHAND || reach == 0.0) return base;
        Multimap<Attribute, AttributeModifier> c = cachedMainHand;
        if (c == null) {
            c = WeaponAttributes.withReach(base, reach);
            cachedMainHand = c;
        }
        return c;
    }
}
