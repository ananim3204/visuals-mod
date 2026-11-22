package dev.visuals.mixin;

import dev.visuals.SwingAnimations;
import dev.visuals.VisualsConfig.SwingStyle;
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
}
