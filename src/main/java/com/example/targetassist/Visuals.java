package com.example.targetassist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderBlockScreenEffectEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;

// Client-only visual options: fullbright, no fog, no particles, no weather.
@Mod.EventBusSubscriber(modid = TargetAssistMod.MODID, value = Dist.CLIENT)
public class Visuals {
    private static boolean nvApplied, gammaApplied, gammaTried;
    private static double oldGamma = 0.5;
    private static Field gammaField;

    // The gamma slider normally stops at 100%. Writing the value directly lets it go far past that,
    // which pushes every light level to full brightness.
    private static void setGamma(Minecraft mc, double v) {
        try {
            if (gammaField == null && !gammaTried) {
                gammaTried = true;
                gammaField = ObfuscationReflectionHelper.findField(OptionInstance.class, "value");
            }
            if (gammaField != null) gammaField.set(mc.options.gamma(), v);
        } catch (Exception ex) {
            gammaField = null;  // if this ever fails, night vision below still gives a bright view
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            nvApplied = false;
            if (gammaApplied) {
                setGamma(mc, oldGamma);  // do not leave the huge value behind when you leave the world
                gammaApplied = false;
            }
            return;
        }
        while (Keys.FULLBRIGHT.consumeClick()) {
            if (mc.screen == null) {
                Settings.fullbright = !Settings.fullbright;
                Settings.save();
            }
        }

        if (Settings.fullbright) {
            if (!gammaApplied) {
                oldGamma = mc.options.gamma().get();
                gammaApplied = true;
            }
            setGamma(mc, 1000.0);

            // plus a night vision effect that only exists on your own client
            MobEffectInstance cur = mc.player.getEffect(MobEffects.NIGHT_VISION);
            if (cur == null || (nvApplied && cur.getDuration() < 400)) {
                mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 100000, 0, false, false, false));
                nvApplied = true;
            }
        } else {
            if (gammaApplied) {
                setGamma(mc, oldGamma);
                gammaApplied = false;
            }
            if (nvApplied) {
                mc.player.removeEffectNoUpdate(MobEffects.NIGHT_VISION);
                nvApplied = false;
            }
        }

        // no particles: wipe them every tick, so none are ever drawn
        if (Settings.noParticles) mc.particleEngine.setLevel(mc.level);
    }

    // no weather: rain, snow, storm darkening and thunder are all driven by these two values
    @SubscribeEvent
    public static void onRender(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START || !Settings.noWeather) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        mc.level.setRainLevel(0f);
        mc.level.setThunderLevel(0f);
    }

    // no fog: pushes the fog start and end so far away that it never shows (covers lava, water, blindness)
    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog e) {
        if (!Settings.noFog) return;
        e.setNearPlaneDistance(1.0E6F);
        e.setFarPlaneDistance(2.0E6F);
        e.setCanceled(true);
    }

    // no fog also hides the full-screen water and fire overlays
    @SubscribeEvent
    public static void onScreenEffect(RenderBlockScreenEffectEvent e) {
        if (!Settings.noFog) return;
        RenderBlockScreenEffectEvent.OverlayType t = e.getOverlayType();
        if (t == RenderBlockScreenEffectEvent.OverlayType.WATER || t == RenderBlockScreenEffectEvent.OverlayType.FIRE) {
            e.setCanceled(true);
        }
    }
}
