package com.yourname.magi.compat.irons;

import com.yourname.magi.MagiMod;
import com.yourname.magi.magic.ManaProvider;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Spends Iron's Spells mana through its public API class (io.redspace.ironsspellbooks.api.magic.MagicData),
 * looked up by reflection so this mod needs no compile-time dependency. Any mismatch (renamed method, new
 * Iron's version) disables the bridge and arts fall back to Djinn Energy instead of crashing.
 */
final class IronsMana implements ManaProvider {
    private final Method getData;
    private final Method getMana;
    private final Method setMana;
    private volatile boolean broken;

    private IronsMana(Method getData, Method getMana, Method setMana) {
        this.getData = getData;
        this.getMana = getMana;
        this.setMana = setMana;
    }

    @Nullable
    static IronsMana create() {
        try {
            Class<?> md = Class.forName("io.redspace.ironsspellbooks.api.magic.MagicData");
            Method get = null, getMana = null, setMana = null;
            for (Method m : md.getMethods()) {
                Class<?>[] p = m.getParameterTypes();
                if (m.getName().equals("getPlayerMagicData") && Modifier.isStatic(m.getModifiers())
                        && p.length == 1 && p[0].isAssignableFrom(ServerPlayer.class)) get = m;
                else if (m.getName().equals("getMana") && p.length == 0) getMana = m;
                else if (m.getName().equals("setMana") && p.length == 1 && (p[0] == float.class || p[0] == double.class)) setMana = m;
            }
            if (get == null || getMana == null || setMana == null) throw new NoSuchMethodException("MagicData mana methods");
            MagiMod.LOGGER.info("Iron's Spells mana bridge active: Djinn arts cost Iron's mana");
            return new IronsMana(get, getMana, setMana);
        } catch (Throwable t) {
            MagiMod.LOGGER.warn("Iron's Spells mana bridge unavailable ({}); Djinn arts will use Djinn Energy", t.toString());
            return null;
        }
    }

    @Override
    public Result trySpend(ServerPlayer player, float amount) {
        if (broken) return Result.UNAVAILABLE;
        try {
            Object data = getData.invoke(null, player);
            float mana = ((Number) getMana.invoke(data)).floatValue();
            if (mana < amount) return Result.INSUFFICIENT;
            if (setMana.getParameterTypes()[0] == double.class) setMana.invoke(data, (double) (mana - amount));
            else setMana.invoke(data, mana - amount);
            return Result.SPENT;
        } catch (Throwable t) {
            broken = true;
            MagiMod.LOGGER.warn("Iron's Spells mana bridge failed ({}); switching to Djinn Energy", t.toString());
            return Result.UNAVAILABLE;
        }
    }
}
