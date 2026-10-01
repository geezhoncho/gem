package com.yourname.magi.magic;

import com.yourname.magi.capability.MagiCapabilities;
import com.yourname.magi.capability.PlayerDjinnData;
import com.yourname.magi.config.MagiConfig;
import com.yourname.magi.djinn.DjinnDefinition;
import com.yourname.magi.djinn.DjinnManager;
import com.yourname.magi.networking.MagiNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.SwordItem;

/** Server-side validation and payment for every art cast. The client only ever asks. */
public final class ArtCaster {

    public static void tryCast(ServerPlayer player, ResourceLocation artId) {
        DjinnArt art = MagiArts.get(artId);
        if (art == null) return;
        PlayerDjinnData data = MagiCapabilities.get(player).resolve().orElse(null);
        if (data == null) return;

        ResourceLocation eq = data.getEquipped();
        DjinnDefinition def = eq == null ? null : DjinnManager.get(eq);
        if (def == null) { fail(player, "no_djinn"); return; }
        boolean belongs = def.abilities().contains(artId) || def.signature().map(artId::equals).orElse(false);
        if (!belongs) { fail(player, "wrong_djinn"); return; }
        if (art.requiresSword() && !(player.getMainHandItem().getItem() instanceof SwordItem)) { fail(player, "need_sword"); return; }
        int djinnLevel = data.level(eq);
        int minLevel = art.minDjinnLevel();
        if (djinnLevel < minLevel) { fail(player, "need_level", minLevel); return; }

        long now = player.level().getGameTime();
        long cd = data.cooldownRemaining(artId, now);
        if (cd > 0) { fail(player, "cooldown", String.format("%.1f", cd / 20.0)); return; }
        if (ArtScheduler.isChanneling(player)) { fail(player, "busy"); return; }

        double regen = MagiConfig.DJINN_ENERGY_REGEN_PER_SECOND.get() / 20.0;
        double maxEnergy = def.maxEnergy();
        double energy = data.currentEnergy(now, regen, maxEnergy);
        if (art.requiresFullEnergy() && energy < maxEnergy - 0.01) { fail(player, "need_full_energy"); return; }

        double energyNeed = art.requiresFullEnergy() ? maxEnergy : art.energyCost();
        float mana = (float) (art.manaCost() * MagiConfig.MANA_COST_MULTIPLIER.get());
        double manaAsEnergy = mana * MagiConfig.ENERGY_PER_MANA.get();
        ManaProvider provider = ManaProvider.Registry.get();

        if (mana > 0 && provider == null) energyNeed += manaAsEnergy;
        if (energy + 1.0E-6 < energyNeed) { fail(player, "no_energy"); return; }
        if (mana > 0 && provider != null) {
            ManaProvider.Result r = provider.trySpend(player, mana);
            if (r == ManaProvider.Result.INSUFFICIENT) { fail(player, "no_mana"); return; }
            if (r == ManaProvider.Result.UNAVAILABLE) {
                energyNeed += manaAsEnergy;
                if (energy + 1.0E-6 < energyNeed) { fail(player, "no_energy"); return; }
            }
        }
        if (energyNeed > 0) data.trySpendEnergy(now, regen, maxEnergy, Math.min(energyNeed, energy));

        float power = SpellPower.multiplier(player, art.school()) * (1.0F + 0.08F * (djinnLevel - 1));
        ArtSequence sequence = art.cast(new ArtContext(player.serverLevel(), player, djinnLevel, power));
        if (sequence != null) ArtScheduler.add(sequence);

        int cooldown = Math.max(1, Math.round(art.cooldownTicks() * SpellPower.cooldownMultiplier(player)));
        data.startCooldown(artId, now + cooldown);
        MagiNetwork.syncDjinnData(player, data);
    }

    private static void fail(ServerPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable("art.magi.fail." + key, args).withStyle(ChatFormatting.RED), true);
    }

    private ArtCaster() {}
}
