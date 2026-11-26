package dev.visuals;

import dev.visuals.VisualsConfig.Hat;
import dev.visuals.VisualsConfig.Pack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Визуальная косметика на своём игроке (видна от 3-го лица): шапки, рюкзаки, крылья.
 * Модели рисуются как закрашенные эллипсы/многоугольники, спроецированные на экран.
 */
final class Cosmetics {
    private static final float[] t = new float[4];
    private static float bodyYaw;
    private static boolean init;
    private static final double UP_X = 0, UP_Y = 1, UP_Z = 0;

    private static boolean themed() { return VisualsConfig.I.themedCosmetics; }

    /** Окраска под тему клиента, если включена. */
    private static int col(int natural) {
        return themed() ? Ui.mix(0xFF000000 | natural, Ui.accent(), 0.7f) : (0xFF000000 | natural);
    }

    static void draw(DrawContext c, MinecraftClient mc, Fx.Cam cam, Vec3d pos, float dt, float clock) {
        ClientPlayerEntity p = mc.player;
        if (p == null) return;
        VisualsConfig cfg = VisualsConfig.I;

        float target = p.getBodyYaw();
        if (!init) { bodyYaw = target; init = true; }
        bodyYaw += MathHelper.wrapDegrees(target - bodyYaw) * Math.min(1f, dt * 18f);

        float headTop = p.getHeight() - 0.06f;
        if (cfg.hat != Hat.NONE) hat(c, cam, cfg.hat, pos.x, pos.y + headTop, pos.z, p.getYaw(), cfg.hatScale, clock);
        if (cfg.pack != Pack.NONE) pack(c, cam, cfg.pack, pos, p, clock);
    }

    // ------------------------------------------------------------ векторы
    private static double[] ma(double[] a, double[] b, double k) {
        return new double[]{a[0] + b[0] * k, a[1] + b[1] * k, a[2] + b[2] * k};
    }

    private static double[] norm(double[] a) {
        double l = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]) + 1e-9;
        return new double[]{a[0] / l, a[1] / l, a[2] / l};
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static boolean facing(Fx.Cam cam, double x, double z, double dx, double dz) {
        return (cam.x - x) * dx + (cam.z - z) * dz > 0;
    }

    // ------------------------------------------------------------ примитивы
    private static void dot(DrawContext c, Fx.Cam cam, double x, double y, double z, float size, int color) {
        if (!cam.project(x, y, z, t)) return;
        int r = Math.max(1, Math.round(size * t[3]));
        Ui.disc(c, Math.round(t[0]), Math.round(t[1]), r, color);
    }

    /** Светящаяся точка: мягкий ореол + яркое ядро. */
    private static void soft(DrawContext c, Fx.Cam cam, double x, double y, double z, float size, int rgb, float alpha) {
        dot(c, cam, x, y, z, size * 2.3f, Ui.argb(rgb, alpha * 0.22f));
        dot(c, cam, x, y, z, size, Ui.argb(rgb, alpha));
    }

    private static void line(DrawContext c, Fx.Cam cam, double[] a, double[] b, float size, int color, int steps) {
        for (int i = 0; i <= steps; i++) {
            double f = i / (double) steps;
            dot(c, cam, a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f, size, color);
        }
    }

    private static void fillEllipse(DrawContext c, float cx, float cy, float rx, float ry, int color) {
        int step = ry > 14 ? 2 : 1;
        int y0 = (int) Math.floor(cy - ry), y1 = (int) Math.ceil(cy + ry);
        for (int yy = y0; yy <= y1; yy += step) {
            float k = (yy + 0.5f * step - cy) / ry;
            if (k < -1f || k > 1f) continue;
            float hw = rx * (float) Math.sqrt(1f - k * k);
            c.fill(Math.round(cx - hw), yy, Math.round(cx + hw), yy + step, color);
        }
    }

    /** Эллипс (кольцо в перспективе); gloss добавляет блик слева — получается эффект объёма. */
    private static void ellipse(DrawContext c, Fx.Cam cam, double x, double y, double z, double r, int color, boolean gloss) {
        if (!cam.project(x, y, z, t)) return;
        float cx = t[0], cy = t[1], rx = (float) (r * t[3]);
        double dx = cam.x - x, dy = cam.y - y, dz = cam.z - z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        float ry = Math.max(0.8f, rx * (float) (Math.abs(dy) / dist));
        fillEllipse(c, cx, cy, rx, ry, color);
        if (gloss && rx > 6f) fillEllipse(c, cx - rx * 0.3f, cy - ry * 0.15f, rx * 0.5f, ry * 0.6f, Ui.mix(color, 0xFFFFFFFF, 0.15f));
    }

    private static void ellipse(DrawContext c, Fx.Cam cam, double x, double y, double z, double r, int color) {
        ellipse(c, cam, x, y, z, r, color, false);
    }

    /**
     * Конус/цилиндр из стопки эллипсов. bx/bz — изгиб верхушки, by — провисание, bands>0 — полосатая раскраска.
     */
    private static void coneImpl(DrawContext c, Fx.Cam cam, double x, double yb, double z, double r0, double r1, double h,
                                 int c0, int c1, double bx, double bz, double by, int bands) {
        if (!cam.project(x, yb, z, t)) return;
        int n = MathHelper.clamp((int) (h * t[3] / 1.5f), 3, 28);
        boolean above = cam.y > yb + h * 0.5;
        for (int i = 0; i < n; i++) {
            int idx = above ? i : n - 1 - i;
            float f = idx / (float) (n - 1);
            double ff = f * f;
            int colr = bands > 0 ? (((int) (f * bands) % 2 == 0) ? c0 : c1) : Ui.mix(c0, c1, f);
            ellipse(c, cam, x + bx * ff, yb + f * h - by * ff, z + bz * ff, r0 + (r1 - r0) * f, colr, bands <= 0);
        }
    }

    private static void cone(DrawContext c, Fx.Cam cam, double x, double yb, double z, double r0, double r1, double h, int c0, int c1) {
        coneImpl(c, cam, x, yb, z, r0, r1, h, c0, c1, 0, 0, 0, 0);
    }

    private static void tri3(DrawContext c, Fx.Cam cam, double[] a, double[] b, double[] d, int color) {
        if (!cam.project(a[0], a[1], a[2], t)) return;
        float ax = t[0], ay = t[1];
        if (!cam.project(b[0], b[1], b[2], t)) return;
        float bx = t[0], by = t[1];
        if (!cam.project(d[0], d[1], d[2], t)) return;
        Ui.tri(c, ax, ay, bx, by, t[0], t[1], color);
    }

    private static final int[][] FACES = {{1, 3, 7, 5}, {0, 4, 6, 2}, {2, 6, 7, 3}, {0, 1, 5, 4}, {4, 5, 7, 6}, {0, 2, 3, 1}};

    /** Параллелепипед (для рюкзаков): рисуются видимые грани с простым освещением. */
    private static void box(DrawContext c, Fx.Cam cam, double cx, double cy, double cz, double hw, double hh, double hd,
                            double[] r, double[] f, int color) {
        double[][] P = new double[8][3];
        for (int i = 0; i < 8; i++) {
            double sx = (i & 1) != 0 ? 1 : -1, sy = (i & 2) != 0 ? 1 : -1, sz = (i & 4) != 0 ? 1 : -1;
            P[i][0] = cx + r[0] * sx * hw + f[0] * sz * hd;
            P[i][1] = cy + sy * hh;
            P[i][2] = cz + r[2] * sx * hw + f[2] * sz * hd;
        }
        double[][] N = {{r[0], 0, r[2]}, {-r[0], 0, -r[2]}, {0, 1, 0}, {0, -1, 0}, {f[0], 0, f[2]}, {-f[0], 0, -f[2]}};
        double[] half = {hw, hw, hh, hh, hd, hd};
        for (int fi = 0; fi < 6; fi++) {
            double fcx = cx + N[fi][0] * half[fi], fcy = cy + N[fi][1] * half[fi], fcz = cz + N[fi][2] * half[fi];
            double vis = N[fi][0] * (cam.x - fcx) + N[fi][1] * (cam.y - fcy) + N[fi][2] * (cam.z - fcz);
            if (vis <= 0) continue;
            double lit = Math.max(0, N[fi][0] * 0.3 + N[fi][1] * 0.85 + N[fi][2] * 0.4);
            int shaded = Ui.shade(color, (float) (0.5 + 0.55 * lit));
            int[] q = FACES[fi];
            tri3(c, cam, P[q[0]], P[q[1]], P[q[2]], shaded);
            tri3(c, cam, P[q[0]], P[q[2]], P[q[3]], shaded);
        }
    }

    // ----------------------------------------------------------------- шапки
    private static void hat(DrawContext c, Fx.Cam cam, Hat h, double x, double y, double z, float headYaw, float s, float clock) {
        double th = headYaw * 0.017453292;
        double fx = -Math.sin(th), fz = Math.cos(th), rx = -Math.cos(th), rz = -Math.sin(th);
        switch (h) {
            case SAMURAI -> {
                // золотая кромка и лаковый конус
                cone(c, cam, x, y - 0.012 * s, z, 0.5 * s, 0.5 * s, 0.024 * s, col(0x8A6A1E), col(0xF2D35C));
                cone(c, cam, x, y, z, 0.47 * s, 0.05 * s, 0.2 * s, col(0x14141A), col(0x3C3C4A));
                // красная и золотая полосы лака (радиус считаем по конусу)
                cone(c, cam, x, y + 0.05 * s, z, (0.47 - 0.42 * 0.25) * s + 0.004, (0.47 - 0.42 * 0.25) * s + 0.004, 0.014 * s, col(0xA81D1D), col(0xD83A3A));
                cone(c, cam, x, y + 0.11 * s, z, (0.47 - 0.42 * 0.55) * s + 0.004, (0.47 - 0.42 * 0.55) * s + 0.004, 0.007 * s, col(0xB8902A), col(0xF2D35C));
                // рёбра шляпы
                for (int k = 0; k < 10; k++) {
                    double a = k * Math.PI * 2 / 10 + 0.3, ca = Math.cos(a), sa = Math.sin(a);
                    if (!facing(cam, x, z, ca, sa)) continue;
                    for (int j = 0; j <= 7; j++) {
                        double tt = j / 7.0 * 0.2;
                        double rr = (0.47 - 0.42 * (tt / 0.2)) * s + 0.003;
                        dot(c, cam, x + ca * rr, y + tt * s, z + sa * rr, 0.0045f * s, col(0x0B0B0F));
                    }
                }
                // золотой герб спереди
                if (facing(cam, x, z, fx, fz)) {
                    double rr = (0.47 - 0.42 * 0.375) * s;
                    double ex = x + fx * rr, ey = y + 0.075 * s, ez = z + fz * rr;
                    dot(c, cam, ex, ey, ez, 0.03f * s, col(0xF2D35C));
                    dot(c, cam, ex, ey, ez, 0.019f * s, col(0x14141A));
                    dot(c, cam, ex, ey, ez, 0.009f * s, col(0xF2D35C));
                }
                // навершие
                cone(c, cam, x, y + 0.19 * s, z, 0.03 * s, 0.012 * s, 0.05 * s, col(0xB8902A), col(0xF2D35C));
                soft(c, cam, x, y + 0.245 * s, z, 0.011f * s, 0xFFE680, 0.9f);
                // ремешки под подбородок
                for (int sd = -1; sd <= 1; sd += 2) {
                    double[] st = {x + rx * sd * 0.33 * s + fx * 0.08, y, z + rz * sd * 0.33 * s + fz * 0.08};
                    double[] en = {x + fx * 0.14, y - 0.34, z + fz * 0.14};
                    for (int i = 0; i <= 14; i++) {
                        double u = i / 14.0, bulge = 0.07 * Math.sin(u * Math.PI);
                        dot(c, cam, st[0] + (en[0] - st[0]) * u + rx * sd * bulge, st[1] + (en[1] - st[1]) * u,
                                st[2] + (en[2] - st[2]) * u + rz * sd * bulge, 0.006f * s, col(0xA81D1D));
                    }
                }
                dot(c, cam, x + fx * 0.14, y - 0.34, z + fz * 0.14, 0.013f * s, col(0xF2D35C));
            }
            case WIZARD -> {
                double bendX = -fx * 0.2 * s, bendZ = -fz * 0.2 * s;
                cone(c, cam, x, y - 0.014 * s, z, 0.44 * s, 0.44 * s, 0.02 * s, col(0xB8902A), col(0xF2D35C));
                cone(c, cam, x, y - 0.008 * s, z, 0.42 * s, 0.38 * s, 0.026 * s, col(0x2E1860), col(0x4A2B8C));
                coneImpl(c, cam, x, y, z, 0.22 * s, 0.0, 0.6 * s, col(0x3A1F7A), col(0x9468EE), bendX, bendZ, 0.05 * s, 0);
                cone(c, cam, x, y + 0.025 * s, z, 0.226 * s, 0.226 * s, 0.04 * s, col(0xB8902A), col(0xF6D860));
                if (facing(cam, x, z, fx, fz)) {
                    dot(c, cam, x + fx * 0.228 * s, y + 0.045 * s, z + fz * 0.228 * s, 0.024f * s, col(0xF6D860));
                    dot(c, cam, x + fx * 0.228 * s, y + 0.045 * s, z + fz * 0.228 * s, 0.012f * s, col(0x2E1860));
                }
                // звёзды и луны на ткани
                for (int k = 0; k < 7; k++) {
                    double a = k * 2.4 + 0.5, f = 0.12 + 0.62 * ((k * 0.37) % 1.0);
                    double rr = 0.22 * s * (1 - f), ff = f * f;
                    double sx = x + Math.cos(a) * rr + bendX * ff, sz = z + Math.sin(a) * rr + bendZ * ff;
                    if (!facing(cam, x, z, Math.cos(a), Math.sin(a))) continue;
                    float tw = 0.55f + 0.45f * (float) Math.sin(clock * 3 + k * 1.7);
                    soft(c, cam, sx, y + f * 0.6 * s - 0.05 * s * ff, sz, 0.009f * s, 0xFFE680, tw);
                }
                double tx = x + bendX, ty = y + 0.6 * s - 0.05 * s, tz = z + bendZ;
                soft(c, cam, tx, ty, tz, 0.02f * s, 0xFFF3B0, 0.7f + 0.3f * (float) Math.sin(clock * 4));
                dot(c, cam, tx + rx * 0.03 * s, ty, tz + rz * 0.03 * s, 0.004f * s, col(0xFFF3B0));
                dot(c, cam, tx - rx * 0.03 * s, ty, tz - rz * 0.03 * s, 0.004f * s, col(0xFFF3B0));
            }
            case TOPHAT -> {
                cone(c, cam, x, y - 0.012 * s, z, 0.33 * s, 0.33 * s, 0.024 * s, col(0x0C0C0E), col(0x1E1E22));
                cone(c, cam, x, y, z, 0.19 * s, 0.19 * s, 0.31 * s, col(0x101012), col(0x2C2C32));
                cone(c, cam, x, y + 0.012 * s, z, 0.196 * s, 0.196 * s, 0.054 * s, col(0x8E1830), col(0xB52A45));
                cone(c, cam, x, y + 0.068 * s, z, 0.196 * s, 0.196 * s, 0.006 * s, col(0xB8902A), col(0xF2D35C));
                cone(c, cam, x, y + 0.008 * s, z, 0.196 * s, 0.196 * s, 0.005 * s, col(0xB8902A), col(0xF2D35C));
                ellipse(c, cam, x, y + 0.312 * s, z, 0.19 * s, col(0x3A3A42));
                ellipse(c, cam, x, y + 0.313 * s, z, 0.165 * s, col(0x24242A));
                if (facing(cam, x, z, fx, fz)) {
                    double ex = x + fx * 0.198 * s, ez = z + fz * 0.198 * s;
                    dot(c, cam, ex, y + 0.04 * s, ez, 0.02f * s, col(0xF2D35C));
                    dot(c, cam, ex, y + 0.04 * s, ez, 0.01f * s, col(0x8E1830));
                }
            }
            case PARTY -> {
                coneImpl(c, cam, x, y, z, 0.2 * s, 0.0, 0.44 * s, col(0xFF4FA0), col(0x4FD0FF), 0, 0, 0, 9);
                for (int k = 0; k < 8; k++) {
                    double a = k * 0.8 + clock * 0.3;
                    soft(c, cam, x + Math.cos(a) * 0.17 * s, y + 0.06 * s + ((k * 0.13) % 0.3) * s, z + Math.sin(a) * 0.17 * s, 0.008f * s,
                            0xFFE14D, 0.8f);
                }
                for (int k = 0; k < 7; k++) {
                    double a = k * 0.9;
                    dot(c, cam, x + Math.cos(a) * 0.025 * s, y + 0.44 * s + Math.sin(a * 1.3) * 0.02 * s, z + Math.sin(a) * 0.025 * s, 0.013f * s,
                            k % 2 == 0 ? 0xFFFFE14D : 0xFFFFFFFF);
                }
                for (int k = 0; k < 8; k++) {
                    double a = clock * 0.6 + k * 0.785, rr = 0.34 + 0.05 * Math.sin(clock + k);
                    double yy = y + 0.18 * s + Math.sin(clock * 1.5 + k * 1.3) * 0.12 * s;
                    dot(c, cam, x + Math.cos(a) * rr * s, yy, z + Math.sin(a) * rr * s, 0.008f * s, Ui.hsv(k / 8f, 0.7f, 1f));
                }
            }
            case SANTA -> {
                double bendX = (-fx * 0.13 + rx * 0.1) * s, bendZ = (-fz * 0.13 + rz * 0.1) * s, drop = 0.11 * s;
                coneImpl(c, cam, x, y, z, 0.2 * s, 0.05 * s, 0.36 * s, col(0xB81818), col(0xE84545), bendX, bendZ, drop, 0);
                cone(c, cam, x, y - 0.01 * s, z, 0.218 * s, 0.218 * s, 0.07 * s, col(0xDADAE4), col(0xFFFFFF));
                for (int k = 0; k < 14; k++) {
                    double a = k * 0.45 + 0.2;
                    if (!facing(cam, x, z, Math.cos(a), Math.sin(a))) continue;
                    dot(c, cam, x + Math.cos(a) * 0.222 * s, y + 0.01 * s + ((k * 0.37) % 1) * 0.04 * s, z + Math.sin(a) * 0.222 * s, 0.01f * s, 0xFFC8C8D6);
                }
                double tx = x + bendX, ty = y + 0.36 * s - drop, tz = z + bendZ;
                for (int k = 0; k < 8; k++) {
                    double a = k * 0.8;
                    dot(c, cam, tx + Math.cos(a) * 0.03 * s, ty + Math.sin(a * 1.7) * 0.025 * s - 0.02 * s, tz + Math.sin(a) * 0.03 * s, 0.025f * s,
                            k % 2 == 0 ? 0xFFFFFFFF : 0xFFE8E8F2);
                }
                dot(c, cam, tx, ty - 0.02 * s, tz, 0.04f * s, 0xFFFFFFFF);
            }
            case CROWN -> {
                cone(c, cam, x, y, z, 0.2 * s, 0.2 * s, 0.1 * s, col(0xA67C1A), col(0xF6D860));
                ellipse(c, cam, x, y + 0.095 * s, z, 0.185 * s, col(0x8B1A2A), true);
                for (int k = 0; k < 14; k++) {
                    double a = k * Math.PI * 2 / 14 + 0.1;
                    if (!facing(cam, x, z, Math.cos(a), Math.sin(a))) continue;
                    dot(c, cam, x + Math.cos(a) * 0.203 * s, y + 0.03 * s, z + Math.sin(a) * 0.203 * s, 0.007f * s, col(0x8A6A1E));
                }
                for (int k = 0; k < 5; k++) {
                    double a = k * Math.PI * 2 / 5, ca = Math.cos(a), sa = Math.sin(a);
                    double sx = x + ca * 0.2 * s, sz = z + sa * 0.2 * s;
                    cone(c, cam, sx, y + 0.095 * s, sz, 0.042 * s, 0.0, 0.15 * s, col(0xD4AF37), col(0xFFE680));
                    dot(c, cam, sx, y + 0.255 * s, sz, 0.017f * s, 0xFFFFFFFF);
                    double a2 = a + Math.PI / 5;
                    cone(c, cam, x + Math.cos(a2) * 0.2 * s, y + 0.095 * s, z + Math.sin(a2) * 0.2 * s, 0.03 * s, 0.0, 0.08 * s, col(0xD4AF37), col(0xFFE680));
                    if (facing(cam, x, z, Math.cos(a2), Math.sin(a2))) {
                        int[] gem = {0xE02A4A, 0x2A9BE0, 0x2ADB6A, 0xE02A4A, 0xA02AE0};
                        soft(c, cam, x + Math.cos(a2) * 0.205 * s, y + 0.05 * s, z + Math.sin(a2) * 0.205 * s, 0.014f * s, gem[k], 0.95f);
                    }
                }
                if (facing(cam, x, z, fx, fz)) {
                    soft(c, cam, x + fx * 0.206 * s, y + 0.05 * s, z + fz * 0.206 * s, 0.022f * s, 0xFF3355,
                            0.8f + 0.2f * (float) Math.sin(clock * 3));
                }
            }
            case HALO -> {
                double yy = y + 0.3 * s + Math.sin(clock * 2.0) * 0.018;
                for (int k = 0; k < 64; k++) {
                    double a = k * Math.PI * 2 / 64 + clock * 0.5;
                    double px = x + Math.cos(a) * 0.27 * s, pz = z + Math.sin(a) * 0.27 * s;
                    dot(c, cam, px, yy, pz, 0.026f * s, Ui.argb(0xFFE680, 0.14f));
                    dot(c, cam, px, yy, pz, 0.009f * s, Ui.argb(0xFFF6C0, 0.95f));
                }
                for (int k = 0; k < 6; k++) {
                    double a = -clock * 1.2 + k * 1.047;
                    double px = x + Math.cos(a) * 0.27 * s, pz = z + Math.sin(a) * 0.27 * s;
                    soft(c, cam, px, yy + 0.02 * s, pz, 0.014f * s, 0xFFFFFF, 0.9f);
                    dot(c, cam, px, yy + 0.05 * s, pz, 0.004f * s, 0xFFFFFFFF);
                    dot(c, cam, px, yy - 0.01 * s, pz, 0.004f * s, 0xFFFFFFFF);
                }
            }
            case CAT_EARS -> {
                // дужка
                for (int i = 0; i <= 20; i++) {
                    double u = i / 10.0 - 1.0;
                    dot(c, cam, x + rx * u * 0.2 * s, y + 0.02 * s + 0.045 * s * (1 - u * u), z + rz * u * 0.2 * s, 0.009f * s, col(0x1B1B1F));
                }
                for (int sd = -1; sd <= 1; sd += 2) {
                    double[] a = {x + rx * sd * 0.06 * s, y - 0.01, z + rz * sd * 0.06 * s};
                    double[] b = {x + rx * sd * 0.27 * s, y - 0.01, z + rz * sd * 0.27 * s};
                    double[] tip = {x + rx * sd * 0.21 * s + fx * 0.01, y + 0.23 * s, z + rz * sd * 0.21 * s + fz * 0.01};
                    tri3(c, cam, a, b, tip, col(0x1B1B1F));
                    double[] a2 = {x + rx * sd * 0.10 * s + fx * 0.012, y, z + rz * sd * 0.10 * s + fz * 0.012};
                    double[] b2 = {x + rx * sd * 0.235 * s + fx * 0.012, y, z + rz * sd * 0.235 * s + fz * 0.012};
                    double[] tip2 = {x + rx * sd * 0.205 * s + fx * 0.012, y + 0.17 * s, z + rz * sd * 0.205 * s + fz * 0.012};
                    tri3(c, cam, a2, b2, tip2, col(0xFF8FB5));
                    double[] a3 = {x + rx * sd * 0.13 * s + fx * 0.014, y + 0.01, z + rz * sd * 0.13 * s + fz * 0.014};
                    double[] b3 = {x + rx * sd * 0.21 * s + fx * 0.014, y + 0.01, z + rz * sd * 0.21 * s + fz * 0.014};
                    double[] tip3 = {x + rx * sd * 0.2 * s + fx * 0.014, y + 0.11 * s, z + rz * sd * 0.2 * s + fz * 0.014};
                    tri3(c, cam, a3, b3, tip3, col(0xFFC2D8));
                    dot(c, cam, tip[0], tip[1] + 0.015, tip[2], 0.01f * s, col(0x2A2A30));
                }
            }
            case HORNS -> {
                for (int sd = -1; sd <= 1; sd += 2) {
                    for (int i = 0; i < 11; i++) {
                        double u = i / 10.0;
                        double lat = (0.15 + 0.2 * u - 0.1 * u * u) * s, up = 0.34 * u * s - 0.01, fw = 0.04 * u * s;
                        int base = Ui.mix(col(0x2A1B1B), col(0xE0402A), (float) (u * u));
                        cone(c, cam, x + rx * sd * lat + fx * fw, y + up, z + rz * sd * lat + fz * fw, 0.05 * s * (1 - 0.82 * u), 0.046 * s * (1 - 0.85 * u),
                                0.045 * s, i % 2 == 0 ? base : Ui.shade(base, 0.8f), base);
                    }
                    double lat = (0.15 + 0.2 - 0.1) * s;
                    soft(c, cam, x + rx * sd * lat + fx * 0.04 * s, y + 0.34 * s, z + rz * sd * lat + fz * 0.04 * s, 0.012f * s, 0xFF7A3A,
                            0.7f + 0.3f * (float) Math.sin(clock * 5 + sd));
                }
            }
            case FLOWERS -> {
                int n = 16;
                int[] petals = {0xFFFF8FB5, 0xFFFFFFFF, 0xFFFFD866, 0xFFB58CFF, 0xFFFF7A7A};
                for (int pass = 0; pass < 2; pass++) {
                    for (int k = 0; k < n; k++) {
                        double a = k * Math.PI * 2 / n, ca = Math.cos(a), sa = Math.sin(a);
                        boolean front = facing(cam, x, z, ca, sa);
                        if ((pass == 0) == front) continue;
                        double px = x + ca * 0.27 * s, pz = z + sa * 0.27 * s, py = y + 0.02 * s + Math.sin(a * 3) * 0.01;
                        if (k % 2 == 1) {
                            dot(c, cam, px, py, pz, 0.022f * s, 0xFF3FA050);
                            dot(c, cam, px + ca * 0.02 * s, py, pz + sa * 0.02 * s, 0.015f * s, 0xFF2E8040);
                        } else {
                            int pc = petals[(k / 2) % petals.length];
                            for (int q = 0; q < 5; q++) {
                                double qa = q * Math.PI * 2 / 5 + clock * 0.3;
                                dot(c, cam, px + (-sa * Math.cos(qa) + 0) * 0.02 * s, py + Math.sin(qa) * 0.02 * s, pz + ca * Math.cos(qa) * 0.02 * s, 0.014f * s, pc);
                            }
                            dot(c, cam, px, py, pz, 0.012f * s, 0xFFFFC433);
                        }
                    }
                }
            }
            default -> { }
        }
    }

    // -------------------------------------------------- рюкзаки и крылья
    private static void pack(DrawContext c, Fx.Cam cam, Pack pk, Vec3d pos, ClientPlayerEntity p, float clock) {
        double th = bodyYaw * 0.017453292;
        double[] f = {-Math.sin(th), 0, Math.cos(th)};
        double[] r = {-Math.cos(th), 0, -Math.sin(th)};
        double[] back = {-f[0], 0, -f[2]};

        double toX = cam.x - pos.x, toZ = cam.z - pos.z, len = Math.hypot(toX, toZ) + 1e-6;
        if ((toX * back[0] + toZ * back[2]) / len < -0.35) return; // камера спереди — спина закрыта телом

        double cy = pos.y + 1.1 - (p.isSneaking() ? 0.14 : 0.0);
        double bx = pos.x, bz = pos.z;

        switch (pk) {
            case SCHOOL -> {
                double d = 0.125 + 0.11;
                box(c, cam, bx + back[0] * d, cy, bz + back[2] * d, 0.17, 0.24, 0.11, r, f, col(0x2F6FDB));
                double d2 = 0.125 + 0.22 + 0.04;
                box(c, cam, bx + back[0] * d2, cy - 0.1, bz + back[2] * d2, 0.12, 0.09, 0.05, r, f, col(0x234FA0));
            }
            case HIKER -> {
                double d = 0.125 + 0.13;
                box(c, cam, bx + back[0] * d, cy + 0.02, bz + back[2] * d, 0.19, 0.3, 0.13, r, f, col(0x4E6B3A));
                box(c, cam, bx + back[0] * d, cy + 0.38, bz + back[2] * d, 0.21, 0.07, 0.13, r, f, col(0xB5651D));
                for (int sd = -1; sd <= 1; sd += 2) {
                    box(c, cam, bx + back[0] * (d - 0.02) + r[0] * sd * 0.25, cy - 0.12, bz + back[2] * (d - 0.02) + r[2] * sd * 0.25,
                            0.05, 0.1, 0.08, r, f, col(0x35502A));
                }
            }
            case JETPACK -> {
                double d = 0.125 + 0.07;
                box(c, cam, bx + back[0] * d, cy, bz + back[2] * d, 0.11, 0.2, 0.04, r, f, col(0x3A3F4B));
                for (int sd = -1; sd <= 1; sd += 2) {
                    double ex = bx + back[0] * (d + 0.08) + r[0] * sd * 0.09, ez = bz + back[2] * (d + 0.08) + r[2] * sd * 0.09;
                    box(c, cam, ex, cy, ez, 0.06, 0.26, 0.07, r, f, col(0xB0B7C3));
                    double fl = 0.2 + 0.08 * Math.sin(clock * 30 + sd);
                    double yb = cy - 0.26;
                    tri3(c, cam, new double[]{ex + r[0] * 0.05, yb, ez + r[2] * 0.05}, new double[]{ex - r[0] * 0.05, yb, ez - r[2] * 0.05},
                            new double[]{ex, yb - fl, ez}, 0xFFFF8A1F);
                    tri3(c, cam, new double[]{ex + r[0] * 0.025, yb, ez + r[2] * 0.025}, new double[]{ex - r[0] * 0.025, yb, ez - r[2] * 0.025},
                            new double[]{ex, yb - fl * 0.6, ez}, 0xFFFFE066);
                }
            }
            case WINGS_ANGEL -> wings(c, cam, bx, cy + 0.2, bz, r, back, p, clock, false);
            case WINGS_DEMON -> wings(c, cam, bx, cy + 0.2, bz, r, back, p, clock, true);
            default -> { }
        }
    }

    private static void wings(DrawContext c, Fx.Cam cam, double bx, double by, double bz, double[] r, double[] back,
                              ClientPlayerEntity p, float clock, boolean demon) {
        boolean ground = p.isOnGround();
        double amp = ground ? 0.12 : 0.55, rate = ground ? 1.6 : 6.5;
        double flap = Math.sin(clock * rate);
        double phi = 0.4 + amp * flap * 0.6;
        double lift = amp * flap * 0.5;
        double cp = Math.cos(phi), sp = Math.sin(phi);
        for (int sd = -1; sd <= 1; sd += 2) {
            double[] out = {r[0] * sd, 0, r[2] * sd};
            double[] S = {bx + back[0] * 0.17 + out[0] * 0.08, by, bz + back[2] * 0.17 + out[2] * 0.08};
            double[] o = {out[0] * cp + back[0] * sp, 0, out[2] * cp + back[2] * sp};
            if (demon) demonWing(c, cam, S, o, back, lift, clock);
            else angelWing(c, cam, S, o, back, lift, clock, sd);
        }
    }

    private static int wingCol(int base) { return themed() ? Ui.mix(base, Ui.accent(), 0.55f) : base; }

    private static void feather(DrawContext c, Fx.Cam cam, double[] root, double[] dir, double len, double width, double[] back, int color) {
        double[] wv = norm(cross(back, dir));
        double[] bl = ma(root, wv, width * 0.5), br = ma(root, wv, -width * 0.5), tip = ma(root, dir, len);
        tri3(c, cam, bl, br, tip, color);
    }

    private static void angelWing(DrawContext c, Fx.Cam cam, double[] S, double[] o, double[] back, double lift, float clock, int sd) {
        int n = 9;
        double[] up = {UP_X, UP_Y, UP_Z};
        double[][] att = new double[n][];
        double[][] dirs = new double[n][];
        double[] lens = new double[n];
        for (int i = 0; i < n; i++) {
            double u = 0.08 + 0.92 * i / (n - 1);
            double[] a = ma(ma(S, o, u * 0.95), up, 0.5 * Math.sin(u * Math.PI * 0.75) + 0.2 * u + lift * u);
            double[] d = ma(ma(ma(new double[]{0, 0, 0}, o, 0.4 + 0.9 * u), up, -(0.9 - 0.5 * u) + lift * 0.5), back, 0.15);
            att[i] = a; dirs[i] = norm(d); lens[i] = 0.4 + 0.72 * u;
        }
        double[] mul = {1.0, 0.62, 0.34};
        int[][] cols = {{0xFFDDE0F4, 0xFFC9CEEB}, {0xFFF2F3FF, 0xFFE4E7F8}, {0xFFFFFFFF, 0xFFF6F7FF}};
        for (int layer = 0; layer < 3; layer++) {
            for (int i = 0; i < n; i++) {
                feather(c, cam, att[i], dirs[i], lens[i] * mul[layer], 0.13 - 0.02 * layer, back, wingCol(cols[layer][i % 2]));
            }
        }
        // косточка и светящиеся кончики
        for (int k = 0; k <= 10; k++) {
            double u = 0.08 + 0.92 * k / 10.0;
            double[] a = ma(ma(S, o, u * 0.95), up, 0.5 * Math.sin(u * Math.PI * 0.75) + 0.2 * u + lift * u);
            dot(c, cam, a[0], a[1], a[2], 0.011f, wingCol(0xFFE8C96A));
        }
        for (int i = 2; i < n; i += 2) {
            double[] tip = ma(att[i], dirs[i], lens[i]);
            soft(c, cam, tip[0], tip[1], tip[2], 0.012f, Ui.accent(i * 0.05f) & 0xFFFFFF, 0.55f);
        }
        // искорки
        for (int k = 0; k < 6; k++) {
            double ph = (clock * 0.45 + k * 0.17 + (sd > 0 ? 0.08 : 0)) % 1.0;
            double[] pos = ma(ma(S, o, 0.4 + 0.6 * (k / 6.0)), up, 0.4 - ph * 0.9);
            soft(c, cam, pos[0], pos[1], pos[2], 0.01f, 0xFFFFFF, (float) Math.sin(ph * Math.PI));
        }
    }

    private static void demonWing(DrawContext c, Fx.Cam cam, double[] S, double[] o, double[] back, double lift, float clock) {
        double[] up = {UP_X, UP_Y, UP_Z};
        double[] E = ma(ma(S, o, 0.28), up, 0.38 + lift * 0.3);
        double[] W = ma(ma(S, o, 0.62), up, 0.52 + lift * 0.6);
        double[] Slow = ma(ma(S, up, -0.42), back, 0.02);
        double[] angs = {38, 12, -18, -48};
        double[] lens = {0.8, 1.0, 0.95, 0.7};
        double[][] T = new double[4][];
        for (int j = 0; j < 4; j++) {
            double a = Math.toRadians(angs[j]);
            double[] d = norm(ma(ma(ma(new double[]{0, 0, 0}, o, Math.cos(a)), up, Math.sin(a) + lift * 0.5), back, 0.1));
            T[j] = ma(W, d, lens[j]);
        }
        // перепонка со «скалопами»
        for (int j = 0; j < 3; j++) {
            double[] mid = ma(ma(T[j], T[j + 1], 1.0), new double[]{0, 0, 0}, 0);
            double[] M = {(T[j][0] + T[j + 1][0]) * 0.5, (T[j][1] + T[j + 1][1]) * 0.5, (T[j][2] + T[j + 1][2]) * 0.5};
            M = ma(M, ma(W, M, -1.0), 0.28);
            int mc1 = wingCol(j % 2 == 0 ? 0xFF7A1020 : 0xFF5E0C19);
            int mc2 = wingCol(j % 2 == 0 ? 0xFF8E1526 : 0xFF6E1020);
            tri3(c, cam, Slow, T[j], M, mc1);
            tri3(c, cam, Slow, M, T[j + 1], mc2);
        }
        tri3(c, cam, S, W, T[0], wingCol(0xFF8A1626));
        tri3(c, cam, S, T[0], Slow, wingCol(0xFF701020));
        // кости и коготь
        int bone = wingCol(0xFF2A0A0F);
        line(c, cam, S, E, 0.016f, bone, 8);
        line(c, cam, E, W, 0.014f, bone, 8);
        for (int j = 0; j < 4; j++) line(c, cam, W, T[j], 0.009f, bone, 10);
        dot(c, cam, W[0], W[1], W[2], 0.022f, 0xFFE8DCC0);
        for (int j = 0; j < 4; j++) {
            soft(c, cam, T[j][0], T[j][1], T[j][2], 0.012f, 0xFF7A3A, 0.6f + 0.4f * (float) Math.sin(clock * 4 + j * 1.3));
        }
    }
}
