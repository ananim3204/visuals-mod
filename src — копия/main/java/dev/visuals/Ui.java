package dev.visuals;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

/** Мелкие помощники для рисования: скруглённые прямоугольники, свечение, акцентный цвет. */
public final class Ui {
    /** Глобальная прозрачность (для анимации открытия меню и «призраков» в редакторе). */
    public static float alpha = 1f;

    public static int a(int argb) {
        int al = (int) (((argb >>> 24) & 0xFF) * alpha);
        return (al << 24) | (argb & 0xFFFFFF);
    }

    public static int argb(int rgb, float a) {
        return ((int) (MathHelper.clamp(a, 0f, 1f) * 255f) << 24) | (rgb & 0xFFFFFF);
    }

    public static int mix(int c1, int c2, float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        int a = (int) (((c1 >>> 24) & 255) * (1 - t) + ((c2 >>> 24) & 255) * t);
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static float ease(float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    public static int hsv(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        float r = 0, g = 0, b = 0;
        int i = (int) (h * 6);
        float f = h * 6 - i, p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return 0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    public static int accent() { return accent(0f); }

    /** Цвет акцента. Для радуги offset сдвигает оттенок (даёт градиент вдоль текста/линии). */
    public static int accent(float off) {
        VisualsConfig c = VisualsConfig.I;
        if (c.accent == VisualsConfig.Accent.RAINBOW) {
            return hsv((System.currentTimeMillis() % 6000L) / 6000f + off, 0.6f, 1f);
        }
        return 0xFF000000 | c.accent.rgb;
    }

    public static void rect(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x, y, x + w, y + h, a(col));
    }

    public static void outline(DrawContext c, int x, int y, int w, int h, int col) {
        rect(c, x, y, w, 1, col);
        rect(c, x, y + h - 1, w, 1, col);
        rect(c, x, y + 1, 1, h - 2, col);
        rect(c, x + w - 1, y + 1, 1, h - 2, col);
    }

    /** Прямоугольник со скруглёнными углами (радиус до ~6 px). */
    public static void round(DrawContext c, int x, int y, int w, int h, int r, int col) {
        col = a(col);
        if ((col >>> 24) == 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) { c.fill(x, y, x + w, y + h, col); return; }
        c.fill(x + r, y, x + w - r, y + h, col);
        c.fill(x, y + r, x + r, y + h - r, col);
        c.fill(x + w - r, y + r, x + w, y + h - r, col);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            c.fill(x + inset, y + i, x + r, y + i + 1, col);
            c.fill(x + w - r, y + i, x + w - inset, y + i + 1, col);
            c.fill(x + inset, y + h - i - 1, x + r, y + h - i, col);
            c.fill(x + w - r, y + h - i - 1, x + w - inset, y + h - i, col);
        }
    }

    private static final float[] DY = {-1f, -0.6f, -0.2f, 0.2f, 0.6f, 1f};
    private static final float[] DW = {0.6f, 0.92f, 1f, 0.92f, 0.6f};

    /** Быстрый круг (≤5 заливок, глобальная alpha НЕ применяется — для мирового рендера). */
    public static void disc(DrawContext c, int cx, int cy, int r, int col) {
        if ((col >>> 24) == 0) return;
        if (r <= 1) { c.fill(cx - 1, cy - 1, cx + 1, cy + 1, col); return; }
        if (r == 2) {
            c.fill(cx - 1, cy - 2, cx + 1, cy + 2, col);
            c.fill(cx - 2, cy - 1, cx - 1, cy + 1, col);
            c.fill(cx + 1, cy - 1, cx + 2, cy + 1, col);
            return;
        }
        for (int i = 0; i < 5; i++) {
            int y1 = cy + Math.round(DY[i] * r), y2 = cy + Math.round(DY[i + 1] * r);
            if (y2 <= y1) y2 = y1 + 1;
            int w = Math.round(DW[i] * r);
            c.fill(cx - w, y1, cx + w, y2, col);
        }
    }

    /** Граница по высоте для заливки многоугольников (выставляется перед кадром). */
    public static int clipH = 2000;

    /** Закрашенный треугольник (построчно). Цвет передавать уже с нужной alpha. */
    public static void tri(DrawContext c, float x1, float y1, float x2, float y2, float x3, float y3, int col) {
        if ((col >>> 24) == 0) return;
        float t;
        if (y1 > y2) { t = y1; y1 = y2; y2 = t; t = x1; x1 = x2; x2 = t; }
        if (y1 > y3) { t = y1; y1 = y3; y3 = t; t = x1; x1 = x3; x3 = t; }
        if (y2 > y3) { t = y2; y2 = y3; y3 = t; t = x2; x2 = x3; x3 = t; }
        int ymin = Math.max(-2, (int) Math.ceil(y1)), ymax = Math.min(clipH + 2, (int) Math.floor(y3));
        for (int y = ymin; y <= ymax; y++) {
            float fy = y + 0.5f;
            float xa = x1 + (x3 - x1) * ((fy - y1) / (y3 - y1 + 1e-6f));
            float xb = fy < y2 ? x1 + (x2 - x1) * ((fy - y1) / (y2 - y1 + 1e-6f))
                               : x2 + (x3 - x2) * ((fy - y2) / (y3 - y2 + 1e-6f));
            int l = (int) Math.floor(Math.min(xa, xb)), r = (int) Math.ceil(Math.max(xa, xb));
            if (r > l) c.fill(l, y, r, y + 1, col);
        }
    }

    public static int shade(int rgb, float f) {
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * f)), g = Math.min(255, (int) (((rgb >> 8) & 255) * f)), b = Math.min(255, (int) ((rgb & 255) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static void text(DrawContext c, TextRenderer tr, String s, int x, int y, int col) {
        int cc = a(col);
        if ((cc >>> 24) < 8) return;
        c.drawTextWithShadow(tr, s, x, y, cc);
    }

    public static void textFlat(DrawContext c, TextRenderer tr, String s, int x, int y, int col) {
        int cc = a(col);
        if ((cc >>> 24) < 8) return;
        c.drawText(tr, s, x, y, cc, false);
    }

    public static void ctext(DrawContext c, TextRenderer tr, String s, int cx, int y, int col) {
        text(c, tr, s, cx - tr.getWidth(s) / 2, y, col);
    }

    /** Текст с акцентным градиентом по буквам. */
    public static void gradientText(DrawContext c, TextRenderer tr, String s, int x, int y) {
        int px = x;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            text(c, tr, ch, px, y, accent(i * 0.05f));
            px += tr.getWidth(ch);
        }
    }
}
