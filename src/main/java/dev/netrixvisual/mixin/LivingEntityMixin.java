package dev.netrixvisual.mixin;

import dev.netrixvisual.VisualsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Скорость взмаха + скрытие слепоты/тьмы/тошноты от рендера (только у локального игрока). */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true, require = 0)
    private void netrix$swingSpeed(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this != MinecraftClient.getInstance().player) return;
        float s = VisualsConfig.I.swingSpeed;
        if (Math.abs(s - 1f) < 0.01f) return;
        cir.setReturnValue(Math.max(1, Math.round(cir.getReturnValue() / s)));
    }

    private boolean netrix$hidden(RegistryEntry<StatusEffect> e) {
        if ((Object) this != MinecraftClient.getInstance().player) return false;
        VisualsConfig c = VisualsConfig.I;
        return (c.noBlindness && e == StatusEffects.BLINDNESS)
                || (c.noDarkness && e == StatusEffects.DARKNESS)
                || (c.noNausea && e == StatusEffects.NAUSEA);
    }

    @Inject(method = "hasStatusEffect", at = @At("RETURN"), cancellable = true, require = 0)
    private void netrix$has(RegistryEntry<StatusEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() != null && cir.getReturnValue() && netrix$hidden(effect)) cir.setReturnValue(false);
    }

    @Inject(method = "getStatusEffect", at = @At("RETURN"), cancellable = true, require = 0)
    private void netrix$get(RegistryEntry<StatusEffect> effect, CallbackInfoReturnable<StatusEffectInstance> cir) {
        if (cir.getReturnValue() != null && netrix$hidden(effect)) cir.setReturnValue(null);
    }
}
