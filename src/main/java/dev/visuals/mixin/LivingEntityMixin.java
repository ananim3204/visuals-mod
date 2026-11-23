package dev.visuals.mixin;

import dev.visuals.VisualsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Скорость взмаха руки (только у локального игрока). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true, require = 0)
    private void visuals$swingSpeed(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this != MinecraftClient.getInstance().player) return;
        float s = VisualsConfig.I.swingSpeed;
        if (Math.abs(s - 1f) < 0.01f) return;
        cir.setReturnValue(Math.max(1, Math.round(cir.getReturnValue() / s)));
    }
}
