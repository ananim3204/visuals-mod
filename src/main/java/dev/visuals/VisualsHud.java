package dev.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public final class VisualsHud {
    public static long lastHitMs = 0, lastPvpMs = 0;

    private static float lastHp = -1f;
    private static long hurtMs = 0;

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
        Fx.burst(e);
        Fx.watch(e);
    }

    public static void render(DrawContext ctx, RenderTickCounter tc) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        if (mc.currentScreen instanceof HudEditorScreen) return; // редактор рисует виджеты сам
        VisualsConfig c = VisualsConfig.I;
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();

        HudWidgets.ClickTracker.poll(mc);
        vignette(ctx, mc, sw, sh);
        Fx.frame(ctx, mc);
        if (c.hitMarker) hitMarker(ctx, sw, sh);

        for (HudWidget w : HudWidgets.ALL) {
            VisualsConfig.WidgetState st = c.state(w);
            if (!st.on) continue;
            int x = Math.max(0, Math.min((int) (st.x * sw), Math.max(0, sw - w.w)));
            int y = Math.max(0, Math.min((int) (st.y * sh), Math.max(0, sh - w.h)));
            w.draw(ctx, mc, x, y, false);
        }
    }

    /** Красные края экрана при получении урона и при низком HP. */
    private static void vignette(DrawContext ctx, MinecraftClient mc, int sw, int sh) {
        VisualsConfig c = VisualsConfig.I;
        float hp = mc.player.getHealth();
        long now = System.currentTimeMillis();
        if (lastHp >= 0 && hp < lastHp - 0.01f) hurtMs = now;
        lastHp = hp;
        float a = 0f;
        if (c.hurtVignette) {
            long age = now - hurtMs;
            if (age < 600) a = Math.max(a, (1f - age / 600f) * 0.55f);
        }
        if (c.lowHpVignette && hp <= 6f && mc.player.isAlive()) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(now / 260.0);
            a = Math.max(a, 0.12f + 0.2f * pulse * (1f - hp / 8f));
        }
        if (a <= 0.01f) return;
        int n = 22, sx = Math.max(2, sw / 90), sy = Math.max(2, sh / 90);
        for (int i = 0; i < n; i++) {
            float k = 1f - i / (float) n;
            int col = Ui.argb(0xFF2020, a * k * k);
            ctx.fill(0, i * sy, sw, (i + 1) * sy, col);
            ctx.fill(0, sh - (i + 1) * sy, sw, sh - i * sy, col);
            ctx.fill(i * sx, 0, (i + 1) * sx, sh, col);
            ctx.fill(sw - (i + 1) * sx, 0, sw - i * sx, sh, col);
        }
    }

    private static void hitMarker(DrawContext ctx, int sw, int sh) {
        long age = System.currentTimeMillis() - lastHitMs;
        if (age >= 320) return;
        float t = age / 320f;
        float a = 1f - t;
        int gap = 3 + (int) (5 * Ui.ease(t));
        int col = Ui.argb(0xFFFFFF, a);
        int shadow = Ui.argb(0x000000, a * 0.5f);
        int cx = sw / 2, cy = sh / 2;
        for (int i = gap; i < gap + 5; i++) {
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sy = -1; sy <= 1; sy += 2) {
                    int px = cx + sx * i, py = cy + sy * i;
                    ctx.fill(px, py + 1, px + 1, py + 2, shadow);
                    ctx.fill(px, py, px + 1, py + 1, col);
                }
            }
        }
    }
}
