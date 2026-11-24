package dev.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Базовый перетаскиваемый элемент HUD. Позиция хранится в долях экрана (0..1). */
public abstract class HudWidget {
    public final String id, name;
    public final float defX, defY;
    public final boolean defOn;
    /** Размер, измеренный при последней отрисовке. */
    public int w, h;

    protected HudWidget(String id, String name, float defX, float defY, boolean defOn) {
        this.id = id; this.name = name; this.defX = defX; this.defY = defY; this.defOn = defOn;
    }

    /** Рисует виджет в (x, y) и обновляет w/h. editor=true — показывать заглушку, даже если данных нет. */
    public abstract void draw(DrawContext c, MinecraftClient mc, int x, int y, boolean editor);

    protected static void panel(DrawContext c, int x, int y, int w, int h) {
        VisualsConfig cfg = VisualsConfig.I;
        float o = cfg.hudOpacity;
        switch (cfg.hudStyle) {
            case GLASS -> {
                Ui.round(c, x, y, w, h, 3, Ui.argb(0x0E1016, o * 0.85f));
                Ui.rect(c, x + 3, y, w - 6, 1, Ui.argb(Ui.accent(0) & 0xFFFFFF, 0.9f));
            }
            case SOLID -> {
                Ui.round(c, x, y, w, h, 2, Ui.argb(0x0B0D12, Math.min(1f, o + 0.25f)));
                Ui.rect(c, x, y + 1, 2, h - 2, Ui.accent(0));
            }
            default -> {}
        }
    }

    /** Таблица «название — значение» в одной панели. */
    protected void rows(DrawContext c, MinecraftClient mc, int x, int y, String[] labels, String[] values, int[] colors) {
        TextRenderer tr = mc.textRenderer;
        int lw = 0, vw = 0;
        for (int i = 0; i < labels.length; i++) {
            lw = Math.max(lw, tr.getWidth(labels[i]));
            vw = Math.max(vw, tr.getWidth(values[i]));
        }
        w = lw + 6 + vw + 10;
        h = labels.length * 10 + 6;
        panel(c, x, y, w, h);
        for (int i = 0; i < labels.length; i++) {
            Ui.text(c, tr, labels[i], x + 5, y + 4 + i * 10, Ui.accent(0));
            Ui.text(c, tr, values[i], x + 5 + lw + 6, y + 4 + i * 10, colors[i]);
        }
    }

    protected void one(DrawContext c, MinecraftClient mc, int x, int y, String label, String value, int color) {
        rows(c, mc, x, y, new String[]{label}, new String[]{value}, new int[]{color});
    }

    protected static void bar(DrawContext c, int x, int y, int w, int h, float ratio, int color) {
        Ui.round(c, x, y, w, h, 2, 0xFF1A1D24);
        int fw = (int) (w * MathHelperClamp(ratio));
        if (fw > 0) Ui.round(c, x, y, Math.max(fw, 2), h, 2, color);
    }

    protected static float MathHelperClamp(float v) { return v < 0 ? 0 : Math.min(v, 1); }

    protected static int good() { return 0xFF55FF7A; }
    protected static int warn() { return 0xFFFFD74D; }
    protected static int bad() { return 0xFFFF5555; }
}
