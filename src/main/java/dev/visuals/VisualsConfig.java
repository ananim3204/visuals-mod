package dev.visuals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.nio.file.Files;
import java.nio.file.Path;

public class VisualsConfig {
    public static VisualsConfig I = new VisualsConfig();

    public boolean showFps = true, showPing = true, showCoords = true;
    public boolean showHp = true, showTargetHp = true, showFood = true, showArmor = true;
    public boolean removeFog = false, hitMarker = true, smoothPvp = true;
    public HitParticle hitParticle = HitParticle.CRIT;
    public HitSound hitSound = HitSound.DING;
    public SwingStyle swordSwing = SwingStyle.VANILLA;
    public SwingStyle toolSwing = SwingStyle.VANILLA;
    public SwingStyle spearSwing = SwingStyle.VANILLA;

    public enum HitParticle {
        OFF("Выкл", null), CRIT("Криты", ParticleTypes.CRIT), MAGIC("Магия", ParticleTypes.ENCHANTED_HIT),
        HEART("Сердца", ParticleTypes.HEART), FLAME("Огонь", ParticleTypes.FLAME), STAR("Звёзды", ParticleTypes.END_ROD);
        public final String label; public final ParticleEffect type;
        HitParticle(String l, ParticleEffect t) { label = l; type = t; }
    }

    public enum HitSound {
        OFF("Выкл", null), DING("Дзынь", SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP), CHIME("Кристалл", SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME),
        ARROW("Попадание", SoundEvents.ENTITY_ARROW_HIT_PLAYER), ANVIL("Наковальня", SoundEvents.BLOCK_ANVIL_LAND);
        public final String label; public final SoundEvent sound;
        HitSound(String l, SoundEvent s) { label = l; sound = s; }
    }

    public enum SwingStyle {
        VANILLA("Ванильная"), SMOOTH("Плавная"), SLASH("Рубящая"), CHOP("Сверху вниз"), STAB("Выпад"), SPIN("Вращение");
        public final String label;
        SwingStyle(String l) { label = l; }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("visuals.json"); }

    public static void load() {
        try {
            if (Files.exists(file())) {
                VisualsConfig c = GSON.fromJson(Files.readString(file()), VisualsConfig.class);
                if (c != null) I = c;
            }
        } catch (Exception e) { e.printStackTrace(); }
        save();
    }

    public static void save() {
        try { Files.writeString(file(), GSON.toJson(I)); } catch (Exception e) { e.printStackTrace(); }
    }
}
