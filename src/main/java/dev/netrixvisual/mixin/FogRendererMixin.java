package dev.netrixvisual.mixin;

import dev.netrixvisual.VisualsConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Раздувает дистанцию тумана до огромной => туман не виден. */
@Pseudo
@Mixin(targets = "net.minecraft.client.render.fog.FogRenderer")
public class FogRendererMixin {
    @ModifyVariable(method = "applyFog", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int visuals$noFog(int viewDistance) {
        return VisualsConfig.I.removeFog ? 100000 : viewDistance;
    }
}
