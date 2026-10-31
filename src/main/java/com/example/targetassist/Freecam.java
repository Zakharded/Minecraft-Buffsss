package com.example.targetassist;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

// Client-only freecam: a local fake entity is used as the camera. Nothing is sent to the server,
// and your real player stands still (no movement, no attacking or using items) while it is on.
// A client can only draw chunks the server has sent it, so how far you can see is limited by the
// server's view distance. This mod raises your own render distance to the max while freecam is on.
@Mod.EventBusSubscriber(modid = TargetAssistMod.MODID, value = Dist.CLIENT)
public class Freecam {
    private static boolean active;
    private static RemotePlayer cam;
    private static double x, y, z, vx, vy, vz;
    private static float camYaw, camPitch, frozenYaw, frozenPitch;
    private static CameraType oldView;
    private static int oldRender;
    private static boolean oldCull, boosted;
    private static long lastNanos;

    public static boolean isActive() {
        return active;
    }

    private static void start(Minecraft mc) {
        LocalPlayer p = mc.player;
        cam = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "freecam"));
        x = p.getX();
        y = p.getY() + p.getEyeHeight();
        z = p.getZ();
        vx = vy = vz = 0;
        camYaw = frozenYaw = p.getYRot();
        camPitch = frozenPitch = p.getXRot();
        cam.setPos(x, y - cam.getEyeHeight(), z);
        cam.xo = x;
        cam.yo = y - cam.getEyeHeight();
        cam.zo = z;
        oldView = mc.options.getCameraType();
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.setCameraEntity(cam);

        // see as far as possible, and never hide chunks just because the camera is inside a block
        oldCull = mc.smartCull;
        mc.smartCull = false;
        if (Settings.freecamBoost) {
            oldRender = mc.options.renderDistance().get();
            mc.options.renderDistance().set(32);
            mc.options.save();  // also tells the server how far you want to see
            boosted = true;
        }
        lastNanos = System.nanoTime();
        active = true;
    }

    private static void stop(Minecraft mc) {
        active = false;
        cam = null;
        if (mc.player != null) mc.setCameraEntity(mc.player);
        if (oldView != null) mc.options.setCameraType(oldView);
        mc.smartCull = oldCull;
        if (boosted) {
            mc.options.renderDistance().set(oldRender);
            mc.options.save();
            boosted = false;
        }
    }

    // ---------- per tick: toggle key, block attacking/using ----------
    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (e.phase == TickEvent.Phase.START) {
            if (active) {
                mc.options.keyAttack.setDown(false);
                mc.options.keyUse.setDown(false);
                Keys.LOCK_ON.setDown(false);
            }
            return;
        }
        if (mc.player == null || mc.level == null) {
            if (active) stop(mc);
            return;
        }
        while (Keys.FREECAM.consumeClick()) {
            if (mc.screen != null) continue;
            if (active) stop(mc);
            else start(mc);
        }
        // respawn or dimension change resets the camera, so drop out of freecam
        if (active && mc.getCameraEntity() != cam) stop(mc);
    }

    // ---------- per frame: fly the camera ----------
    @SubscribeEvent
    public static void onRender(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START || !active) return;
        long now = System.nanoTime();
        double dt = Math.min((now - lastNanos) / 1e9, 0.1);
        lastNanos = now;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || cam == null) return;
        Keys.LOCK_ON.setDown(false);

        double fwd = 0, side = 0, vert = 0;
        if (mc.screen == null) {
            fwd = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
            side = (mc.options.keyRight.isDown() ? 1 : 0) - (mc.options.keyLeft.isDown() ? 1 : 0);
            vert = (mc.options.keyJump.isDown() ? 1 : 0) - (mc.options.keyShift.isDown() ? 1 : 0);
        }
        double speed = Settings.freecamSpeed * (mc.options.keySprint.isDown() ? 2.5 : 1.0);
        double yaw = Math.toRadians(camYaw);

        // W/S move flat along the ground, A/D strafe, Space goes up, Shift goes down (looking up or down changes nothing)
        double tx = (fwd * -Math.sin(yaw) + side * -Math.cos(yaw)) * speed;
        double ty = vert * speed;
        double tz = (fwd * Math.cos(yaw) + side * -Math.sin(yaw)) * speed;

        // ease towards the wanted velocity so starting and stopping feels smooth
        double blend = 1.0 - Math.exp(-14.0 * dt);
        vx += (tx - vx) * blend;
        vy += (ty - vy) * blend;
        vz += (tz - vz) * blend;
        x += vx * dt;
        y += vy * dt;
        z += vz * dt;

        double feet = y - cam.getEyeHeight();
        cam.setPos(x, feet, z);
        cam.xo = x;
        cam.yo = feet;
        cam.zo = z;
    }

    // mouse movement turns the real player, so move that turn over to the camera and put the player back
    @SubscribeEvent
    public static void onAngles(ViewportEvent.ComputeCameraAngles e) {
        if (!active) return;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        camYaw += Mth.wrapDegrees(p.getYRot() - frozenYaw);
        camPitch = Mth.clamp(camPitch + (p.getXRot() - frozenPitch), -90f, 90f);
        p.setYRot(frozenYaw);
        p.setXRot(frozenPitch);
        p.yRotO = frozenYaw;
        p.xRotO = frozenPitch;
        e.setYaw(camYaw);
        e.setPitch(camPitch);
    }

    // ---------- scroll wheel changes the fly speed ----------
    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent e) {
        if (!active || Minecraft.getInstance().screen != null) return;
        double f = e.getScrollDelta() > 0 ? 1.2 : 1.0 / 1.2;
        Settings.freecamSpeed = Mth.clamp(Settings.freecamSpeed * f, 2.0, 200.0);
        e.setCanceled(true);
    }

    // ---------- freeze the real player ----------
    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent e) {
        if (!active) return;
        Input in = e.getInput();
        in.up = false;
        in.down = false;
        in.left = false;
        in.right = false;
        in.jumping = false;
        in.shiftKeyDown = false;
        in.forwardImpulse = 0;
        in.leftImpulse = 0;
    }

    @SubscribeEvent
    public static void onClick(InputEvent.InteractionKeyMappingTriggered e) {
        if (!active) return;
        e.setCanceled(true);
        e.setSwingHand(false);
    }

    @SubscribeEvent
    public static void onHand(RenderHandEvent e) {
        if (active) e.setCanceled(true);  // hide the first-person hand
    }
}
