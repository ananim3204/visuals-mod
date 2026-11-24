package dev.netrixvisual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.SimpleOption;

/** Зум на клавишу (по умолчанию C): плавно меняет FOV и возвращает обратно. */
public final class Zoom {
    private static double cur = -1;
    private static int orig = -1;

    public static void tick(MinecraftClient mc, KeyBinding key) {
        if (mc.player == null) { restore(mc); return; }
        VisualsConfig c = VisualsConfig.I;
        SimpleOption<Integer> fov = mc.options.getFov();
        boolean want = c.zoom && mc.currentScreen == null && key.isPressed();
        if (want && orig < 0) { orig = fov.getValue(); cur = orig; }
        if (orig < 0) return;
        double target = want ? c.zoomFov : orig;
        cur += (target - cur) * 0.4;
        if (Math.abs(target - cur) < 0.7) {
            cur = target;
            if (!want) { fov.setValue(orig); orig = -1; return; }
        }
        fov.setValue((int) Math.round(cur));
    }

    public static void restore(MinecraftClient mc) {
        if (orig >= 0) { mc.options.getFov().setValue(orig); orig = -1; }
    }
}
