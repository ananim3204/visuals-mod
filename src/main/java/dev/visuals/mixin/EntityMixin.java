package dev.visuals.mixin;

import dev.visuals.VisualsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Огонь не перекрывает экран: для рендера свой игрок «не горит» (на сервере всё как обычно). */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "isOnFire", at = @At("RETURN"), cancellable = true, require = 0)
    private void netrix$noFire(CallbackInfoReturnable<Boolean> cir) {
        if (!VisualsConfig.I.noFireOverlay) return;
        if (cir.getReturnValue() != null && cir.getReturnValue() && (Object) this == MinecraftClient.getInstance().player) {
            cir.setReturnValue(false);
        }
    }
}
