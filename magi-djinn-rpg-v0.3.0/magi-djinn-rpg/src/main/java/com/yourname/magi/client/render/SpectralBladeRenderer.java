package com.yourname.magi.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.yourname.magi.MagiMod;
import com.yourname.magi.entity.SpectralBladeEntity;
import com.yourname.magi.registry.MagiItems;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Renders the sky magic circle, the ground warning circle and the colossal spectral greatsword (a 3D item model x14). */
public class SpectralBladeRenderer extends EntityRenderer<SpectralBladeEntity> {
    private static final ResourceLocation CIRCLE = MagiMod.id("textures/effect/baal_magic_circle.png");
    /** Distance from the greatsword model's centre to its tip, in blocks at scale 1 (see tools/model_gen.py). */
    private static final float MODEL_TIP_OFFSET = (31.0F - 8.0F) / 16.0F;
    private static final float SCALE = 14.0F;
    private final ItemRenderer items;
    private ItemStack stack;

    public SpectralBladeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.items = ctx.getItemRenderer();
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(SpectralBladeEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(SpectralBladeEntity e, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float t = e.tickCount + partialTick;
        float appear = Math.min(1.0F, t / 20.0F);
        float fade = t > SpectralBladeEntity.FADE_START
                ? Math.max(0.0F, 1.0F - (t - SpectralBladeEntity.FADE_START) / (SpectralBladeEntity.LIFETIME - SpectralBladeEntity.FADE_START))
                : 1.0F;

        circle(pose, buffers, SpectralBladeEntity.SKY_HEIGHT + 3.0F, 22.0F * appear, t * 1.2F, 0.9F * appear * fade);
        if (t < SpectralBladeEntity.IMPACT + 20) {
            float shrink = t > SpectralBladeEntity.IMPACT ? 1.0F - (t - SpectralBladeEntity.IMPACT) / 20.0F : 1.0F;
            circle(pose, buffers, 0.12F, 13.0F * appear * shrink, -t * 2.0F, 0.6F * shrink);
        }

        if (fade <= 0.0F) return;
        if (stack == null) stack = new ItemStack(MagiItems.SPECTRAL_GREATSWORD.get());
        float tip = SpectralBladeEntity.tipHeight(t);
        float scale = SCALE * (0.85F + 0.15F * fade);
        pose.pushPose();
        pose.translate(0.0F, tip + MODEL_TIP_OFFSET * scale, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(-e.getYRot()));
        pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
        pose.scale(scale, scale, scale);
        items.renderStatic(stack, ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                pose, buffers, e.level(), e.getId());
        pose.popPose();
    }

    private static void circle(PoseStack pose, MultiBufferSource buffers, float y, float r, float spin, float alpha) {
        if (r <= 0.05F || alpha <= 0.01F) return;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(CIRCLE));
        pose.pushPose();
        pose.translate(0.0F, y, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        int a = (int) (alpha * 255);
        // both windings so the circle is visible from above and below
        v(vc, m, n, -r, -r, 0, 0, a); v(vc, m, n, -r, r, 0, 1, a); v(vc, m, n, r, r, 1, 1, a); v(vc, m, n, r, -r, 1, 0, a);
        v(vc, m, n, r, -r, 1, 0, a); v(vc, m, n, r, r, 1, 1, a); v(vc, m, n, -r, r, 0, 1, a); v(vc, m, n, -r, -r, 0, 0, a);
        pose.popPose();
    }

    private static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float z, float u, float vv, int a) {
        vc.vertex(m, x, 0.0F, z).color(255, 255, 255, a).uv(u, vv).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(n, 0.0F, 1.0F, 0.0F).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(SpectralBladeEntity entity) {
        return CIRCLE;
    }
}
