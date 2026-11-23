package dev.visuals;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class VisualsClient implements ClientModInitializer {
    public static KeyBinding openKey, zoomKey;

    @Override
    public void onInitializeClient() {
        VisualsConfig.load();

        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("visuals", "main"));
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.visuals.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, cat));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.visuals.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, cat));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.wasPressed()) {
                if (mc.currentScreen == null) mc.setScreen(new VisualsScreen());
            }
            Fx.tick(mc);
            Zoom.tick(mc, zoomKey);
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> Zoom.restore(mc));

        HudRenderCallback.EVENT.register(VisualsHud::render);

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && entity instanceof LivingEntity) VisualsHud.onHit(entity);
            return ActionResult.PASS;
        });
    }
}
