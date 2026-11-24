package dev.netrixvisual;

import dev.netrixvisual.VisualsConfig.Hat;
import dev.netrixvisual.VisualsConfig.Pack;
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
    private static final int GOLD0 = 0xFFB8902A, GOLD1 = 0xFFF2D35C;

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
        float diff = MathHelper.wrapDegrees(target - bodyYaw);
        bodyYaw += diff * Math.min(1f, dt * 18f);

        float headTop = p.getHeight() - 0.06f;
        if (cfg.hat != Hat.NONE) hat(c, cam, cfg.hat, pos.x, pos.y + headTop, pos.z, p.getYaw(), cfg.hatScale, clock);
        if (cfg.pack != Pack.NONE) pack(c, cam, cfg.pack, pos, p, clock);
    }

    // ------------------------------------------------------------ примитивы
    private static void dot(DrawContext c, Fx.Cam cam, double x, double y, double z, float size, int color) {
        if (!cam.project(x, y, z, t)) return;
        int r = Math.max(1, Math.round(size * t[3]));
        Ui.disc(c, Math.round(t[0]), Math.round(t[1]), r, color);
    }

    private static void ellipse(DrawContext c, Fx.Cam cam, double x, double y, double z, double r, int color) {
        if (!cam.project(x, y, z, t)) return;
        float cx = t[0], cy = t[1], rx = (float) (r * t[3]);
        double dx = cam.x - x, dy = cam.y - y, dz = cam.z - z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        float ry = Math.max(0.8f, rx * (float) (Math.abs(dy) / dist));
        int step = ry > 14 ? 2 : 1;
        int y0 = (int) Math.floor(cy - ry), y1 = (int) Math.ceil(cy + ry);
        for (int yy = y0; yy <= y1; yy += step) {
            float k = (yy + 0.5f * step - cy) / ry;
            if (k < -1f || k > 1f) continue;
            float hw = rx * (float) Math.sqrt(1f - k * k);
            c.fill(Math.round(cx - hw), yy, Math.round(cx + hw), yy + step, color);
        }
    }

    /** Усечённый конус/цилиндр из стопки эллипсов (число слоёв зависит от размера на экране). */
    private static void cone(DrawContext c, Fx.Cam cam, double x, double yb, double z, double r0, double r1, double h, int c0, int c1) {
        if (!cam.project(x, yb, z, t)) return;
        int n = MathHelper.clamp((int) (h * t[3] / 1.5f), 3, 26);
        boolean above = cam.y > yb + h * 0.5;
        for (int i = 0; i < n; i++) {
            int idx = above ? i : n - 1 - i;
            float f = idx / (float) (n - 1);
            ellipse(c, cam, x, yb + f * h, z, r0 + (r1 - r0) * f, Ui.mix(c0, c1, f));
        }
    }

    private static void tri3(DrawContext c, Fx.Cam cam, double[] a, double[] b, double[] d, int color) {
        float[] pa = new float[2], pb = new float[2], pd = new float[2];
        if (!cam.project(a[0], a[1], a[2], t)) return; pa[0] = t[0]; pa[1] = t[1];
        if (!cam.project(b[0], b[1], b[2], t)) return; pb[0] = t[0]; pb[1] = t[1];
        if (!cam.project(d[0], d[1], d[2], t)) return; pd[0] = t[0]; pd[1] = t[1];
        Ui.tri(c, pa[0], pa[1], pb[0], pb[1], pd[0], pd[1], color);
    }

    private static final int[][] FACES = {{1, 3, 7, 5}, {0, 4, 6, 2}, {2, 6, 7, 3}, {0, 1, 5, 4}, {4, 5, 7, 6}, {0, 2, 3, 1}};

    /**
     * Параллелепипед: r — вектор «вправо», f — вектор «вперёд» (по телу), вверх — мировой Y.
     * Рисуются только видимые грани, с простым освещением.
     */
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
        switch (h) {
            case SAMURAI -> {
                cone(c, cam, x, y - 0.012 * s, z, 0.49 * s, 0.49 * s, 0.022 * s, col(0xB8902A), col(0xF2D35C));
                cone(c, cam, x, y, z, 0.46 * s, 0.06 * s, 0.18 * s, col(0x17171B), col(0x3E3E4C));
                cone(c, cam, x, y + 0.046 * s, z, 0.352 * s, 0.352 * s, 0.014 * s, col(0xB22222), col(0xD63A3A));
                cone(c, cam, x, y + 0.178 * s, z, 0.035 * s, 0.018 * s, 0.045 * s, col(0xB8902A), col(0xF2D35C));
                dot(c, cam, x, y + 0.235 * s, z, 0.013f * s, col(0xF2D35C));
            }
            case WIZARD -> {
                cone(c, cam, x, y - 0.01 * s, z, 0.40 * s, 0.40 * s, 0.02 * s, col(0x3B2266), col(0x4E2F8A));
                cone(c, cam, x, y, z, 0.21 * s, 0.0, 0.55 * s, col(0x4B2A8F), col(0x8A5CE0));
                cone(c, cam, x, y + 0.03 * s, z, 0.205 * s, 0.205 * s, 0.035 * s, col(0xB8902A), col(0xF2D35C));
                dot(c, cam, x, y + 0.56 * s, z, 0.022f * s, col(0xF2D35C));
            }
            case TOPHAT -> {
                cone(c, cam, x, y - 0.01 * s, z, 0.31 * s, 0.31 * s, 0.022 * s, col(0x0E0E10), col(0x1C1C20));
                cone(c, cam, x, y, z, 0.19 * s, 0.19 * s, 0.30 * s, col(0x151517), col(0x26262B));
                cone(c, cam, x, y + 0.012 * s, z, 0.196 * s, 0.196 * s, 0.05 * s, col(0x9B1C1C), col(0xB82828));
                ellipse(c, cam, x, y + 0.302 * s, z, 0.19 * s, col(0x34343A));
            }
            case PARTY -> {
                cone(c, cam, x, y, z, 0.19 * s, 0.0, 0.42 * s, col(0xFF4FA0), col(0x4FD0FF));
                dot(c, cam, x, y + 0.43 * s, z, 0.03f * s, col(0xFFE14D));
            }
            case SANTA -> {
                cone(c, cam, x, y, z, 0.2 * s, 0.045 * s, 0.34 * s, col(0xC81E1E), col(0xE04545));
                cone(c, cam, x, y - 0.008 * s, z, 0.215 * s, 0.215 * s, 0.06 * s, col(0xE8E8EE), col(0xFFFFFF));
                dot(c, cam, x, y + 0.36 * s, z, 0.05f * s, 0xFFFFFFFF);
            }
            case CROWN -> {
                cone(c, cam, x, y, z, 0.2 * s, 0.2 * s, 0.09 * s, col(0xB8902A), col(0xF6D860));
                for (int i = 0; i < 5; i++) {
                    double a = i * Math.PI * 2 / 5;
                    double sx = x + Math.cos(a) * 0.2 * s, sz = z + Math.sin(a) * 0.2 * s;
                    cone(c, cam, sx, y + 0.085 * s, sz, 0.04 * s, 0.0, 0.11 * s, col(0xD4AF37), col(0xFFE680));
                    dot(c, cam, sx, y + 0.205 * s, sz, 0.012f * s, i % 2 == 0 ? col(0xE02A4A) : col(0x2A9BE0));
                }
            }
            case HALO -> {
                double yy = y + 0.3 * s + Math.sin(clock * 2.0) * 0.015;
                for (int i = 0; i < 30; i++) {
                    double a = i * Math.PI * 2 / 30 + clock * 0.8;
                    dot(c, cam, x + Math.cos(a) * 0.27 * s, yy, z + Math.sin(a) * 0.27 * s, 0.017f * s, col(0xFFE680));
                }
            }
            case CAT_EARS -> {
                double th = headYaw * 0.017453292;
                double rx = -Math.cos(th), rz = -Math.sin(th), fx = -Math.sin(th), fz = Math.cos(th);
                for (int sd = -1; sd <= 1; sd += 2) {
                    double[] a = {x + rx * sd * 0.06 * s, y - 0.02, z + rz * sd * 0.06 * s};
                    double[] b = {x + rx * sd * 0.26 * s, y - 0.02, z + rz * sd * 0.26 * s};
                    double[] tip = {x + rx * sd * 0.22 * s + fx * 0.01, y + 0.2 * s, z + rz * sd * 0.22 * s + fz * 0.01};
                    tri3(c, cam, a, b, tip, col(0x1B1B1F));
                    double[] a2 = {x + rx * sd * 0.10 * s + fx * 0.012, y - 0.01, z + rz * sd * 0.10 * s + fz * 0.012};
                    double[] b2 = {x + rx * sd * 0.22 * s + fx * 0.012, y - 0.01, z + rz * sd * 0.22 * s + fz * 0.012};
                    double[] tip2 = {x + rx * sd * 0.20 * s + fx * 0.012, y + 0.14 * s, z + rz * sd * 0.20 * s + fz * 0.012};
                    tri3(c, cam, a2, b2, tip2, col(0xFF8FB5));
                }
            }
            case HORNS -> {
                double th = headYaw * 0.017453292;
                double rx = -Math.cos(th), rz = -Math.sin(th);
                for (int sd = -1; sd <= 1; sd += 2) {
                    for (int i = 0; i < 7; i++) {
                        double u = i / 6.0;
                        double lat = (0.17 + 0.16 * u - 0.07 * u * u) * s, up = 0.3 * u * s - 0.01;
                        cone(c, cam, x + rx * sd * lat, y + up, z + rz * sd * lat, 0.045 * s * (1 - 0.7 * u), 0.04 * s * (1 - 0.8 * u), 0.05 * s,
                                col(0xE8DCC0), col(0xB8503C));
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
        if ((toX * back[0] + toZ * back[2]) / len < -0.35) return; // камера спереди — рюкзак закрыт телом

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
        double flap = p.isOnGround() ? 0.18 : 0.5;
        double phi = 0.35 + flap * Math.sin(clock * (p.isOnGround() ? 2.0 : 6.0));
        int n = 8;
        for (int sd = -1; sd <= 1; sd += 2) {
            double rx0 = bx + back[0] * 0.17 + r[0] * sd * 0.07, rz0 = bz + back[2] * 0.17 + r[2] * sd * 0.07;
            double[][] tips = new double[n][3];
            for (int i = 0; i < n; i++) {
                double u = i / (double) (n - 1);
                double alpha = Math.toRadians(75 - 100 * u);
                double len = demon ? (i % 2 == 0 ? 0.95 : 0.65) * (0.8 + 0.3 * Math.sin(u * Math.PI)) : (0.55 + 0.55 * Math.sin(u * Math.PI * 0.9 + 0.2));
                double out = Math.cos(alpha) * Math.cos(phi), bk = Math.cos(alpha) * Math.sin(phi), up = Math.sin(alpha);
                tips[i][0] = rx0 + (r[0] * sd * out + back[0] * bk) * len;
                tips[i][1] = by + up * len;
                tips[i][2] = rz0 + (r[2] * sd * out + back[2] * bk) * len;
            }
            double[] root = {rx0, by, rz0};
            for (int i = 0; i < n - 1; i++) {
                int base = demon ? (i % 2 == 0 ? 0xFF6B0F1A : 0xFF8A1624) : (i % 2 == 0 ? 0xFFF5F5FF : 0xFFDADCEE);
                int colr = themed() ? Ui.mix(base, Ui.accent(), 0.6f) : base;
                tri3(c, cam, root, tips[i], tips[i + 1], colr);
            }
        }
    }
}
