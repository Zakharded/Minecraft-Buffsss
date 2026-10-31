package com.example.targetassist;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class Settings {
    // display / HUD
    public static boolean healthIndicators = true, playerSight = true, invisMarkers = true, tabHighlight = true, nearbyList = false;
    public static double nearbyRange = 64;

    // aim assist
    public static boolean lockOn = true, aimHead = false, closestToMe = false, losCheck = true;
    public static boolean tPlayers = true, tZombies = true, tHostile = false, tPassive = false;
    public static double lockRange = 100, lockCone = 60, lockSpeed = 180, lockSmooth = 50, reactionMs = 150;

    // radar
    public static boolean radarOn = true, radarPlayers = true, radarHostile = true, radarPassive = false;
    public static boolean radarHeads = true, radarCompass = true, radarRings = true, radarCone = true, radarSquare = false;
    public static double radarRange = 64, radarSize = 110, radarOpacity = 75, radarIcon = 8, radarCorner = 1;

    // visuals + freecam
    public static boolean noFog = false, fullbright = false, noParticles = false, noWeather = false, freecamBoost = true;
    public static double freecamSpeed = 20;

    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("targetassist.properties");

    public static void load() {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(FILE)) {
            p.load(r);
        } catch (IOException e) {
            return;
        }
        sync(p, false);
    }

    public static void save() {
        Properties p = new Properties();
        sync(p, true);
        try (Writer w = Files.newBufferedWriter(FILE)) {
            p.store(w, "Target Assist");
        } catch (IOException ignored) {
        }
    }

    private static void sync(Properties p, boolean saving) {
        for (Field f : Settings.class.getFields()) {
            String n = f.getName();
            try {
                if (saving) {
                    p.setProperty(n, String.valueOf(f.get(null)));
                } else if (p.containsKey(n)) {
                    if (f.getType() == boolean.class) f.setBoolean(null, Boolean.parseBoolean(p.getProperty(n)));
                    else if (f.getType() == double.class) f.setDouble(null, Double.parseDouble(p.getProperty(n)));
                }
            } catch (Exception ignored) {
            }
        }
    }
}
