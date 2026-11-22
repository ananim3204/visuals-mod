package dev.visuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import dev.visuals.VisualsConfig.SwingStyle;

public final class SwingAnimations {
    /** Стиль анимации для предмета в основной руке. */
    public static SwingStyle currentStyle() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return SwingStyle.VANILLA;
        VisualsConfig c = VisualsConfig.I;
        ItemStack s = mc.player.getMainHandStack();
        String id = Registries.ITEM.getId(s.getItem()).getPath();
        SwingStyle st = SwingStyle.VANILLA;
        if (id.endsWith("_sword")) st = c.swordSwing;
        else if (id.endsWith("_spear") || id.equals("trident")) st = c.spearSwing;
        else if (id.endsWith("_pickaxe") || id.endsWith("_axe") || id.endsWith("_shovel") || id.endsWith("_hoe")) st = c.toolSwing;
        if (st == SwingStyle.VANILLA && c.smoothPvp && VisualsHud.inPvp()) st = SwingStyle.SMOOTH;
        return st;
    }

    private static void rx(MatrixStack m, float d) { m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(d)); }
    private static void ry(MatrixStack m, float d) { m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(d)); }
    private static void rz(MatrixStack m, float d) { m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(d)); }

    public static void apply(MatrixStack m, Arm arm, float p, SwingStyle style) {
        int side = arm == Arm.RIGHT ? 1 : -1;
        float pi = (float) Math.PI;
        switch (style) {
            case SMOOTH -> {
                float e = p * p * (3f - 2f * p);
                float sq = MathHelper.sin(MathHelper.sqrt(e) * pi);
                float sin = MathHelper.sin(e * pi);
                m.translate(side * -0.28f * sq, 0.12f * MathHelper.sin(MathHelper.sqrt(e) * pi * 2f), -0.15f * sin);
                ry(m, side * (45f + MathHelper.sin(e * e * pi) * -14f));
                rz(m, side * sq * -14f);
                rx(m, sq * -56f);
                ry(m, side * -45f);
            }
            case SLASH -> {
                float sq = MathHelper.sin(MathHelper.sqrt(p) * pi);
                float sin = MathHelper.sin(p * pi);
                m.translate(side * -0.3f * sq, 0.1f * sq, -0.2f * sin);
                rz(m, side * -sq * 70f);
                rx(m, -sin * 30f);
            }
            case CHOP -> {
                float sq = MathHelper.sin(MathHelper.sqrt(p) * pi);
                float sin = MathHelper.sin(p * pi);
                m.translate(0f, 0.2f * sq, -0.3f * sin);
                rx(m, -sin * 100f);
            }
            case STAB -> {
                float sin = MathHelper.sin(p * pi);
                m.translate(0f, 0f, -0.9f * sin);
                rx(m, -sin * 10f);
            }
            case SPIN -> {
                float sin = MathHelper.sin(p * pi);
                m.translate(0f, 0f, -0.25f * sin);
                rz(m, side * p * 360f);
            }
            default -> {}
        }
    }
}
