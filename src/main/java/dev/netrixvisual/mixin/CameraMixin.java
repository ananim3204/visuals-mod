package dev.netrixvisual.mixin;

import dev.netrixvisual.VisualsConfig;
import net.minecraft.client.render.CameraSubmersionType;
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
    private void netrix$clear(CallbackInfoReturnable<CameraSubmersionType> cir) {
        VisualsConfig c = VisualsConfig.I;
        CameraSubmersionType t = cir.getReturnValue();
        if (t == null) return;
        if ((t == CameraSubmersionType.LAVA && c.clearLava)
                || (t == CameraSubmersionType.WATER && c.clearWater)
                || (t == CameraSubmersionType.POWDER_SNOW && c.clearSnow)) {
            cir.setReturnValue(CameraSubmersionType.NONE);
        }
    }
}
