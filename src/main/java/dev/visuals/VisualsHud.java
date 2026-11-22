package dev.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public final class VisualsHud {
    public static long lastHitMs = 0, lastPvpMs = 0;
    private static float shownHp = -1;
    private static long lastNs = 0;

    public static boolean inPvp() { return System.currentTimeMillis() - lastPvpMs < 6000; }

    /** Вызывается при ударе по существу (клиент). */
    public static void onHit(Entity e) {
        MinecraftClient mc = MinecraftClient.getInstance();
        VisualsConfig c = VisualsConfig.I;
        lastHitMs = System.currentTimeMillis();
        if (e instanceof PlayerEntity) lastPvpMs = lastHitMs;

        if (c.hitSound.sound != null) {
            mc.getSoundManager().play(PositionedSoundInstance.master(c.hitSound.sound, 1.0f, 1.0f));
        }
        if (c.hitParticle.type != null && mc.world != null) {
            for (int i = 0; i < 12; i++) {
                double x = e.getX() + (mc.world.random.nextDouble() - 0.5) * 0.8;
                double y = e.getY() + mc.world.random.nextDouble() * e.getHeight();
                double z = e.getZ() + (mc.world.random.nextDouble() - 0.5) * 0.8;
                mc.world.addParticleClient(c.hitParticle.type, x, y, z,
                        (mc.world.random.nextDouble() - 0.5) * 0.3, 0.15, (mc.world.random.nextDouble() - 0.5) * 0.3);
            }
        }
    }

    private static int hpColor(float ratio) {
        return ratio > 0.6f ? 0xFF55FF55 : ratio > 0.3f ? 0xFFFFFF55 : 0xFFFF5555;
    }

    public static void render(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.options.hudHidden) return;
        VisualsConfig c = VisualsConfig.I;
        TextRenderer tr = mc.textRenderer;
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();

        // ---- Левая панель ----
        List<String> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        if (c.showFps) { lines.add("FPS: " + mc.getCurrentFps()); colors.add(0xFFFFFFFF); }
        if (c.showPing) {
            PlayerListEntry e = mc.getNetworkHandler() == null ? null : mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
            int ping = e == null ? 0 : e.getLatency();
            lines.add("Пинг: " + ping + " мс");
            colors.add(ping < 80 ? 0xFF55FF55 : ping < 160 ? 0xFFFFFF55 : 0xFFFF5555);
        }
        if (c.showCoords) {
            lines.add("XYZ: " + p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ());
            colors.add(0xFFFFFFFF);
        }
        if (c.showFood) {
            lines.add("Голод: " + p.getHungerManager().getFoodLevel() + "/20");
            colors.add(0xFFFFAA55);
            ItemStack held = p.getMainHandStack();
            FoodComponent food = held.get(DataComponentTypes.FOOD);
            if (food != null) {
                lines.add(String.format("Еда восстановит: +%d голода, +%.1f насыщ.", food.nutrition(), food.saturation()));
                colors.add(0xFFFFD27F);
            }
        }
        int x = 6, y = 6;
        int w = 100;
        for (String s : lines) w = Math.max(w, tr.getWidth(s));
        int h = lines.size() * 10 + (c.showHp ? 14 : 0);
        if (h > 0) ctx.fill(x - 3, y - 3, x + w + 3, y + h + 1, 0x90000000);
        for (int i = 0; i < lines.size(); i++) {
            ctx.drawTextWithShadow(tr, lines.get(i), x, y + i * 10, colors.get(i));
        }

        // ---- HP (плавная полоска) ----
        if (c.showHp) {
            float hp = p.getHealth(), max = Math.max(1f, p.getMaxHealth());
            long now = System.nanoTime();
            float dt = lastNs == 0 ? 0 : (now - lastNs) / 1e9f;
            lastNs = now;
            if (shownHp < 0 || !c.smoothPvp) shownHp = hp;
            else shownHp += (hp - shownHp) * (1f - (float) Math.exp(-dt * 8f));
            int by = y + lines.size() * 10 + 2;
            ctx.fill(x, by, x + w, by + 9, 0xFF2A0000);
            ctx.fill(x, by, x + (int) (w * MathHelper.clamp(shownHp / max, 0f, 1f)), by + 9, hpColor(hp / max));
            String t = String.format("HP %.1f / %.0f", hp, max);
            float abs = p.getAbsorptionAmount();
            if (abs > 0) t += String.format(" (+%.1f)", abs);
            ctx.drawTextWithShadow(tr, t, x + 3, by + 1, 0xFFFFFFFF);
        }

        // ---- Броня (справа) ----
        if (c.showArmor) {
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            int ax = sw - 84, ay = sh / 2 - 40;
            for (EquipmentSlot s : slots) {
                ItemStack st = p.getEquippedStack(s);
                if (!st.isEmpty()) {
                    ctx.fill(ax - 3, ay - 2, ax + 80, ay + 18, 0x90000000);
                    ctx.drawItem(st, ax, ay);
                    int max = st.getMaxDamage();
                    if (max > 0) {
                        int left = max - st.getDamage();
                        ctx.drawTextWithShadow(tr, left + "/" + max, ax + 20, ay + 4, hpColor((float) left / max));
                    }
                }
                ay += 21;
            }
        }

        // ---- HP цели ----
        if (c.showTargetHp && mc.targetedEntity instanceof LivingEntity t) {
            int bw = 90, bx = sw / 2 - bw / 2, by = 24;
            float max = Math.max(1f, t.getMaxHealth());
            ctx.drawCenteredTextWithShadow(tr, t.getDisplayName(), sw / 2, by - 11, 0xFFFFFFFF);
            ctx.fill(bx, by, bx + bw, by + 9, 0xFF2A0000);
            ctx.fill(bx, by, bx + (int) (bw * MathHelper.clamp(t.getHealth() / max, 0f, 1f)), by + 9, hpColor(t.getHealth() / max));
            ctx.drawCenteredTextWithShadow(tr, Text.literal(String.format("%.1f", t.getHealth())), sw / 2, by + 1, 0xFFFFFFFF);
        }

        // ---- Хитмаркер ----
        if (c.hitMarker) {
            long age = System.currentTimeMillis() - lastHitMs;
            if (age < 300) {
                int a = (int) (255 * (1f - age / 300f));
                int col = (a << 24) | 0xFFFFFF;
                int cx = sw / 2, cy = sh / 2;
                for (int i = 3; i <= 8; i++) {
                    ctx.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, col);
                    ctx.fill(cx - i - 1, cy + i, cx - i, cy + i + 1, col);
                    ctx.fill(cx + i, cy - i - 1, cx + i + 1, cy - i, col);
                    ctx.fill(cx - i - 1, cy - i - 1, cx - i, cy - i, col);
                }
            }
        }
    }
}
