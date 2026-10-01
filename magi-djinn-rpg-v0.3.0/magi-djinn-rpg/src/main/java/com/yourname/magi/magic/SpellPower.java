package com.yourname.magi.magic;

import com.yourname.magi.config.MagiConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Reads Iron's Spells attributes BY ID (no compile dependency), so Iron's gear boosts Djinn arts.
 * Without Iron's every value is 1.0.
 */
public final class SpellPower {

    public static float multiplier(LivingEntity caster, String school) {
        double general = attribute(caster, "irons_spellbooks:spell_power", 1.0);
        double specific = attribute(caster, "irons_spellbooks:" + school + "_spell_power", 1.0);
        return (float) (general * specific * MagiConfig.SPELL_DAMAGE_MULTIPLIER.get());
    }

    /** Iron's cooldown_reduction is 1.0 by default; higher means shorter cooldowns. */
    public static float cooldownMultiplier(LivingEntity caster) {
        double cdr = attribute(caster, "irons_spellbooks:cooldown_reduction", 1.0);
        return (float) Mth.clamp(2.0 - cdr, 0.25, 1.0);
    }

    private static double attribute(LivingEntity entity, String id, double fallback) {
        Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(id));
        if (attr == null) return fallback;
        AttributeInstance inst = entity.getAttribute(attr);
        return inst == null ? fallback : inst.getValue();
    }

    private SpellPower() {}
}
