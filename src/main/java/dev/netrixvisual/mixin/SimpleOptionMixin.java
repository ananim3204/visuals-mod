package dev.netrixvisual.mixin;

import dev.netrixvisual.VisualsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Ночное зрение: подменяет значение «Гаммы» на огромное — тьмы нет совсем (как у гамма-модов). */
@Mixin(SimpleOption.class)
public abstract class SimpleOptionMixin<T> {
    @SuppressWarnings("unchecked")
    @Inject(method = "getValue", at = @At("RETURN"), cancellable = true, require = 0)
    private void netrix$gamma(CallbackInfoReturnable<T> cir) {
        if (!VisualsConfig.I.fullbright) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null) return;
        if ((Object) this == mc.options.getGamma()) cir.setReturnValue((T) (Double) 16.0);
    }
}
