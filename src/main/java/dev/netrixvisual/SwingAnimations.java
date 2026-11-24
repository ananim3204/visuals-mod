package dev.netrixvisual;

import dev.netrixvisual.VisualsConfig.SwingStyle;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public final class SwingAnimations {
    /** Стиль анимации для предмета в основной руке. */
    public static SwingStyle currentStyle() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return SwingStyle.VANILLA;
        VisualsConfig c = VisualsConfig.I;
        ItemStack s = mc.player.getMainHandStack();
        String id = Registries.ITEM.getId(s.getItem()).getPath();
        SwingStyle st;
        if (id.endsWith("_sword")) st = c.swordSwing;
        else if (id.endsWith("_spear") || id.equals("trident")) st = c.spearSwing;
        else if (id.endsWith("_pickaxe") || id.endsWith("_axe") || id.endsWith("_shovel") || id.endsWith("_hoe")) st = c.toolSwing;
        else st = c.otherSwing;
        if (st == SwingStyle.VANILLA && c.smoothPvp && VisualsHud.inPvp()) st = SwingStyle.SMOOTH;
        return st;
    }

    private static void rx(MatrixStack m, float d) { m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(d)); }
    private static void ry(MatrixStack m, float d) { m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(d)); }
    private static void rz(MatrixStack m, float d) { m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(d)); }

    private static float outCubic(float t) { float u = 1f - t; return 1f - u * u * u; }
    private static float inOutCubic(float t) { return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f; }

    /**
     * Профиль удара: короткий замах назад (-0.18) → резкий удар (1.0) → плавный возврат (0).
     * Именно замах и резкость делают анимацию «живой» вместо ванильной синусоиды.
     */
    private static float curve(float p) {
        if (p < 0.22f) return -0.18f * Ui.ease(p / 0.22f);
        if (p < 0.48f) return MathHelper.lerp(outCubic((p - 0.22f) / 0.26f), -0.18f, 1f);
        return 1f - inOutCubic((p - 0.48f) / 0.52f);
    }

    /** Профиль для «Сокрушения»: высокий замах и резкий удар вниз. */
    private static float curveSmash(float p) {
        if (p < 0.35f) return -0.55f * Ui.ease(p / 0.35f);
        if (p < 0.55f) return MathHelper.lerp(outCubic((p - 0.35f) / 0.2f), -0.55f, 1f);
        return 1f - inOutCubic((p - 0.55f) / 0.45f);
    }

    public static void apply(MatrixStack m, Arm arm, float p, SwingStyle style) {
        int side = arm == Arm.RIGHT ? 1 : -1;
        float pi = (float) Math.PI;
        float k = VisualsConfig.I.swingIntensity;
        float a = curve(p) * k;
        float s = Ui.ease(p);
        float sin = MathHelper.sin(p * pi) * k;
        switch (style) {
            case SMOOTH -> {
                m.translate(side * -0.22f * a, 0.08f * a, -0.2f * a);
                ry(m, side * (45f - 14f * a));
                rz(m, side * a * -14f);
                rx(m, a * -52f);
                ry(m, side * -45f);
            }
            case SLASH -> {
                m.translate(side * -0.34f * a, 0.12f * a, -0.25f * a);
                rz(m, side * -a * 68f);
                rx(m, -a * 34f);
                ry(m, side * a * 20f);
            }
            case CHOP -> {
                m.translate(0f, 0.28f * a, -0.35f * a);
                rx(m, -a * 98f);
                rz(m, side * a * 8f);
            }
            case STAB -> {
                m.translate(0f, 0.02f * a, -1.0f * a);
                rx(m, -a * 8f);
            }
            case SPIN -> {
                m.translate(0f, 0f, -0.28f * sin);
                rz(m, side * s * 360f);
            }
            case WHIP -> {
                float wob = MathHelper.sin(p * pi * 3f) * (1f - p) * 9f;
                m.translate(side * -0.42f * a, 0.16f * a, -0.3f * a);
                rz(m, side * (-a * 42f + wob));
                rx(m, -a * 62f);
                ry(m, side * a * 38f);
            }
            case ARC -> {
                m.translate(side * (0.28f - 0.62f * s), 0.1f * sin, -0.3f * sin);
                rz(m, side * (42f - 105f * s));
                rx(m, -30f * sin);
            }
            case FLIP -> {
                m.translate(0f, 0.1f * sin, -0.32f * sin);
                rx(m, -s * 360f);
            }
            case PUNCH -> {
                m.translate(side * -0.14f * a, -0.04f * a, -0.65f * a);
                rz(m, side * -a * 24f);
                ry(m, side * a * 20f);
                rx(m, -a * 14f);
            }
            case SMASH -> {
                float b = curveSmash(p) * k;
                m.translate(0f, -0.25f * b, -0.3f * b);
                rx(m, -b * 105f);
                rz(m, side * b * 6f);
            }
            case WINDMILL -> {
                m.translate(0f, 0f, -0.22f * sin);
                ry(m, side * s * 360f);
                rx(m, -sin * 25f);
            }
            case THROW -> {
                m.translate(side * -0.1f * a, 0.12f * sin, -0.95f * a);
                rx(m, -a * 70f);
                rz(m, side * a * 10f);
            }
            case BLADE -> {
                m.translate(side * -0.3f * a, 0.1f * a, -0.3f * a);
                rz(m, side * -a * 50f);
                ry(m, side * s * 180f);
                rx(m, -a * 20f);
            }
            case HAMMER -> {
                float b = curveSmash(p) * k;
                m.translate(0f, -0.35f * b, -0.4f * b);
                rx(m, -b * 120f);
                ry(m, side * b * 12f);
                rz(m, side * b * 10f);
            }
            case CORKSCREW -> {
                m.translate(0f, 0f, -0.9f * a);
                rz(m, side * s * 540f);
            }
            default -> {}
        }
    }
}
