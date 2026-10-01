package com.yourname.magi.weapon;

import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;

public class MagiBowItem extends BowItem {
    private final double bonusArrowDamage;

    public MagiBowItem(Properties properties, double bonusArrowDamage) {
        super(properties);
        this.bonusArrowDamage = bonusArrowDamage;
    }

    /**
     * UNVERIFIED HOOK: 1.20.1's BowItem is believed to expose customArrow(AbstractArrow) and call it from
     * releaseUsing. Deliberately NOT annotated @Override so this compiles either way; if the hook does not
     * exist the bow simply has no damage bonus (check in game: tooltips won't show it, damage will).
     */
    public AbstractArrow customArrow(AbstractArrow arrow) {
        if (bonusArrowDamage != 0.0) arrow.setBaseDamage(arrow.getBaseDamage() + bonusArrowDamage);
        return arrow;
    }
}
