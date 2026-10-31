package com.example.targetassist;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

public final class Radar {
    private static final int MARGIN = 6;
    private static final int ACCENT = 0xFF00FF66, EDGE = 0xFF0B6B35, TEXT = 0xFFD8FFE4;

    // 7 x 7 skull, # = filled
    private static final String[] SKULL = {
            ".#####.",
            "#######",
            "##.#.##",
            "##.#.##",
            ".#####.",
            "..#.#..",
            "..###.."
    };

    // y where the nearby-players list should start so it sits under the radar
    public static int listY() {
        return Settings.radarOn && (int) Settings.radarCorner == 1 ? (int) Settings.radarSize + 14 : 4;
    }

    public static void render(GuiGraphics g, Minecraft mc, int screenW, int screenH) {
        if (!Settings.radarOn || mc.player == null || mc.level == null) return;
        Player me = mc.player;
        int size = (int) Settings.radarSize;
        int r = size / 2;
        int corner = (int) Settings.radarCorner;
        int cx = (corner == 0 || corner == 2) ? MARGIN + r : screenW - MARGIN - r;
        int cy = corner < 2 ? MARGIN + r : screenH - MARGIN - r;
        double range = Settings.radarRange;
        boolean square = Settings.radarSquare;
        int alpha = (int) (Math.max(10.0, Math.min(100.0, Settings.radarOpacity)) / 100.0 * 255.0);
        int bg = (alpha << 24) | 0x050805;

        // background + border
        if (square) {
            g.fill(cx - r, cy - r, cx + r + 1, cy + r + 1, EDGE);
            g.fill(cx - r + 1, cy - r + 1, cx + r, cy + r, bg);
        } else {
            disc(g, cx, cy, r - 1, bg);
            ring(g, cx, cy, r, EDGE);
        }

        // faint range rings and cross hair
        if (Settings.radarRings) {
            if (!square) ring(g, cx, cy, r / 2, 0x4000FF66);
            else g.fill(cx - r / 2, cy - r / 2, cx + r / 2 + 1, cy + r / 2 + 1, 0x1800FF66);
        }
        g.fill(cx - r + 2, cy, cx + r - 1, cy + 1, 0x2800FF66);
        g.fill(cx, cy - r + 2, cx + 1, cy + r - 1, 0x2800FF66);

        // compass letters, north is always up
        if (Settings.radarCompass) {
            g.drawCenteredString(mc.font, "N", cx, cy - r + 3, ACCENT);
            g.drawCenteredString(mc.font, "S", cx, cy + r - 11, TEXT);
            g.drawCenteredString(mc.font, "W", cx - r + 7, cy - 4, TEXT);
            g.drawCenteredString(mc.font, "E", cx + r - 6, cy - 4, TEXT);
        }

        // lock-on cone and facing line, drawn from the middle outwards
        double yaw = Math.toRadians(me.getYRot());
        if (Settings.radarCone && Settings.lockOn) {
            double half = Math.toRadians(Math.min(Settings.lockCone, 90.0));
            for (int side = -1; side <= 1; side += 2) {
                double a = yaw + side * half;
                for (int i = 4; i < r - 3; i += 3) {
                    int fx = cx + (int) Math.round(-Math.sin(a) * i);
                    int fy = cy + (int) Math.round(Math.cos(a) * i);
                    g.fill(fx, fy, fx + 1, fy + 1, 0x6000FF66);
                }
            }
        }
        for (int i = 1; i < r - 4; i++) {
            int fx = cx + (int) Math.round(-Math.sin(yaw) * i);
            int fy = cy + (int) Math.round(Math.cos(yaw) * i);
            g.fill(fx, fy, fx + 1, fy + 1, 0xB0FFFFFF);
        }

        // mobs first, players on top of them
        int icon = Math.max(6, (int) Settings.radarIcon);
        int half = icon / 2;
        int reach = r - half - 3;
        for (int pass = 0; pass < 2; pass++) {
            for (Entity en : mc.level.entitiesForRendering()) {
                if (en == me || !(en instanceof LivingEntity le) || !le.isAlive() || le.isSpectator()) continue;
                boolean isPlayer = en instanceof Player;
                if (isPlayer != (pass == 1)) continue;

                int color;
                if (isPlayer) {
                    if (!Settings.radarPlayers) continue;
                    color = en.isInvisible() ? 0xFFFF55FF : 0xFFFF4444;
                } else if (en instanceof Enemy) {
                    if (!Settings.radarHostile) continue;
                    color = 0xFFFF8A1F;
                } else if (en instanceof Mob) {
                    if (!Settings.radarPassive) continue;
                    color = 0xFF9DFFB0;
                } else {
                    continue;
                }

                double dx = en.getX() - me.getX(), dz = en.getZ() - me.getZ();
                double dist = square ? Math.max(Math.abs(dx), Math.abs(dz)) : Math.sqrt(dx * dx + dz * dz);
                if (dist > range) continue;
                int ex = cx + (int) Math.round(dx / range * reach);
                int ey = cy + (int) Math.round(dz / range * reach);

                if (isPlayer && Settings.radarHeads && en instanceof AbstractClientPlayer ap) {
                    g.fill(ex - half - 1, ey - half - 1, ex - half + icon + 1, ey - half + icon + 1, color);
                    RenderSystem.enableBlend();
                    g.blit(ap.getSkinTextureLocation(), ex - half, ey - half, icon, icon, 8f, 8f, 8, 8, 64, 64);
                    g.blit(ap.getSkinTextureLocation(), ex - half, ey - half, icon, icon, 40f, 8f, 8, 8, 64, 64);
                } else if (isPlayer) {
                    g.fill(ex - 2, ey - 2, ex + 3, ey + 3, color);
                } else {
                    skull(g, ex, ey, icon, color);
                }
            }
        }

        // you
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFFFFFFFF);
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, ACCENT);
    }

    private static void skull(GuiGraphics g, int mx, int my, int icon, int color) {
        int s = Math.max(1, icon / 7);
        int w = 7 * s;
        int x0 = mx - w / 2, y0 = my - w / 2;
        for (int row = 0; row < 7; row++) {
            for (int col = 0; col < 7; col++) {
                if (SKULL[row].charAt(col) == '#') {
                    g.fill(x0 + col * s, y0 + row * s, x0 + (col + 1) * s, y0 + (row + 1) * s, color);
                }
            }
        }
    }

    private static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    // a one pixel thick circle outline
    private static void ring(GuiGraphics g, int cx, int cy, int r, int color) {
        int ri = r - 1;
        for (int dy = -r; dy <= r; dy++) {
            int outer = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            if (Math.abs(dy) > ri) {
                g.fill(cx - outer, cy + dy, cx + outer + 1, cy + dy + 1, color);
            } else {
                int inner = (int) Math.round(Math.sqrt((double) ri * ri - (double) dy * dy));
                if (inner < outer) {
                    g.fill(cx - outer, cy + dy, cx - inner, cy + dy + 1, color);
                    g.fill(cx + inner + 1, cy + dy, cx + outer + 1, cy + dy + 1, color);
                }
            }
        }
    }
}
