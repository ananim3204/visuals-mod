package dev.visuals.mixin;

import dev.visuals.VisualsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Чистая видимость в лаве / воде / сыпучем снегу: камера «думает», что она не под жидкостью. */
@Pseudo
@Mixin(targets = "net.minecraft.client.render.Camera")
public class CameraMixin {
    @Inject(method = "getSubmersionType", at = @At("RETURN"), cancellable = true, require = 0)
    private void netrix$clear(CallbackInfoReturnable<Object> cir) {
        Object t = cir.getReturnValue();
        if (!(t instanceof Enum<?> e)) return;
        VisualsConfig c = VisualsConfig.I;
        String n = e.name();
        if (("LAVA".equals(n) && c.clearLava) || ("WATER".equals(n) && c.clearWater) || ("POWDER_SNOW".equals(n) && c.clearSnow)) {
            for (Object k : e.getDeclaringClass().getEnumConstants()) {
                if ("NONE".equals(((Enum<?>) k).name())) { cir.setReturnValue(k); return; }
            }
        }
    }
}
