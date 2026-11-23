package dev.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Собственные визуальные эффекты (не ванильные частицы):
 * кольца/нимб/орбы вокруг цели, взрывы при ударе, цифры урона.
 * Всё считается в 3D и проецируется на экран вручную.
 */
public final class Fx {
    private static final Random R = new Random();
    private static final int SPARK = 0, STAR = 1, DISC = 2;

    private static final class P {
        double x, y, z, vx, vy, vz;
        float life, max, size, drag, grav;
        int color, shape;
    }

    private static final class FT {
        double x, y, z;
        String s;
        float life, max;
        int color;
    }

    private static final List<P> parts = new ArrayList<>();
    private static final List<FT> texts = new ArrayList<>();

    private static Vec3d camPrev, camCur;
    private static long tickNs = System.nanoTime();
    private static Entity tgt;
    private static Vec3d tPrev, tCur;
    private static boolean tgtOn;
    private static float tAlpha;
    private static Entity watch;
    private static float watchHp;
    private static long watchUntil;
    private static long lastNs;
    private static float clock;
    private static final float[] tmp = new float[4];

    private static Vec3d pos(Entity e) { return new Vec3d(e.getX(), e.getY(), e.getZ()); }

    private static Vec3d lerp(Vec3d a, Vec3d b, float t) {
        return new Vec3d(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t);
    }

    // ------------------------------------------------------------------ tick
    public static void tick(MinecraftClient mc) {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null) {
            parts.clear(); texts.clear();
            tgt = null; watch = null; camPrev = camCur = null; tAlpha = 0;
            return;
        }
        camPrev = camCur;
        camCur = pos(p);
        if (camPrev == null) camPrev = camCur;
        tickNs = System.nanoTime();

        VisualsConfig c = VisualsConfig.I;
        Entity t = mc.targetedEntity;
        boolean valid = c.targetFx && t instanceof LivingEntity && t.isAlive() && t != p
                && (!c.targetOnlyPlayers || t instanceof PlayerEntity);
        if (valid) {
            if (t != tgt) { tgt = t; tCur = tPrev = pos(t); }
            else { tPrev = tCur; tCur = pos(t); }
            tgtOn = true;
        } else {
            tgtOn = false;
            if (tgt != null) {
                if (tgt.isRemoved() || !tgt.isAlive()) tgt = null;
                else { tPrev = tCur; tCur = pos(tgt); }
            }
        }

        // цифры урона
        if (watch != null) {
            if (watch instanceof LivingEntity le && !watch.isRemoved()) {
                float hp = le.getHealth();
                if (hp < watchHp - 0.01f && c.damageNumbers) {
                    spawnText(watch, String.format("-%.1f", watchHp - hp), 0xFFFF5A4A);
                }
                if (hp != watchHp) watchHp = hp;
            }
            if (System.currentTimeMillis() > watchUntil || watch.isRemoved()) watch = null;
        }
    }

    public static void watch(Entity e) {
        if (e instanceof LivingEntity le) {
            if (watch != e) watchHp = le.getHealth();
            watch = e;
            watchUntil = System.currentTimeMillis() + 1500;
        }
    }

    private static void spawnText(Entity e, String s, int color) {
        FT f = new FT();
        f.x = e.getX() + (R.nextDouble() - 0.5) * 0.5;
        f.y = e.getY() + e.getHeight() + 0.1;
        f.z = e.getZ() + (R.nextDouble() - 0.5) * 0.5;
        f.s = s; f.color = color; f.life = f.max = 1.1f;
        if (texts.size() < 30) texts.add(f);
    }

    // ----------------------------------------------------------------- burst
    private static void spawn(double x, double y, double z, double vx, double vy, double vz,
                              float life, float size, int color, int shape, float drag, float grav) {
        if (parts.size() > 500) return;
        P p = new P();
        p.x = x; p.y = y; p.z = z; p.vx = vx; p.vy = vy; p.vz = vz;
        p.life = p.max = life; p.size = size; p.color = color; p.shape = shape; p.drag = drag; p.grav = grav;
        parts.add(p);
    }

    public static void burst(Entity e) {
        VisualsConfig c = VisualsConfig.I;
        if (c.hitFx == VisualsConfig.HitFx.OFF) return;
        double x = e.getX(), y = e.getY() + e.getHeight() * 0.6, z = e.getZ();
        int n = c.hitFxCount;
        switch (c.hitFx) {
            case SPARKS -> {
                for (int i = 0; i < n; i++) {
                    double[] d = randDir();
                    double sp = 2.0 + R.nextDouble() * 3.5;
                    int col = R.nextInt(4) == 0 ? 0xFFFFFFFF : Ui.accent(R.nextFloat() * 0.3f);
                    spawn(x, y, z, d[0] * sp, d[1] * sp + 1.0, d[2] * sp, 0.45f + R.nextFloat() * 0.4f,
                            0.018f + R.nextFloat() * 0.018f, col, SPARK, 2.5f, 9f);
                }
            }
            case STARS -> {
                for (int i = 0; i < n; i++) {
                    double[] d = randDir();
                    double sp = 1.2 + R.nextDouble() * 2.6;
                    spawn(x, y, z, d[0] * sp, d[1] * sp + 0.6, d[2] * sp, 0.7f + R.nextFloat() * 0.5f,
                            0.03f + R.nextFloat() * 0.03f, Ui.accent(R.nextFloat() * 0.6f), STAR, 2.0f, 2.5f);
                }
            }
            case SHOCKWAVE -> {
                int ring = Math.max(16, n + 8);
                for (int i = 0; i < ring; i++) {
                    double a = i * Math.PI * 2 / ring;
                    spawn(x, y, z, Math.cos(a) * 3.6, 0, Math.sin(a) * 3.6, 0.5f, 0.04f,
                            Ui.accent(i / (float) ring * 0.5f), DISC, 4.2f, 0f);
                }
                spawn(x, y, z, 0, 0, 0, 0.18f, 0.3f, 0xFFFFFFFF, DISC, 0f, 0f);
            }
            case SOULS -> {
                for (int i = 0; i < n; i++) {
                    spawn(x + (R.nextDouble() - 0.5) * 0.6, y - 0.4 + R.nextDouble() * 0.8, z + (R.nextDouble() - 0.5) * 0.6,
                            (R.nextDouble() - 0.5) * 0.5, 1.2 + R.nextDouble() * 1.8, (R.nextDouble() - 0.5) * 0.5,
                            0.9f + R.nextFloat() * 0.7f, 0.04f + R.nextFloat() * 0.03f,
                            Ui.accent(R.nextFloat() * 0.4f), DISC, 0.6f, -0.5f);
                }
            }
            default -> {}
        }
    }

    private static double[] randDir() {
        double u = R.nextDouble() * 2 - 1, a = R.nextDouble() * Math.PI * 2, s = Math.sqrt(1 - u * u);
        return new double[]{s * Math.cos(a), u, s * Math.sin(a)};
    }

    // ----------------------------------------------------------------- камера
    private static final class Cam {
        double x, y, z;
        float fx, fy, fz, rx, rz, ux, uy, uz, tanH, aspect;
        int sw, sh;

        boolean project(double wx, double wy, double wz, float[] o) {
            double dx = wx - x, dy = wy - y, dz = wz - z;
            double depth = dx * fx + dy * fy + dz * fz;
            if (depth < 0.1 || depth > 80) return false;
            double sxv = dx * rx + dz * rz;
            double syv = dx * ux + dy * uy + dz * uz;
            o[0] = (float) (sw * 0.5 + (sxv / (depth * tanH * aspect)) * sw * 0.5);
            o[1] = (float) (sh * 0.5 - (syv / (depth * tanH)) * sh * 0.5);
            o[2] = (float) depth;
            o[3] = (float) (sh * 0.5 / (depth * tanH));
            return true;
        }
    }

    private static Cam makeCam(MinecraftClient mc, float pt) {
        ClientPlayerEntity p = mc.player;
        Cam c = new Cam();
        Vec3d cp = lerp(camPrev, camCur, pt);
        c.x = cp.x; c.y = cp.y + (p.getEyeY() - p.getY()); c.z = cp.z;
        float yaw = p.getYaw() * 0.017453292f, pitch = p.getPitch() * 0.017453292f;
        float sy = MathHelper.sin(yaw), cy = MathHelper.cos(yaw), sp = MathHelper.sin(pitch), cpch = MathHelper.cos(pitch);
        c.fx = -sy * cpch; c.fy = -sp; c.fz = cy * cpch;
        c.rx = -cy; c.rz = -sy;
        c.ux = -sy * sp; c.uy = cpch; c.uz = cy * sp;
        c.sw = mc.getWindow().getScaledWidth();
        c.sh = mc.getWindow().getScaledHeight();
        int fov = mc.options.getFov().getValue();
        c.tanH = (float) Math.tan(Math.toRadians(fov) * 0.5);
        c.aspect = (float) c.sw / c.sh;
        return c;
    }

    // ----------------------------------------------------------------- кадр
    public static void frame(DrawContext ctx, MinecraftClient mc) {
        long now = System.nanoTime();
        float dt = lastNs == 0 ? 0.016f : Math.min(0.05f, (now - lastNs) / 1e9f);
        lastNs = now;
        clock += dt;
        if (camCur == null || mc.player == null) return;

        float target = tgtOn ? 1f : 0f;
        tAlpha += (target - tAlpha) * (1f - (float) Math.exp(-dt * 9f));
        if (tAlpha < 0.01f) tAlpha = 0f;

        for (int i = parts.size() - 1; i >= 0; i--) {
            P p = parts.get(i);
            p.life -= dt;
            if (p.life <= 0) { parts.remove(i); continue; }
            float k = Math.max(0f, 1f - p.drag * dt);
            p.vx *= k; p.vy *= k; p.vz *= k;
            p.vy -= p.grav * dt;
            p.x += p.vx * dt; p.y += p.vy * dt; p.z += p.vz * dt;
        }
        for (int i = texts.size() - 1; i >= 0; i--) {
            FT f = texts.get(i);
            f.life -= dt;
            if (f.life <= 0) { texts.remove(i); continue; }
            f.y += (0.4 + 1.2 * (f.life / f.max)) * dt;
        }

        if (!mc.options.getPerspective().isFirstPerson()) return;

        float pt = MathHelper.clamp((now - tickNs) / 50_000_000f, 0f, 1f);
        Cam cam = makeCam(mc, pt);

        if (tgt != null && tAlpha > 0.02f && tPrev != null) drawTarget(ctx, cam, pt, tAlpha);

        for (P p : parts) drawParticle(ctx, cam, p);

        TextRenderer tr = mc.textRenderer;
        for (FT f : texts) {
            if (!cam.project(f.x, f.y, f.z, tmp)) continue;
            float a = Math.min(1f, f.life / f.max * 2.2f);
            if (a < 0.05f) continue;
            int col = Ui.argb(f.color, a);
            ctx.drawTextWithShadow(tr, f.s, Math.round(tmp[0]) - tr.getWidth(f.s) / 2, Math.round(tmp[1]), col);
        }
    }

    // ------------------------------------------------------------- рисование
    private static void glow(DrawContext c, Cam cam, double x, double y, double z, float size, int color, float alpha) {
        if (alpha <= 0.03f) return;
        if (!cam.project(x, y, z, tmp)) return;
        int px = Math.round(tmp[0]), py = Math.round(tmp[1]);
        if (px < -20 || py < -20 || px > cam.sw + 20 || py > cam.sh + 20) return;
        float r = Math.max(1f, size * tmp[3]);
        int ri = Math.min(12, Math.round(r));
        if (ri <= 1) {
            c.fill(px - 2, py - 2, px + 2, py + 2, Ui.argb(color, alpha * 0.25f));
            c.fill(px - 1, py - 1, px + 1, py + 1, Ui.argb(color, alpha));
            return;
        }
        Ui.disc(c, px, py, Math.min(16, Math.round(r * 1.8f)), Ui.argb(color, alpha * 0.2f));
        Ui.disc(c, px, py, ri, Ui.argb(color, alpha * 0.85f));
        Ui.disc(c, px, py, Math.max(1, ri / 2), Ui.argb(0xFFFFFF, alpha * 0.9f));
    }

    private static void drawParticle(DrawContext c, Cam cam, P p) {
        float a = Math.min(1f, p.life / p.max * 1.6f);
        if (p.shape == DISC) { glow(c, cam, p.x, p.y, p.z, p.size * (0.6f + 0.4f * a), p.color, a); return; }
        if (!cam.project(p.x, p.y, p.z, tmp)) return;
        int px = Math.round(tmp[0]), py = Math.round(tmp[1]);
        float r = Math.max(1f, p.size * tmp[3]);
        if (p.shape == SPARK) {
            int ri = Math.max(1, Math.round(r * a + 0.4f));
            c.fill(px - ri - 1, py - ri - 1, px + ri + 1, py + ri + 1, Ui.argb(p.color, a * 0.25f));
            c.fill(px - ri, py - ri, px + ri, py + ri, Ui.argb(p.color, a));
            c.fill(px - ri / 2, py - ri / 2, px + (ri + 1) / 2, py + (ri + 1) / 2, Ui.argb(0xFFFFFF, a));
        } else {
            int L = Math.max(2, Math.round(r * 2.2f * (0.5f + 0.5f * a)));
            int col = Ui.argb(p.color, a);
            c.fill(px - L, py, px + L + 1, py + 1, col);
            c.fill(px, py - L, px + 1, py + L + 1, col);
            int L2 = L / 2;
            c.fill(px - L2, py - L2, px - L2 + 1, py - L2 + 1, Ui.argb(p.color, a * 0.7f));
            c.fill(px + L2, py - L2, px + L2 + 1, py - L2 + 1, Ui.argb(p.color, a * 0.7f));
            c.fill(px - L2, py + L2, px - L2 + 1, py + L2 + 1, Ui.argb(p.color, a * 0.7f));
            c.fill(px + L2, py + L2, px + L2 + 1, py + L2 + 1, Ui.argb(p.color, a * 0.7f));
            c.fill(px - 1, py - 1, px + 2, py + 2, Ui.argb(0xFFFFFF, a));
        }
    }

    private static void drawTarget(DrawContext c, Cam cam, float pt, float al) {
        VisualsConfig cfg = VisualsConfig.I;
        Vec3d pos = lerp(tPrev, tCur, pt);
        float w = tgt.getWidth(), h = tgt.getHeight();
        float t = clock * cfg.fxSpeed, sz = cfg.fxSize;
        switch (cfg.targetFxMode) {
            case RINGS -> rings(c, cam, pos, w, h, t, sz, al);
            case HALO -> halo(c, cam, pos, w, h, t, sz, al);
            case ORBS -> orbs(c, cam, pos, w, h, t, sz, al);
            case SPIRAL -> spiral(c, cam, pos, w, h, t, sz, al);
            case FLOOR -> floor(c, cam, pos, w, h, t, sz, al);
        }
    }

    private static void rings(DrawContext c, Cam cam, Vec3d pos, float w, float h, float t, float sz, float al) {
        double R0 = (w * 0.5 + 0.5) * sz, mid = pos.y + h * 0.5;
        for (int k = 0; k < 3; k++) {
            double tilt = Math.toRadians(25 + k * 55), spin = t * (0.5 + 0.25 * k) + k * 2.1;
            double dir = (k % 2 == 0) ? 1 : -1;
            double head = t * (2.2 + 0.5 * k) * dir;
            double ct = Math.cos(tilt), st = Math.sin(tilt), cs = Math.cos(spin), ss = Math.sin(spin);
            int N = 24;
            for (int i = 0; i < N; i++) {
                float b = 1f - (float) i / N;
                double ang = head - dir * i * 0.16;
                double lx = Math.cos(ang) * R0, lz = Math.sin(ang) * R0;
                double y1 = -lz * st, z1 = lz * ct;
                double x2 = lx * cs + z1 * ss, z2 = -lx * ss + z1 * cs;
                int col = Ui.mix(Ui.accent(k * 0.15f + i * 0.004f), 0xFFFFFFFF, b * 0.5f);
                glow(c, cam, pos.x + x2, mid + y1, pos.z + z2, (0.012f + 0.03f * b) * sz, col, al * (0.12f + 0.88f * b * b));
            }
        }
    }

    private static void halo(DrawContext c, Cam cam, Vec3d pos, float w, float h, float t, float sz, float al) {
        double y = pos.y + h + 0.22 + Math.sin(t * 2) * 0.04;
        double R0 = 0.28 * sz + w * 0.25;
        int N = 36;
        for (int i = 0; i < N; i++) {
            float b = 1f - (float) i / N;
            double ang = t * 1.8 - i * 0.1;
            int col = Ui.mix(Ui.accent(i * 0.006f), 0xFFFFFFFF, b * 0.55f);
            glow(c, cam, pos.x + Math.cos(ang) * R0, y, pos.z + Math.sin(ang) * R0, (0.01f + 0.026f * b) * sz, col, al * (0.1f + 0.9f * b * b));
        }
        for (int i = 0; i < 3; i++) {
            double ang = -t * 1.1 + i * 2.0944;
            glow(c, cam, pos.x + Math.cos(ang) * R0 * 0.55, y + 0.05, pos.z + Math.sin(ang) * R0 * 0.55, 0.02f * sz, Ui.accent(i * 0.2f), al * 0.7f);
        }
    }

    private static void orbs(DrawContext c, Cam cam, Vec3d pos, float w, float h, float t, float sz, float al) {
        double R0 = (w * 0.5 + 0.45) * sz;
        for (int k = 0; k < 4; k++) {
            for (int j = 0; j < 10; j++) {
                float tt = t - j * 0.045f;
                double ang = tt * 2.4 + k * Math.PI / 2;
                double y = pos.y + h * 0.5 + Math.sin(tt * 1.7 + k * 1.9) * h * 0.38;
                float b = 1f - j / 10f;
                int col = Ui.mix(Ui.accent(k * 0.2f), 0xFFFFFFFF, b * 0.5f);
                glow(c, cam, pos.x + Math.cos(ang) * R0, y, pos.z + Math.sin(ang) * R0, (0.015f + 0.05f * b) * sz, col, al * (0.1f + 0.9f * b));
            }
        }
    }

    private static void spiral(DrawContext c, Cam cam, Vec3d pos, float w, float h, float t, float sz, float al) {
        double R0 = (w * 0.5 + 0.35) * sz;
        for (int s = 0; s < 2; s++) {
            for (int i = 0; i < 20; i++) {
                float f = (i / 20f + t * 0.25f) % 1f;
                double ang = t * 2.5 + s * Math.PI + f * 7.0;
                double r = R0 * (0.85 + 0.15 * Math.sin(f * 6 + t));
                float fade = (float) Math.sin(f * Math.PI);
                glow(c, cam, pos.x + Math.cos(ang) * r, pos.y + f * h, pos.z + Math.sin(ang) * r, 0.03f * sz,
                        Ui.accent(s * 0.3f + f * 0.2f), al * fade);
            }
        }
    }

    private static void floor(DrawContext c, Cam cam, Vec3d pos, float w, float h, float t, float sz, float al) {
        double y = pos.y + 0.03;
        double R0 = (w * 0.5 + 0.45) * sz * (1 + 0.06 * Math.sin(t * 2.5));
        int N = 40;
        for (int i = 0; i < N; i++) {
            double ang = i * Math.PI * 2 / N;
            glow(c, cam, pos.x + Math.cos(ang) * R0, y, pos.z + Math.sin(ang) * R0, 0.016f * sz, Ui.accent(i / (float) N * 0.5f), al * 0.35f);
        }
        for (int k = 0; k < 3; k++) {
            for (int j = 0; j < 14; j++) {
                float b = 1f - j / 14f;
                double ang = t * 2 + k * 2.0944 - j * 0.07;
                glow(c, cam, pos.x + Math.cos(ang) * R0, y, pos.z + Math.sin(ang) * R0, (0.02f + 0.035f * b) * sz,
                        Ui.mix(Ui.accent(k * 0.15f), 0xFFFFFFFF, b * 0.5f), al * b);
            }
        }
    }
}
