package com.example.targetassist;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

public class SettingsScreen extends Screen {
    private static final int DIM = 0xB0000000, BORDER = 0xFF0B6B35, PANEL = 0xFF050805, HEADER = 0xFF08100A, SIDE = 0xFF070D09;
    private static final int ACCENT = 0xFF00FF66, TEXT = 0xFFD8FFE4, MUTED = 0xFF5E8A6E;
    private static final int OFF = 0xFF1C2A20, HOVER = 0x2000FF66, TRACK = 0xFF132018, TAB_ON = 0xFF0C1F13;
    private static final int COL_W = 142, GAP = 8, PAD = 10, TITLE_H = 26, TOP = 8, FOOT_H = 30, SIDE_W = 84;
    private static final String[] TABS = {"Aim", "Radar", "Visuals", "Freecam", "HUD", "Keys"};
    private static final String[] CORNERS = {"Top left", "Top right", "Bottom left", "Bottom right"};

    private final List<List<Item>> pages = new ArrayList<>();
    private int tab;
    private int px, py, pw, ph, doneX, doneY;
    private Item dragging;
    private KeyItem listening;

    public SettingsScreen() {
        super(Component.literal("Target Assist"));
    }

    private void add(int page, int col, Item it) {
        it.col = col;
        pages.get(page).add(it);
    }

    @Override
    protected void init() {
        pages.clear();
        for (int i = 0; i < TABS.length; i++) pages.add(new ArrayList<>());

        // ----- Aim -----
        add(0, 0, new Header("Lock-on (hold key)"));
        add(0, 0, new Toggle("Lock-on", () -> Settings.lockOn, v -> Settings.lockOn = v));
        add(0, 0, new Choice("Aim at", () -> Settings.aimHead ? "Head" : "Chest", () -> Settings.aimHead = !Settings.aimHead));
        add(0, 0, new Choice("Priority", () -> Settings.closestToMe ? "Closest to me" : "Crosshair", () -> Settings.closestToMe = !Settings.closestToMe));
        add(0, 0, new Toggle("Needs line of sight", () -> Settings.losCheck, v -> Settings.losCheck = v));
        add(0, 0, new Header("Targets"));
        add(0, 0, new Toggle("Players", () -> Settings.tPlayers, v -> Settings.tPlayers = v));
        add(0, 0, new Toggle("Zombies", () -> Settings.tZombies, v -> Settings.tZombies = v));
        add(0, 0, new Toggle("Other hostile", () -> Settings.tHostile, v -> Settings.tHostile = v));
        add(0, 0, new Toggle("Passive mobs", () -> Settings.tPassive, v -> Settings.tPassive = v));
        add(0, 1, new Header("Tuning"));
        add(0, 1, new Slide("Lock range", 5, 300, "m", () -> Settings.lockRange, v -> Settings.lockRange = v));
        add(0, 1, new Slide("Lock angle", 5, 180, " deg", () -> Settings.lockCone, v -> Settings.lockCone = v));
        add(0, 1, new Slide("Max turn speed", 30, 720, " deg/s", () -> Settings.lockSpeed, v -> Settings.lockSpeed = v));
        add(0, 1, new Slide("Smoothing", 0, 100, "%", () -> Settings.lockSmooth, v -> Settings.lockSmooth = v));
        add(0, 1, new Slide("Reaction delay", 0, 500, " ms", () -> Settings.reactionMs, v -> Settings.reactionMs = v));

        // ----- Radar -----
        add(1, 0, new Header("Radar"));
        add(1, 0, new Toggle("Radar", () -> Settings.radarOn, v -> Settings.radarOn = v));
        add(1, 0, new Toggle("Players", () -> Settings.radarPlayers, v -> Settings.radarPlayers = v));
        add(1, 0, new Toggle("Hostile mobs", () -> Settings.radarHostile, v -> Settings.radarHostile = v));
        add(1, 0, new Toggle("Passive mobs", () -> Settings.radarPassive, v -> Settings.radarPassive = v));
        add(1, 0, new Header("Style"));
        add(1, 0, new Toggle("Player heads", () -> Settings.radarHeads, v -> Settings.radarHeads = v));
        add(1, 0, new Toggle("Compass letters", () -> Settings.radarCompass, v -> Settings.radarCompass = v));
        add(1, 0, new Toggle("Range ring", () -> Settings.radarRings, v -> Settings.radarRings = v));
        add(1, 0, new Toggle("Lock-on cone", () -> Settings.radarCone, v -> Settings.radarCone = v));
        add(1, 0, new Toggle("Square shape", () -> Settings.radarSquare, v -> Settings.radarSquare = v));
        add(1, 1, new Header("Layout"));
        add(1, 1, new Choice("Position", () -> CORNERS[(int) Settings.radarCorner & 3], () -> Settings.radarCorner = ((int) Settings.radarCorner + 1) % 4));
        add(1, 1, new Slide("Radar range", 16, 300, "m", () -> Settings.radarRange, v -> Settings.radarRange = v));
        add(1, 1, new Slide("Radar size", 60, 200, " px", () -> Settings.radarSize, v -> Settings.radarSize = v));
        add(1, 1, new Slide("Opacity", 10, 100, "%", () -> Settings.radarOpacity, v -> Settings.radarOpacity = v));
        add(1, 1, new Slide("Icon size", 6, 16, " px", () -> Settings.radarIcon, v -> Settings.radarIcon = v));

        // ----- Visuals -----
        add(2, 0, new Header("World"));
        add(2, 0, new Toggle("Fullbright", () -> Settings.fullbright, v -> Settings.fullbright = v));
        add(2, 0, new Toggle("No fog", () -> Settings.noFog, v -> Settings.noFog = v));
        add(2, 0, new Toggle("No particles", () -> Settings.noParticles, v -> Settings.noParticles = v));
        add(2, 0, new Toggle("No weather", () -> Settings.noWeather, v -> Settings.noWeather = v));

        // ----- Freecam -----
        add(3, 0, new Header("Freecam"));
        add(3, 0, new Toggle("Max render distance", () -> Settings.freecamBoost, v -> Settings.freecamBoost = v));
        add(3, 0, new Slide("Fly speed", 2, 200, " b/s", () -> Settings.freecamSpeed, v -> Settings.freecamSpeed = v));
        add(3, 1, new Header("Controls"));
        add(3, 1, new Label("WASD / Space / Shift: fly"));
        add(3, 1, new Label("Sprint: fly faster"));
        add(3, 1, new Label("Scroll wheel: fly speed"));

        // ----- HUD -----
        add(4, 0, new Header("Above players"));
        add(4, 0, new Toggle("Health hearts", () -> Settings.healthIndicators, v -> Settings.healthIndicators = v));
        add(4, 0, new Toggle("Player sight (glow)", () -> Settings.playerSight, v -> Settings.playerSight = v));
        add(4, 0, new Toggle("Invisible markers", () -> Settings.invisMarkers, v -> Settings.invisMarkers = v));
        add(4, 1, new Header("Lists"));
        add(4, 1, new Toggle("Tab highlight", () -> Settings.tabHighlight, v -> Settings.tabHighlight = v));
        add(4, 1, new Toggle("Side list", () -> Settings.nearbyList, v -> Settings.nearbyList = v));
        add(4, 1, new Slide("Nearby range", 16, 256, "m", () -> Settings.nearbyRange, v -> Settings.nearbyRange = v));

        // ----- Keys -----
        add(5, 0, new Header("Keybinds"));
        add(5, 0, new KeyItem("Open menu", Keys.OPEN_GUI));
        add(5, 0, new KeyItem("Lock key", Keys.LOCK_ON));
        add(5, 0, new KeyItem("Freecam key", Keys.FREECAM));
        add(5, 0, new KeyItem("Fullbright key", Keys.FULLBRIGHT));

        // layout: stack items down each column, find the tallest page
        int maxH = TABS.length * 21;
        for (List<Item> page : pages) {
            int[] cy = {0, 0};
            for (Item it : page) {
                it.x = it.col * (COL_W + GAP);
                it.y = cy[it.col];
                it.w = COL_W;
                cy[it.col] += it.h + 1;
            }
            maxH = Math.max(maxH, Math.max(cy[0], cy[1]));
        }
        pw = SIDE_W + PAD + COL_W * 2 + GAP + PAD;
        ph = TITLE_H + TOP + maxH + FOOT_H;
        px = (width - pw) / 2;
        py = Math.max(2, (height - ph) / 2);
        int x0 = px + SIDE_W + PAD, y0 = py + TITLE_H + TOP;
        for (List<Item> page : pages) {
            for (Item it : page) {
                it.x += x0;
                it.y += y0;
            }
        }
        doneW = 80;
        doneX = px + pw - PAD - doneW;
        doneY = py + ph - FOOT_H + 6;
    }

    private int doneW;

    private List<Item> current() {
        return pages.get(tab);
    }

    private boolean overDone(double mx, double my) {
        return mx >= doneX && mx < doneX + doneW && my >= doneY && my < doneY + 18;
    }

    private int tabY(int i) {
        return py + TITLE_H + TOP + i * 21;
    }

    private int overTab(double mx, double my) {
        if (mx < px + 6 || mx >= px + SIDE_W) return -1;
        for (int i = 0; i < TABS.length; i++) {
            if (my >= tabY(i) && my < tabY(i) + 19) return i;
        }
        return -1;
    }

    private void finishListening() {
        KeyMapping.resetMapping();
        Minecraft.getInstance().options.save();
        listening = null;
    }

    // ---------- drawing ----------
    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, DIM);
        g.fill(px - 2, py - 2, px + pw + 2, py + ph + 2, 0x4000FF66);
        g.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, BORDER);
        g.fill(px, py, px + pw, py + ph, PANEL);
        g.fill(px, py, px + pw, py + TITLE_H, HEADER);
        g.fill(px, py + TITLE_H, px + pw, py + TITLE_H + 1, ACCENT);
        g.drawString(font, "TARGET ASSIST", px + PAD, py + 9, ACCENT, false);
        String hint = Keys.OPEN_GUI.getTranslatedKeyMessage().getString() + " to close";
        g.drawString(font, hint, px + pw - PAD - font.width(hint), py + 9, MUTED, false);

        // sidebar with tabs
        g.fill(px, py + TITLE_H + 1, px + SIDE_W, py + ph, SIDE);
        g.fill(px + SIDE_W, py + TITLE_H + 1, px + SIDE_W + 1, py + ph, BORDER);
        int hoverTab = overTab(mx, my);
        for (int i = 0; i < TABS.length; i++) {
            int ty = tabY(i);
            boolean on = i == tab;
            if (on) {
                g.fill(px + 6, ty, px + SIDE_W - 2, ty + 19, TAB_ON);
                g.fill(px + 6, ty, px + 8, ty + 19, ACCENT);
            } else if (i == hoverTab) {
                g.fill(px + 6, ty, px + SIDE_W - 2, ty + 19, HOVER);
            }
            g.drawString(font, TABS[i], px + 14, ty + 6, on ? ACCENT : TEXT, false);
        }

        for (Item it : current()) it.render(g, mx, my);

        boolean over = overDone(mx, my);
        g.fill(doneX, doneY, doneX + doneW, doneY + 18, over ? 0xFF66FF99 : ACCENT);
        g.drawCenteredString(font, "Done", doneX + doneW / 2, doneY + 5, 0xFF050805);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        Settings.save();
    }

    // ---------- input ----------
    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (listening != null) {
            if (key != GLFW.GLFW_KEY_ESCAPE) listening.km.setKey(InputConstants.getKey(key, scan));
            finishListening();
            return true;
        }
        if (Keys.OPEN_GUI.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (listening != null) {
            listening.km.setKey(InputConstants.Type.MOUSE.getOrCreate(button));
            finishListening();
            return true;
        }
        if (button == 0) {
            if (overDone(mx, my)) {
                onClose();
                return true;
            }
            int t = overTab(mx, my);
            if (t >= 0) {
                tab = t;
                dragging = null;
                return true;
            }
            for (Item it : current()) {
                if (it.hit(mx, my)) {
                    it.click(mx, my);
                    if (it instanceof Slide) dragging = it;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null) {
            dragging.drag(mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        return super.mouseReleased(mx, my, button);
    }

    // ---------- custom widgets ----------
    private abstract class Item {
        int x, y, w, h = 15, col;
        final String label;

        Item(String label) {
            this.label = label;
        }

        boolean hit(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }

        void hover(GuiGraphics g, int mx, int my) {
            if (hit(mx, my)) g.fill(x, y, x + w, y + h, HOVER);
        }

        abstract void render(GuiGraphics g, int mx, int my);

        void click(double mx, double my) {
        }

        void drag(double mx) {
        }
    }

    private class Header extends Item {
        Header(String label) {
            super(label);
            h = 17;
        }

        @Override
        void render(GuiGraphics g, int mx, int my) {
            g.drawString(font, label.toUpperCase(), x, y + 3, ACCENT, false);
            g.fill(x, y + 13, x + w, y + 14, BORDER);
        }
    }

    private class Label extends Item {
        Label(String label) {
            super(label);
        }

        @Override
        void render(GuiGraphics g, int mx, int my) {
            g.drawString(font, label, x + 4, y + 3, MUTED, false);
        }

        @Override
        boolean hit(double mx, double my) {
            return false;
        }
    }

    private class Toggle extends Item {
        final BooleanSupplier getter;
        final Consumer<Boolean> setter;

        Toggle(String label, BooleanSupplier getter, Consumer<Boolean> setter) {
            super(label);
            this.getter = getter;
            this.setter = setter;
        }

        @Override
        void render(GuiGraphics g, int mx, int my) {
            hover(g, mx, my);
            g.drawString(font, label, x + 4, y + 3, TEXT, false);
            boolean on = getter.getAsBoolean();
            int sx = x + w - 28, sy = y + 3;
            g.fill(sx, sy, sx + 24, sy + 9, on ? ACCENT : OFF);
            int kx = on ? sx + 16 : sx + 1;
            g.fill(kx, sy + 1, kx + 7, sy + 8, on ? 0xFF050805 : 0xFFB8C7BD);
        }

        @Override
        void click(double mx, double my) {
            setter.accept(!getter.getAsBoolean());
        }
    }

    private class Choice extends Item {
        final Supplier<String> value;
        final Runnable action;

        Choice(String label, Supplier<String> value, Runnable action) {
            super(label);
            this.value = value;
            this.action = action;
        }

        @Override
        void render(GuiGraphics g, int mx, int my) {
            hover(g, mx, my);
            g.drawString(font, label, x + 4, y + 3, TEXT, false);
            String v = value.get();
            int bw = font.width(v) + 12;
            int bx = x + w - bw - 2;
            g.fill(bx, y + 1, bx + bw, y + h - 1, TRACK);
            g.drawString(font, v, bx + 6, y + 3, ACCENT, false);
        }

        @Override
        void click(double mx, double my) {
            action.run();
        }
    }

    private class Slide extends Item {
        final double min, max;
        final String unit;
        final DoubleSupplier getter;
        final DoubleConsumer setter;

        Slide(String label, double min, double max, String unit, DoubleSupplier getter, DoubleConsumer setter) {
            super(label);
            this.min = min;
            this.max = max;
            this.unit = unit;
            this.getter = getter;
            this.setter = setter;
            h = 24;
        }

        @Override
        void render(GuiGraphics g, int mx, int my) {
            hover(g, mx, my);
            g.drawString(font, label, x + 4, y + 2, TEXT, false);
            String v = Math.round(getter.getAsDouble()) + unit;
            g.drawString(font, v, x + w - 4 - font.width(v), y + 2, ACCENT, false);
            int tx = x + 4, tw = w - 8, ty = y + 16;
            double f = Mth.clamp((getter.getAsDouble() - min) / (max - min), 0.0, 1.0);
            int kx = tx + (int) (tw * f);
            g.fill(tx, ty, tx + tw, ty + 3, TRACK);
            g.fill(tx, ty, kx, ty + 3, ACCENT);
            g.fill(kx - 2, ty - 3, kx + 2, ty + 6, 0xFFFFFFFF);
        }

        @Override
        void click(double mx, double my) {
            drag(mx);
        }

        @Override
        void drag(double mx) {
            double f = Mth.clamp((mx - (x + 4)) / (w - 8), 0.0, 1.0);
            setter.accept(Math.round(min + f * (max - min)));
        }
    }

    private class KeyItem extends Item {
        final KeyMapping km;

        KeyItem(String label, KeyMapping km) {
            super(label);
            this.km = km;
        }

        @Override
        void render(GuiGraphics g, int mx, int my) {
            hover(g, mx, my);
            g.drawString(font, label, x + 4, y + 3, TEXT, false);
            boolean wait = listening == this;
            String v = wait ? "press a key..." : km.getTranslatedKeyMessage().getString();
            int bw = Math.max(44, font.width(v) + 12);
            int bx = x + w - bw - 2;
            g.fill(bx - 1, y, bx + bw + 1, y + h, wait ? ACCENT : BORDER);
            g.fill(bx, y + 1, bx + bw, y + h - 1, TRACK);
            g.drawString(font, v, bx + (bw - font.width(v)) / 2, y + 3, wait ? ACCENT : TEXT, false);
        }

        @Override
        void click(double mx, double my) {
            listening = this;
        }
    }
}
