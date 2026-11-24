package dev.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class HudWidgets {
    public static final List<HudWidget> ALL = new ArrayList<>();

    static {
        ALL.add(new Watermark());
        ALL.add(new Fps());
        ALL.add(new Ping());
        ALL.add(new Coords());
        ALL.add(new Speed());
        ALL.add(new Cps());
        ALL.add(new Clock());
        ALL.add(new Ram());
        ALL.add(new Status());
        ALL.add(new Armor());
        ALL.add(new Inventory());
        ALL.add(new Effects());
        ALL.add(new Keys());
        ALL.add(new Target());
    }

    public static void resetPositions() {
        for (HudWidget w : ALL) {
            VisualsConfig.WidgetState s = VisualsConfig.I.state(w);
            s.x = w.defX; s.y = w.defY; s.on = w.defOn;
        }
    }

    private static int hpColor(float r) { return r > 0.6f ? 0xFF55FF7A : r > 0.3f ? 0xFFFFD74D : 0xFFFF5555; }

    // ------------------------------------------------------------------ CPS
    public static final class ClickTracker {
        private static final ArrayDeque<Long> L = new ArrayDeque<>(), Rt = new ArrayDeque<>();
        private static boolean pl, pr;

        public static void poll(MinecraftClient mc) {
            long now = System.currentTimeMillis();
            boolean l = mc.currentScreen == null && mc.options.attackKey.isPressed();
            boolean r = mc.currentScreen == null && mc.options.useKey.isPressed();
            if (l && !pl) L.add(now);
            if (r && !pr) Rt.add(now);
            pl = l; pr = r;
            while (!L.isEmpty() && now - L.peekFirst() > 1000) L.pollFirst();
            while (!Rt.isEmpty() && now - Rt.peekFirst() > 1000) Rt.pollFirst();
        }
        public static int left() { return L.size(); }
        public static int right() { return Rt.size(); }
    }

    // ------------------------------------------------------------ простые
    static final class Watermark extends HudWidget {
        Watermark() { super("watermark", "Ватермарк", 0.01f, 0.01f, false); }
        @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y, boolean editor) {
            TextRenderer tr = mc.textRenderer;
            String name = mc.player == null ? "player" : mc.player.getName().getString();
            String t1 = "Visuals", t2 = "  |  " + name;
            w = tr.getWidth(t1) + tr.getWidth(t2) + 10; h = 15;
            panel(c, x, y, w, h);
            Ui.gradientText(c, tr, t1, x + 5, y + 4);
            Ui.text(c, tr, t2, x + 5 + tr.getWidth(t1), y + 4, 0xFFAAAAAA);
        }
    }

    static final class Fps extends HudWidget {
        Fps() { super("fps", "FPS", 0.01f, 0.02f, true); }
        @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y, boolean editor) {
            int f = mc.getCurrentFps();
            one(c, mc, x, y, "FPS", String.valueOf(f), f >= 60 ? good() : f >= 30 ? warn() : bad());
        }
    }

    static final class Ping extends HudWidget {
        Ping() { super("ping", "Пинг", 0.01f, 0.075f, true); }
        @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y, boolean editor) {
            int ping = 0;
            if (mc.player != null && mc.getNetworkHandler() != null) {
                PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
                if (e != null) ping = e.getLatency();
            }
            one(c, mc, x, y, "Пинг", ping + " мс", ping < 80 ? good() : ping < 160 ? warn() : bad());
        }
    }

    static final class Coords extends HudWidget {
        Coords() { super("coords", "Координаты", 0.01f, 0.13f, true); }
        @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y, boolean editor) {
            ClientPlayerEntity p = mc.player;
            if (p == null) return;
            int bx = p.getBlockX(), by = p.getBlockY(), bz = p.getBlockZ();
            String face = String.valueOf(p.getHorizontalFacing().name().charAt(0));
            String l2 = null, v2 = null;
            if (mc.world != null) {
                if (mc.world.getRegistryKey() == World.OVERWORLD) { l2 = "Незер"; v2 = (bx / 8) + " " + (bz / 8); }
                else if (mc.world.getRegistryKey() == World.NETHER) { l2 = "Верх"; v2 = (bx * 8) + " " + (bz * 8); }
            }
            String main = bx + " " + by + " " + bz + "  " + face;
            if (l2 == null) one(c, mc, x, y, "XYZ", main, 0xFFFFFFFF);
            else rows(c, mc, x, y, new String[]{"XYZ", l2}, new String[]{main, v2}, new int[]{0xFFFFFFFF, 0xFFBBBBBB});
        }
    }

    static final class Speed extends HudWidget {
        double lx, lz; long ln; float v;
        Speed() { super("speed", "Скорость", 0.01f, 0.185f, false); }
        @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y, boolean editor) {
            ClientPlayerEntity p = mc.player;
            if (p == null) return;
            long now = System.nanoTime();
            if (ln == 0) { ln = now; lx = p.getX(); lz = p.getZ(); }
            float dt = (now - ln) / 1e9f;
            if (dt >= 0.2f) {
                float cur = (float) (Math.hypot(p.getX() - lx, p.getZ() - lz) / dt);
                v += (cur - v) * 0.5f;
                lx = p.getX(); lz = p.getZ(); ln = now;
            }
            one(c, mc, x, y, "Скорость", String.format("%.1f б/с", v), 0xFFFFFFFF);
        }
    }

    static final class Cps extends HudWidget {
        Cps() { super("cps", "CPS", 0.01f, 0.24f, false); }
        @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y, boolean editor) {
            one(c, mc, x, y, "CPS", ClickTracker.left() + " | " + ClickTracker.right(), 0xFFFFFFFF);
        }
    }

    static final class Clock extends HudWidget {
        static final DateTimeFormatter F = DateTimeFormatter.ofPattern("HH:mm:ss");
        Clock() { super("clock", "Время", 0.9f, 0.02f, false); }
        @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y, boolean editor) {
            one(c, mc, x, y, "Время", LocalTime.now().format(F), 0xFFFFFFFF);
        }
    }

    static final class Ram extends HudWidget {
        Ram() { super("ram", "Память", 0.9f, 0.075f, false); }
        @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y, boolean editor) {
            Runtime r = Runtime.getRuntime();
            long used = (r.totalMemory() - r.freeMemory()) >> 20, max = r.maxMemory() >> 20;
            float ratio = (float) used / max;
            one(c, mc, x, y, "RAM", used + "/" + max + " МБ", ratio < 0.7f ? good() : ratio < 0.9f ? warn() : bad());
        }
    }

    // ---------------------------------------------------------------- статус
    static final class Status extends HudWidget {
        float hp = -1, hunger = -1; long ln;
        Status() { super("status", "Здоровье и голод", 0.01f, 0.88f, true); }
        @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y, boolean editor) {
            ClientPlayerEntity p = mc.player;
            if (p == null) return;
            TextRenderer tr = mc.textRenderer;
            long now = System.nanoTime();
            float dt = ln == 0 ? 0 : (now - ln) / 1e9f; ln = now;
            float k = 1f - (float) Math.exp(-dt * 8f);
            float curHp = p.getHealth(), max = Math.max(1f, p.getMaxHealth());
            float curFood = p.getHungerManager().getFoodLevel();
            hp = hp < 0 || !VisualsConfig.I.smoothPvp ? curHp : hp + (curHp - hp) * k;
            hunger = hunger < 0 ? curFood : hunger + (curFood - hunger) * k;

            String foodLine = null;
            FoodComponent food = p.getMainHandStack().get(DataComponentTypes.FOOD);
            if (food != null) foodLine = String.format("+%d голода, +%.1f насыщ.", food.nutrition(), food.saturation());

            int bw = 104;
            w = bw + 8;
            h = 9 * 2 + 3 + 8 + (foodLine != null ? 11 : 0);
            if (foodLine != null) w = Math.max(w, tr.getWidth(foodLine) + 10);
            panel(c, x, y, w, h);

            int bx = x + 4, by = y + 4;
            bar(c, bx, by, bw, 9, hp / max, hpColor(curHp / max));
            String t = String.format("HP %.1f / %.0f", curHp, max);
            float abs = p.getAbsorptionAmount();
            if (abs > 0) t += String.format(" (+%.1f)", abs);
            Ui.text(c, tr, t, bx + 3, by + 1, 0xFFFFFFFF);

            by += 12;
            bar(c, bx, by, bw, 9, hunger / 20f, 0xFFD9A441);
            Ui.text(c, tr, "Голод " + (int) curFood + "/20", bx + 3, by + 1, 0xFFFFFFFF);

            if (foodLine != null) Ui.text(c, tr, foodLine, bx, by + 12, 0xFFFFD27F);
        }
    }

    // ----------------------------------------------------------------- броня
    static final class Armor extends HudWidget {
        Armor() { super("armor", "Броня", 0.93f, 0.3f, true); }
        @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y, boolean editor) {
            ClientPlayerEntity p = mc.player;
            if (p == null) return;
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            boolean any = false;
            for (EquipmentSlot s : slots) if (!p.getEquippedStack(s).isEmpty()) any = true;
            if (!any && !editor) { w = h = 0; return; }
            TextRenderer tr = mc.textRenderer;
            w = 62; h = 4 * 19 + 4;
            panel(c, x, y, w, h);
            int yy = y + 3;
            for (EquipmentSlot s : slots) {
                ItemStack st = p.getEquippedStack(s);
                if (st.isEmpty()) {
                    Ui.outline(c, x + 4, yy + 1, 16, 16, 0x30FFFFFF);
                    Ui.text(c, tr, "-", x + 26, yy + 5, 0xFF666666);
                } else {
                    c.drawItem(st, x + 4, yy + 1);
                    int max = st.getMaxDamage();
                    if (max > 0) {
                        float r = (float) (max - st.getDamage()) / max;
                        Ui.text(c, tr, (int) (r * 100) + "%", x + 24, yy + 2, hpColor(r));
                        bar(c, x + 24, yy + 13, 32, 3, r, hpColor(r));
                    } else {
                        Ui.text(c, tr, "∞", x + 26, yy + 5, 0xFFAAAAAA);
                    }
                }
                yy += 19;
            }
        }
    }

    // -------------------------------------------------------------- инвентарь
    static final class Inventory extends HudWidget {
        Inventory() { super("inventory", "Инвентарь", 0.78f, 0.62f, true); }
        @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y, boolean editor) {
            ClientPlayerEntity p = mc.player;
            if (p == null) return;
            TextRenderer tr = mc.textRenderer;
            int cell = 18;
            w = 9 * cell + 6; h = 3 * cell + 6 + 11;
            panel(c, x, y, w, h);
            Ui.text(c, tr, "Инвентарь", x + 5, y + 3, Ui.accent(0));
            for (int r = 0; r < 3; r++) {
                for (int col = 0; col < 9; col++) {
                    int sx = x + 3 + col * cell, sy = y + 14 + r * cell;
                    Ui.round(c, sx, sy, cell - 1, cell - 1, 2, 0x28FFFFFF);
                    ItemStack st = p.getInventory().getStack(9 + r * 9 + col);
                    if (!st.isEmpty()) {
                        c.drawItem(st, sx + 1, sy + 1);
                        c.drawStackOverlay(tr, st, sx + 1, sy + 1);
                    }
                }
            }
        }
    }

    // --------------------------------------------------------------- эффекты
    static final class Effects extends HudWidget {
        Effects() { super("effects", "Эффекты зелий", 0.01f, 0.5f, true); }

        private static String roman(int n) {
            String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
            return n >= 0 && n < r.length ? r[n] : String.valueOf(n);
        }

        @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y, boolean editor) {
            ClientPlayerEntity p = mc.player;
            if (p == null) return;
            TextRenderer tr = mc.textRenderer;
            List<String> names = new ArrayList<>(), times = new ArrayList<>();
            List<Integer> cols = new ArrayList<>();
            for (StatusEffectInstance e : p.getStatusEffects()) {
                if (names.size() >= 8) break;
                var type = e.getEffectType().value();
                VisualsConfig vc = VisualsConfig.I;
                names.add(type.getName().getString() + " " + roman(e.getAmplifier() + 1));
                int sec = e.getDuration() / 20;
                times.add(e.isInfinite() ? "∞" : sec / 60 + ":" + String.format("%02d", sec % 60));
                cols.add(type.getCategory() == StatusEffectCategory.HARMFUL ? 0xFFFF7777 : 0xFF8CFFA8);
            }
            if (names.isEmpty()) {
                if (!editor) { w = h = 0; return; }
                names.add("Скорость II"); times.add("1:23"); cols.add(0xFF8CFFA8);
                names.add("Слабость"); times.add("0:42"); cols.add(0xFFFF7777);
            }
            int nw = 0, tw = 0;
            for (int i = 0; i < names.size(); i++) { nw = Math.max(nw, tr.getWidth(names.get(i))); tw = Math.max(tw, tr.getWidth(times.get(i))); }
            w = nw + tw + 18; h = names.size() * 11 + 6;
            panel(c, x, y, w, h);
            for (int i = 0; i < names.size(); i++) {
                Ui.text(c, tr, names.get(i), x + 5, y + 4 + i * 11, cols.get(i));
                Ui.text(c, tr, times.get(i), x + w - 5 - tr.getWidth(times.get(i)), y + 4 + i * 11, 0xFFFFFFFF);
            }
        }
    }

    // --------------------------------------------------------------- клавиши
    static final class Keys extends HudWidget {
        final float[] anim = new float[7];
        Keys() { super("keys", "Клавиши", 0.01f, 0.68f, false); }

        private void key(DrawContext c, TextRenderer tr, int i, int x, int y, int kw, int kh, String label, boolean down) {
            anim[i] += ((down ? 1f : 0f) - anim[i]) * 0.35f;
            int bg = Ui.mix(Ui.argb(0x0E1016, VisualsConfig.I.hudOpacity), Ui.argb(Ui.accent(i * 0.1f) & 0xFFFFFF, 0.85f), anim[i]);
            Ui.round(c, x, y, kw, kh, 3, bg);
            Ui.ctext(c, tr, label, x + kw / 2, y + (kh - 8) / 2, Ui.mix(0xFFFFFFFF, 0xFF101010, anim[i]));
        }

        @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y, boolean editor) {
            var o = mc.options;
            TextRenderer tr = mc.textRenderer;
            boolean live = mc.currentScreen == null;
            int k = 19, g = 2;
            w = 3 * k + 2 * g; h = k + g + k + g + 12 + g + k;
            key(c, tr, 0, x + k + g, y, k, k, "W", live && o.forwardKey.isPressed());
            key(c, tr, 1, x, y + k + g, k, k, "A", live && o.leftKey.isPressed());
            key(c, tr, 2, x + k + g, y + k + g, k, k, "S", live && o.backKey.isPressed());
            key(c, tr, 3, x + 2 * (k + g), y + k + g, k, k, "D", live && o.rightKey.isPressed());
            key(c, tr, 4, x, y + 2 * (k + g), w, 12, "—", live && o.jumpKey.isPressed());
            int hw = (w - g) / 2;
            key(c, tr, 5, x, y + 2 * (k + g) + 12 + g, hw, k, "ЛКМ " + ClickTracker.left(), live && o.attackKey.isPressed());
            key(c, tr, 6, x + hw + g, y + 2 * (k + g) + 12 + g, w - hw - g, k, "ПКМ " + ClickTracker.right(), live && o.useKey.isPressed());
        }
    }

    // ------------------------------------------------------------------ цель
    static final class Target extends HudWidget {
        LivingEntity shown; long until; float hp = -1, ghost = -1; long ln;
        Target() { super("target", "Цель (TargetHUD)", 0.4f, 0.12f, true); }

        @Override public void draw(DrawContext c, MinecraftClient mc, int x, int y, boolean editor) {
            long nowMs = System.currentTimeMillis();
            if (mc.targetedEntity instanceof LivingEntity t && t != mc.player) { shown = t; until = nowMs + 2500; }

            String name; float cur, max; int armor = 0; ItemStack main = ItemStack.EMPTY, off = ItemStack.EMPTY;
            if (shown != null && nowMs < until && !shown.isRemoved() && shown.isAlive()) {
                name = shown.getDisplayName().getString();
                cur = shown.getHealth(); max = Math.max(1f, shown.getMaxHealth());
                armor = shown.getArmor();
                main = shown.getMainHandStack(); off = shown.getOffHandStack();
            } else if (editor) {
                name = "Игрок"; cur = 14.5f; max = 20f; armor = 12;
            } else {
                w = h = 0; hp = ghost = -1; return;
            }

            TextRenderer tr = mc.textRenderer;
            long now = System.nanoTime();
            float dt = ln == 0 ? 0 : Math.min(0.1f, (now - ln) / 1e9f); ln = now;
            if (hp < 0) { hp = cur; ghost = cur; }
            hp += (cur - hp) * (1f - (float) Math.exp(-dt * 12f));
            if (cur > ghost) ghost = cur; else ghost += (cur - ghost) * (1f - (float) Math.exp(-dt * 2.5f));

            w = 150; h = 46;
            panel(c, x, y, w, h);
            Ui.text(c, tr, name, x + 5, y + 4, 0xFFFFFFFF);
            String hpt = String.format("%.1f", cur);
            Ui.text(c, tr, hpt, x + w - 5 - tr.getWidth(hpt), y + 4, hpColor(cur / max));

            int bx = x + 5, by = y + 16, bw = w - 10;
            Ui.round(c, bx, by, bw, 8, 2, 0xFF1A1D24);
            int gw = (int) (bw * MathHelper.clamp(ghost / max, 0f, 1f));
            if (gw > 1) Ui.round(c, bx, by, gw, 8, 2, 0x88FFFFFF);
            int fw = (int) (bw * MathHelper.clamp(hp / max, 0f, 1f));
            if (fw > 1) Ui.round(c, bx, by, fw, 8, 2, hpColor(hp / max));

            Ui.text(c, tr, "Броня: " + armor, x + 5, y + 30, 0xFFBBBBBB);
            if (!off.isEmpty()) c.drawItem(off, x + w - 22, y + 26);
            if (!main.isEmpty()) c.drawItem(main, x + w - 40, y + 26);
        }
    }
}
