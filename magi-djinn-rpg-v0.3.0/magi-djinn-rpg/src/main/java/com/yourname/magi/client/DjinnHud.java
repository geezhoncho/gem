package com.yourname.magi.client;

import com.yourname.magi.MagiMod;
import com.yourname.magi.djinn.DjinnDefinition;
import com.yourname.magi.djinn.MagiDjinn;
import com.yourname.magi.magic.DjinnArt;
import com.yourname.magi.magic.MagiArts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

/** Gold-framed Djinn panel (bottom left): Djinn + level, energy bar, selected art and signature ability with cooldowns. */
@Mod.EventBusSubscriber(modid = MagiMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DjinnHud {
    private static final int GOLD = 0xFFD4AF37, GOLD_DARK = 0xFF8A6A1E, PANEL = 0xC0101828, TEXT = 0xFFF2E6C8;

    @SubscribeEvent
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "djinn_hud", DjinnHud::render);
    }

    private static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null) return;
        ResourceLocation djinn = ClientDjinnState.DATA.getEquipped();
        DjinnDefinition def = MagiKeys.equippedDefinition();
        if (djinn == null || def == null) return;
        Font font = mc.font;
        long now = mc.level.getGameTime();
        int x = 6, y = height - 62, w = 152, h = 56;

        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, GOLD_DARK);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, GOLD);
        g.fill(x, y, x + w, y + h, PANEL);

        int color = 0xFF000000 | def.color();
        g.fill(x + 4, y + 4, x + 10, y + 10, color);
        Component name = Component.translatable(MagiDjinn.nameKey(djinn)).append(" Lv" + ClientDjinnState.DATA.level(djinn));
        g.drawString(font, name, x + 14, y + 3, TEXT, true);

        double max = Math.max(1.0, ClientDjinnState.maxEnergy);
        double energy = ClientDjinnState.energy(now);
        int barW = w - 8;
        g.fill(x + 4, y + 14, x + 4 + barW, y + 19, 0xFF2A2440);
        g.fill(x + 4, y + 14, x + 4 + (int) (barW * Math.min(1.0, energy / max)), y + 19, color);
        g.fill(x + 4, y + 14, x + 4 + barW, y + 15, 0x40FFFFFF);

        art(g, font, MagiKeys.selectedArt(), x + 4, y + 24, now, MagiKeys.CAST.getTranslatedKeyMessage(), true);
        art(g, font, MagiKeys.signature(), x + 4, y + 40, now, MagiKeys.SIGNATURE.getTranslatedKeyMessage(), false);
    }

    private static void art(GuiGraphics g, Font font, @Nullable ResourceLocation id, int x, int y, long now, Component key, boolean showNext) {
        if (id == null) return;
        DjinnArt art = MagiArts.get(id);
        if (art == null) return;
        ResourceLocation icon = new ResourceLocation(id.getNamespace(), "textures/gui/arts/" + id.getPath() + ".png");
        g.blit(icon, x, y, 14, 14, 0, 0, 24, 24, 24, 24);
        long remaining = ClientDjinnState.DATA.cooldownRemaining(id, now);
        if (remaining > 0) {
            float frac = Math.min(1.0F, remaining / (float) Math.max(1, art.cooldownTicks()));
            g.fill(x, y + (int) (14 * (1 - frac)), x + 14, y + 14, 0xB0000000);
        }
        String label = "[" + key.getString() + "] " + Component.translatable(art.nameKey()).getString();
        if (label.length() > 24) label = label.substring(0, 23) + "…";
        g.drawString(font, label, x + 18, y + 3, remaining > 0 ? 0xFF8A8A8A : TEXT, true);
        if (remaining > 0) {
            String s = String.format("%.0fs", Math.ceil(remaining / 20.0));
            g.drawString(font, s, x + 146 - font.width(s), y + 3, 0xFFFF6A6A, true);
        } else if (showNext) {
            String s = "[" + MagiKeys.NEXT.getTranslatedKeyMessage().getString() + "]";
            g.drawString(font, s, x + 146 - font.width(s), y + 3, 0xFF9AA8C8, true);
        }
    }

    private DjinnHud() {}
}
