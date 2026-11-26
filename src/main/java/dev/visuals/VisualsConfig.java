package dev.visuals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class VisualsConfig {
    public static VisualsConfig I = new VisualsConfig();

    // ---- Тема / HUD ----
    public Accent accent = Accent.BLUE;
    public HudStyle hudStyle = HudStyle.GLASS;
    public float hudOpacity = 0.7f;
    public Map<String, WidgetState> widgets = new HashMap<>();

    // ---- Эффект на цели ----
    public boolean targetFx = true;
    public TargetFx targetFxMode = TargetFx.RINGS;
    public boolean targetOnlyPlayers = true;
    public float fxSpeed = 1f;
    public float fxSize = 1f;

    // ---- Удар ----
    public HitFx hitFx = HitFx.SPARKS;
    public int hitFxCount = 18;
    public boolean damageNumbers = true;
    public boolean hitMarker = true;
    public HitSound hitSound = HitSound.DING;

    // ---- Анимации ----
    public SwingStyle swordSwing = SwingStyle.SLASH;
    public SwingStyle toolSwing = SwingStyle.CHOP;
    public SwingStyle spearSwing = SwingStyle.STAB;
    public SwingStyle otherSwing = SwingStyle.VANILLA;
    public boolean smoothPvp = true;
    public float swingSpeed = 1f;
    public float handX = 0f, handY = 0f, handZ = 0f, handScale = 1f;

    public float swingIntensity = 1f;

    // ---- Косметика ----
    public boolean jumpWave = true;
    public Trail trail = Trail.GLOW;
    public boolean killFx = true;
    public boolean hurtVignette = true;
    public boolean lowHpVignette = true;

    public Hat hat = Hat.SAMURAI;
    public float hatScale = 1f;
    public Pack pack = Pack.NONE;
    public boolean themedCosmetics = false;
    public boolean selfFx = false;
    public float fxQuality = 1f;

    // ---- Зрение ----
    public boolean fullbright = false;
    public boolean noBlindness = true;
    public boolean noDarkness = true;
    public boolean noNausea = false;
    public boolean clearLava = true;
    public boolean clearWater = false;
    public boolean clearSnow = true;

    // ---- Мир ----
    public boolean customTitle = true;
    public boolean customGui = true;
    public boolean noFireOverlay = true;
    public boolean removeFog = false;
    public boolean zoom = true;
    public int zoomFov = 30;

    public static class WidgetState {
        public boolean on;
        public float x, y;
    }

    public enum Accent {
        BLUE("Синий", 0x4C8DFF), PURPLE("Фиолет", 0xA06BFF), PINK("Розовый", 0xFF5FA2), RED("Красный", 0xFF4D4D),
        ORANGE("Оранжевый", 0xFF9A3C), GREEN("Зелёный", 0x4DDB7A), CYAN("Бирюза", 0x3CE0E0), WHITE("Белый", 0xE8E8F0),
        RAINBOW("Радуга", 0xFFFFFF);
        public final String label; public final int rgb;
        Accent(String l, int c) { label = l; rgb = c; }
    }

    public enum HudStyle {
        GLASS("Стекло"), SOLID("Тёмный"), NONE("Без фона");
        public final String label;
        HudStyle(String l) { label = l; }
    }

    public enum TargetFx {
        RINGS("Кольца"), HALO("Нимб"), ORBS("Орбы"), SPIRAL("Спираль"), FLOOR("Круг под ногами"),
        ATOM("Атом"), SCANNER("Сканер"), DOME("Купол"), TORNADO("Торнадо"), CROWN("Корона"), CRYSTALS("Кристаллы");
        public final String label;
        TargetFx(String l) { label = l; }
    }

    public enum HitFx {
        OFF("Выкл"), SPARKS("Искры"), STARS("Звёзды"), SHOCKWAVE("Волна"), SOULS("Души"),
        SLASH("Разрез"), HEARTS("Сердца"), CONFETTI("Конфетти"), LIGHTNING("Молнии"), NOVA("Взрыв"), PULSE("Пульс");
        public final String label;
        HitFx(String l) { label = l; }
    }

    public enum Hat {
        NONE("Нет"), SAMURAI("Шляпа самурая"), WIZARD("Шляпа волшебника"), TOPHAT("Цилиндр"), PARTY("Колпак-праздник"),
        SANTA("Колпак Санты"), CROWN("Корона"), HALO("Нимб"), CAT_EARS("Кошачьи уши"), HORNS("Рога"), FLOWERS("Цветочный венок");
        public final String label;
        Hat(String l) { label = l; }
    }

    public enum Pack {
        NONE("Нет"), SCHOOL("Школьный рюкзак"), HIKER("Туристический"), JETPACK("Реактивный ранец"),
        WINGS_ANGEL("Крылья ангела"), WINGS_DEMON("Крылья демона");
        public final String label;
        Pack(String l) { label = l; }
    }

    public enum Trail {
        OFF("Выкл"), GLOW("Свечение"), SPARKLE("Звёздочки"), RAINBOW("Радуга");
        public final String label;
        Trail(String l) { label = l; }
    }

    public enum HitSound {
        OFF("Выкл", null), DING("Дзынь", SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP), CHIME("Кристалл", SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME),
        ARROW("Попадание", SoundEvents.ENTITY_ARROW_HIT_PLAYER), ANVIL("Наковальня", SoundEvents.BLOCK_ANVIL_LAND);
        public final String label; public final SoundEvent sound;
        HitSound(String l, SoundEvent s) { label = l; sound = s; }
    }

    public enum SwingStyle {
        VANILLA("Ванильная"), SMOOTH("Плавная"), SLASH("Рубящая"), CHOP("Сверху вниз"), STAB("Выпад"), SPIN("Вращение"),
        WHIP("Хлыст"), ARC("Дуга"), FLIP("Переворот"), PUNCH("Хук"), BLADE("Лезвие"), HAMMER("Молот"),
        SMASH("Сокрушение"), WINDMILL("Мельница"), THROW("Бросок"), CORKSCREW("Штопор");
        public final String label;
        SwingStyle(String l) { label = l; }
    }

    public WidgetState state(HudWidget w) {
        WidgetState s = widgets.get(w.id);
        if (s == null) {
            s = new WidgetState();
            s.on = w.defOn; s.x = w.defX; s.y = w.defY;
            widgets.put(w.id, s);
        }
        return s;
    }

    private void fix() {
        if (accent == null) accent = Accent.BLUE;
        if (hudStyle == null) hudStyle = HudStyle.GLASS;
        if (targetFxMode == null) targetFxMode = TargetFx.RINGS;
        if (hitFx == null) hitFx = HitFx.SPARKS;
        if (hitSound == null) hitSound = HitSound.DING;
        if (swordSwing == null) swordSwing = SwingStyle.SLASH;
        if (toolSwing == null) toolSwing = SwingStyle.CHOP;
        if (spearSwing == null) spearSwing = SwingStyle.STAB;
        if (otherSwing == null) otherSwing = SwingStyle.VANILLA;
        if (widgets == null) widgets = new HashMap<>();
        if (trail == null) trail = Trail.GLOW;
        if (hat == null) hat = Hat.SAMURAI;
        if (pack == null) pack = Pack.NONE;
        if (hatScale < 0.4f) hatScale = 1f;
        if (fxQuality < 0.2f) fxQuality = 1f;
        if (swingIntensity < 0.2f) swingIntensity = 1f;
        if (swingSpeed < 0.3f) swingSpeed = 1f;
        if (handScale < 0.3f) handScale = 1f;
        if (hitFxCount < 1) hitFxCount = 18;
        if (zoomFov < 10) zoomFov = 30;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("netrixvisual.json"); }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) {
                Path old = FabricLoader.getInstance().getConfigDir().resolve("visuals.json");
                if (Files.exists(old)) f = old;
            }
            if (Files.exists(f)) {
                VisualsConfig c = GSON.fromJson(Files.readString(f), VisualsConfig.class);
                if (c != null) I = c;
            }
        } catch (Exception e) { e.printStackTrace(); }
        I.fix();
        save();
    }

    public static void save() {
        try { Files.writeString(file(), GSON.toJson(I)); } catch (Exception e) { e.printStackTrace(); }
    }
}
