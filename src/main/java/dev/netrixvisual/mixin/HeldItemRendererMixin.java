package dev.netrixvisual.mixin;

import dev.netrixvisual.SwingAnimations;
import dev.netrixvisual.VisualsConfig;
import dev.netrixvisual.VisualsConfig.SwingStyle;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Inject(method = "applySwingOffset", at = @At("HEAD"), cancellable = true, require = 0)
    private void visuals$swing(MatrixStack matrices, Arm arm, float swingProgress, CallbackInfo ci) {
        SwingStyle s = SwingAnimations.currentStyle();
        if (s == SwingStyle.VANILLA) return;
        SwingAnimations.apply(matrices, arm, swingProgress, s);
        ci.cancel();
    }

    /** Смещение и размер руки/предмета от первого лица. */
    @Inject(method = "applyEquipOffset", at = @At("HEAD"), require = 0)
    private void visuals$offset(MatrixStack matrices, Arm arm, float equipProgress, CallbackInfo ci) {
        VisualsConfig c = VisualsConfig.I;
        int side = arm == Arm.RIGHT ? 1 : -1;
        if (c.handX != 0f || c.handY != 0f || c.handZ != 0f) matrices.translate(side * c.handX, c.handY, c.handZ);
        if (c.handScale != 1f) matrices.scale(c.handScale, c.handScale, c.handScale);
    }
}
