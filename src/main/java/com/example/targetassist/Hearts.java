package com.example.targetassist;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Draws a row of real Minecraft hearts above players and mobs (client-only).
// Each heart is 2 health. Anything with more than 20 max health is scaled down to 10 hearts.
@Mod.EventBusSubscriber(modid = TargetAssistMod.MODID, value = Dist.CLIENT)
public class Hearts {
    private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
    private static final float HALF_W = 0.075f;  // half the width of one heart, in blocks

    @SubscribeEvent
    public static void onLivingPost(RenderLivingEvent.Post<?, ?> e) {
        if (!Settings.healthIndicators) return;
        LivingEntity en = e.getEntity();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || en == mc.player || en == mc.getCameraEntity()) return;
        if (!(en instanceof Player || en instanceof Mob) || !en.isAlive()) return;
        if (mc.player.distanceTo(en) > 64) return;

        float max = Math.max(en.getMaxHealth(), 1f);
        int count = Math.min(10, (int) Math.ceil(max / 2f));
        float filled = Mth.clamp(en.getHealth() / max, 0f, 1f) * count;  // measured in hearts

        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(0.0, en.getBbHeight() + 0.3, 0.0);  // between the head and the name tag
        ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        PoseStack.Pose pose = ps.last();
        VertexConsumer vc = e.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(ICONS));

        float total = count * HALF_W * 2f;
        for (int i = 0; i < count; i++) {
            // the billboard's +x points to the viewer's left, so count down from the left edge
            float cx = total / 2f - (i + 0.5f) * HALF_W * 2f;
            quad(vc, pose, cx, 16f, 0f);  // empty heart container
            float fill = Mth.clamp(filled - i, 0f, 1f);
            if (fill >= 0.75f) quad(vc, pose, cx, 52f, 0.001f);       // full heart
            else if (fill >= 0.25f) quad(vc, pose, cx, 61f, 0.001f);  // half heart
        }
        ps.popPose();
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, float cx, float uPx, float z) {
        float h = HALF_W;
        float u0 = uPx / 256f, u1 = (uPx + 9f) / 256f, v0 = 0f, v1 = 9f / 256f;
        int light = LightTexture.FULL_BRIGHT;
        vc.vertex(pose.pose(), cx + h, -h, z).color(255, 255, 255, 255).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0f, 0f, 1f).endVertex();
        vc.vertex(pose.pose(), cx - h, -h, z).color(255, 255, 255, 255).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0f, 0f, 1f).endVertex();
        vc.vertex(pose.pose(), cx - h, h, z).color(255, 255, 255, 255).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0f, 0f, 1f).endVertex();
        vc.vertex(pose.pose(), cx + h, h, z).color(255, 255, 255, 255).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0f, 0f, 1f).endVertex();
    }
}
