package dev.visuals;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;

import java.util.Map;
import java.util.WeakHashMap;

/** Единый стиль для всех экранов Minecraft: акцентные края, пылинки и плоские стеклянные кнопки. */
public final class UiSkin {
    private static final Map<ClickableWidget, Float> HOVER = new WeakHashMap<>();
    private static final long START = System.nanoTime();

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen.getClass().getName().startsWith("dev.visuals")) return;
            if (screen instanceof HandledScreen || screen instanceof ChatScreen) return;
            ScreenEvents.afterRender(screen).register((s, ctx, mx, my, dt) -> render(client, s, ctx, mx, my));
        });
    }

    private static float frac(float v) {
        double s = Math.sin(v) * 43758.5453;
        return (float) (s - Math.floor(s));
    }

    private static void render(MinecraftClient mc, Screen s, DrawContext ctx, int mx, int my) {
        if (!VisualsConfig.I.customGui) return;
        int w = s.width, h = s.height;
        float time = (System.nanoTime() - START) / 1e9f;
        int acc = Ui.accent() & 0xFFFFFF;

        // акцентные края сверху и снизу
        ctx.fillGradient(0, 0, w, h / 5, Ui.argb(acc, 0.22f), Ui.argb(acc, 0f));
        ctx.fillGradient(0, h - h / 5, w, h, Ui.argb(acc, 0f), Ui.argb(acc, 0.22f));
        for (int x = 0; x < w; x += 4) {
            int c = Ui.accent(x / (float) w * 0.6f);
            ctx.fill(x, 0, x + 4, 1, c);
            ctx.fill(x, h - 1, x + 4, h, Ui.argb(c & 0xFFFFFF, 0.6f));
        }

        // пылинки (только на экранах без мира — они лучше смотрятся на панораме)
        if (mc.world == null) {
            for (int i = 0; i < 40; i++) {
                float rx = frac(i * 12.9898f), ry = frac(i * 78.233f), sp = 5f + frac(i * 3.7f) * 12f;
                float x = rx * w + (float) Math.sin(time * 0.4 + i) * 5f;
                float y = ((ry * h - time * sp) % h + h) % h;
                float tw = 0.3f + 0.5f * (0.5f + 0.5f * (float) Math.sin(time * 1.5 + i * 2.1));
                ctx.fill((int) x, (int) y, (int) x + 1, (int) y + 1, Ui.argb(Ui.accent(rx * 0.4f) & 0xFFFFFF, tw));
            }
        }

        // плоские кнопки поверх ванильных
        TextRenderer tr = mc.textRenderer;
        for (ClickableWidget cw : Screens.getButtons(s)) {
            if (!(cw instanceof ButtonWidget || cw instanceof CyclingButtonWidget)) continue;
            if (!cw.visible) continue;
            String msg = cw.getMessage().getString();
            if (msg.isEmpty()) continue;
            int x = cw.getX(), y = cw.getY(), bw = cw.getWidth(), bh = cw.getHeight();
            boolean hov = cw.active && mx >= x && mx < x + bw && my >= y && my < y + bh;
            float a = HOVER.getOrDefault(cw, 0f);
            a += ((hov ? 1f : 0f) - a) * 0.3f;
            HOVER.put(cw, a);

            int base = cw.active ? Ui.mix(0xFF14171F, 0xFF000000 | acc, a * 0.55f) : 0xFF101218;
            ctx.fill(x, y, x + bw, y + bh, base);
            Ui.outline(ctx, x, y, bw, bh, cw.active ? Ui.argb(acc, 0.35f + 0.5f * a) : 0x30FFFFFF);
            if (a > 0.05f) ctx.fill(x, y + 2, x + 2, y + bh - 2, Ui.accent());
            String txt = tr.getWidth(msg) > bw - 8 ? tr.trimToWidth(msg, bw - 14) + "…" : msg;
            ctx.drawCenteredTextWithShadow(tr, txt, x + bw / 2, y + (bh - 8) / 2, cw.active ? 0xFFFFFFFF : 0xFF777777);
        }
    }
}
