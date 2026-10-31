package com.example.targetassist;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TargetAssistMod.MODID, value = Dist.CLIENT)
public class ClientEvents {

    private static final Set<Integer> glowing = new HashSet<>();
    private static LivingEntity target;
    private static final Map<UUID, Component> tabOriginal = new HashMap<>();
    private static final Map<UUID, Component> tabApplied = new HashMap<>();
    private static long acquiredAt, lastNanos;

    // ---------- per tick: GUI key + player sight ----------
    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            glowing.clear();
            tabOriginal.clear();
            tabApplied.clear();
            return;
        }
        updateTabList(mc);
        while (Keys.OPEN_GUI.consumeClick()) {
            if (mc.screen == null) mc.setScreen(new SettingsScreen());
        }
        for (Player p : mc.level.players()) {
            if (p == mc.player) continue;
            if (Settings.playerSight) {
                p.setGlowingTag(true);
                glowing.add(p.getId());
            } else if (glowing.remove(p.getId())) {
                p.setGlowingTag(p.hasEffect(MobEffects.GLOWING));
            }
        }
    }

    // ---------- tab list: gold name for nearby players ----------
    private static void updateTabList(Minecraft mc) {
        if (mc.getConnection() == null || mc.player == null || mc.level == null) return;
        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            UUID id = info.getProfile().getId();
            Player other = mc.level.getPlayerByUUID(id);
            boolean near = Settings.tabHighlight && other != null && other != mc.player
                    && mc.player.distanceTo(other) <= Settings.nearbyRange;
            if (near) {
                Component current = info.getTabListDisplayName();
                if (current == null || current != tabApplied.get(id)) {
                    tabOriginal.put(id, current);  // remember what the server had set (may be null)
                    Component gold = Component.literal(current != null ? current.getString() : info.getProfile().getName())
                            .withStyle(ChatFormatting.GREEN);
                    tabApplied.put(id, gold);
                    info.setTabListDisplayName(gold);
                }
            } else if (tabApplied.remove(id) != null) {
                info.setTabListDisplayName(tabOriginal.remove(id));
            }
        }
    }

    // ---------- per frame: smooth lock-on ----------
    @SubscribeEvent
    public static void onRender(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        long now = System.nanoTime();
        double dt = Math.min((now - lastNanos) / 1e9, 0.1);
        lastNanos = now;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.screen != null || !Settings.lockOn || !Keys.LOCK_ON.isDown()) {
            target = null;
            return;
        }

        // every frame, pick whatever is closest to the crosshair (or closest to you, if that mode is on)
        LivingEntity best = pickTarget(p);
        if (!Settings.closestToMe && target != null && best != null && best != target
                && valid(p, target) && angle(p, target) <= angle(p, best) + 6.0) {
            best = target;  // small stickiness so the lock does not flicker between two close targets
        }
        if (best != target) {
            target = best;
            acquiredAt = System.currentTimeMillis();
        }
        if (target == null) return;
        if (System.currentTimeMillis() - acquiredAt < (long) Settings.reactionMs) return;

        Vec3 eye = p.getEyePosition();
        Vec3 aim = aimPoint(target);
        double dx = aim.x - eye.x, dy = aim.y - eye.y, dz = aim.z - eye.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float wantYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float wantPitch = (float) -Math.toDegrees(Math.atan2(dy, horiz));

        double errYaw = Mth.wrapDegrees(wantYaw - p.getYRot());
        double errPitch = wantPitch - p.getXRot();
        double k = 40.0 - 36.0 * (Settings.lockSmooth / 100.0);   // higher smoothing = softer approach
        double ease = 1.0 - Math.exp(-k * dt);
        double maxStep = Settings.lockSpeed * dt;                 // hard cap in degrees this frame
        float yaw = p.getYRot() + (float) Mth.clamp(errYaw * ease, -maxStep, maxStep);
        float pitch = Mth.clamp(p.getXRot() + (float) Mth.clamp(errPitch * ease, -maxStep, maxStep), -90f, 90f);
        p.setYRot(yaw);
        p.setXRot(pitch);
        p.yRotO = yaw;
        p.xRotO = pitch;
    }

    private static boolean wanted(LocalPlayer p, LivingEntity ent) {
        if (ent == p || !ent.isAlive() || ent.isSpectator()) return false;
        if (ent instanceof Player) return Settings.tPlayers;
        if (ent instanceof Zombie) return Settings.tZombies;
        if (ent instanceof Enemy) return Settings.tHostile;
        return Settings.tPassive && ent instanceof Mob;
    }

    private static boolean valid(LocalPlayer p, LivingEntity ent) {
        if (!wanted(p, ent)) return false;
        if (p.distanceTo(ent) > Settings.lockRange) return false;
        if (!Settings.closestToMe && angle(p, ent) > Settings.lockCone) return false;
        return !Settings.losCheck || visible(p, ent);
    }

    // angle in degrees between where you look and the target
    private static double angle(LocalPlayer p, LivingEntity ent) {
        Vec3 dir = aimPoint(ent).subtract(p.getEyePosition()).normalize();
        double dot = Mth.clamp(p.getLookAngle().dot(dir), -1.0, 1.0);
        return Math.toDegrees(Math.acos(dot));
    }

    // own raycasts (the built-in check gives up past 128 blocks); head, middle or feet visible is enough
    private static boolean visible(LocalPlayer p, LivingEntity ent) {
        Vec3 from = p.getEyePosition();
        Vec3[] points = {
                ent.getEyePosition(),
                new Vec3(ent.getX(), ent.getY() + ent.getBbHeight() * 0.5, ent.getZ()),
                new Vec3(ent.getX(), ent.getY() + 0.2, ent.getZ())
        };
        for (Vec3 to : points) {
            ClipContext ctx = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p);
            if (p.level().clip(ctx).getType() == HitResult.Type.MISS) return true;
        }
        return false;
    }

    private static Vec3 aimPoint(LivingEntity ent) {
        if (Settings.aimHead) return ent.getEyePosition();
        return new Vec3(ent.getX(), ent.getY() + ent.getBbHeight() * 0.65, ent.getZ());
    }

    private static LivingEntity pickTarget(LocalPlayer p) {
        double bestScore = Double.MAX_VALUE;
        LivingEntity best = null;
        List<LivingEntity> nearby = p.level().getEntitiesOfClass(LivingEntity.class,
                p.getBoundingBox().inflate(Settings.lockRange), ent -> wanted(p, ent));
        for (LivingEntity cand : nearby) {
            if (!valid(p, cand)) continue;
            double score = Settings.closestToMe ? p.distanceTo(cand) : angle(p, cand);
            if (score < bestScore) {
                bestScore = score;
                best = cand;
            }
        }
        return best;
    }

    // ---------- health indicators ----------
    @SubscribeEvent
    public static void onNameTag(RenderNameTagEvent e) {
        boolean marker = Settings.invisMarkers && e.getEntity() instanceof Player && e.getEntity().isInvisible();
        if (!Settings.healthIndicators && !marker) return;
        Entity en = e.getEntity();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || en == mc.player) return;
        if (!(en instanceof LivingEntity le) || !(en instanceof Player || en instanceof Mob)) return;
        if (mc.player.distanceTo(en) > 64) return;
        if (false) /* the number is replaced by hearts, see Hearts.java */ e.setContent(Component.empty()
                .append(e.getContent())
                .append(Component.literal(String.format(" ♥ %.1f", le.getHealth())).withStyle(ChatFormatting.RED)));
        e.setResult(Event.Result.ALLOW);
    }

    // ---------- invisible players: floating head above the name ----------
    @SubscribeEvent
    public static void onLivingPost(RenderLivingEvent.Post<?, ?> e) {
        if (!Settings.invisMarkers) return;
        LivingEntity en = e.getEntity();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(en instanceof AbstractClientPlayer pl) || pl == mc.player || !pl.isInvisible()) return;
        if (mc.player.distanceTo(pl) > 64) return;

        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(0.0, pl.getBbHeight() + 0.7, 0.0);
        ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        PoseStack.Pose pose = ps.last();
        VertexConsumer vc = e.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(pl.getSkinTextureLocation()));
        headQuad(vc, pose, 0.125f, 0.25f, 0f);      // face
        headQuad(vc, pose, 0.625f, 0.75f, 0.001f);  // hat layer
        ps.popPose();
    }

    private static void headQuad(VertexConsumer vc, PoseStack.Pose pose, float u0, float u1, float z) {
        float h = 0.2f, v0 = 0.125f, v1 = 0.25f;
        int light = LightTexture.FULL_BRIGHT;
        vc.vertex(pose.pose(), -h, -h, z).color(255, 255, 255, 255).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0f, 0f, 1f).endVertex();
        vc.vertex(pose.pose(), h, -h, z).color(255, 255, 255, 255).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0f, 0f, 1f).endVertex();
        vc.vertex(pose.pose(), h, h, z).color(255, 255, 255, 255).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0f, 0f, 1f).endVertex();
        vc.vertex(pose.pose(), -h, h, z).color(255, 255, 255, 255).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), 0f, 0f, 1f).endVertex();
    }

    // ---------- nearby players list (top right) ----------
    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Post e) {
        if (e.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        Radar.render(e.getGuiGraphics(), Minecraft.getInstance(), e.getWindow().getGuiScaledWidth(), e.getWindow().getGuiScaledHeight());
        if (Freecam.isActive()) {
            Minecraft fm = Minecraft.getInstance();
            String label = "FREECAM  " + Math.round(Settings.freecamSpeed) + " b/s  (scroll wheel = speed)";
            e.getGuiGraphics().drawCenteredString(fm.font, label, e.getWindow().getGuiScaledWidth() / 2, 10, 0xFF00FF66);
        }
        if (!Settings.nearbyList) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        List<AbstractClientPlayer> list = mc.level.players().stream()
                .filter(o -> o != mc.player && mc.player.distanceTo(o) <= Settings.nearbyRange)
                .sorted(Comparator.comparingDouble(o -> mc.player.distanceTo(o)))
                .limit(8)
                .toList();
        if (list.isEmpty()) return;

        GuiGraphics g = e.getGuiGraphics();
        Font f = mc.font;
        int w = e.getWindow().getGuiScaledWidth();
        int y = 4;
        String header = "Nearby players";
        y = Radar.listY();  // sit below the radar when it is in the top right
        g.drawString(f, header, w - 4 - f.width(header), y, 0xFF00FF66, true);
        y += 10;
        for (AbstractClientPlayer o : list) {
            String s = o.getName().getString() + " " + Math.round(mc.player.distanceTo(o)) + "m";
            g.drawString(f, s, w - 4 - f.width(s), y, 0xFFFFFF, true);
            y += 10;
        }
    }
}
