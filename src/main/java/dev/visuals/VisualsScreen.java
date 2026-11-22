package dev.visuals;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.util.function.Function;

public class VisualsScreen extends Screen {
    private int idx = 0;

    public VisualsScreen() { super(Text.literal("Визуалы")); }

    private int px() { return width / 2 + (idx % 2 == 0 ? -154 : 4); }
    private int py() { return 36 + (idx / 2) * 24; }

    private void bool(String name, String field) {
        ButtonWidget b = ButtonWidget.builder(boolLabel(name, field), btn -> {
            try {
                Field f = VisualsConfig.class.getField(field);
                f.setBoolean(VisualsConfig.I, !f.getBoolean(VisualsConfig.I));
            } catch (Exception ignored) {}
            btn.setMessage(boolLabel(name, field));
        }).dimensions(px(), py(), 150, 20).build();
        addDrawableChild(b);
        idx++;
    }

    private Text boolLabel(String name, String field) {
        boolean v = false;
        try { v = VisualsConfig.class.getField(field).getBoolean(VisualsConfig.I); } catch (Exception ignored) {}
        return Text.literal(name + ": " + (v ? "§aВКЛ" : "§cВЫКЛ"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void cycle(String name, String field, Function<Object, String> label) {
        ButtonWidget b = ButtonWidget.builder(enumLabel(name, field, label), btn -> {
            try {
                Field f = VisualsConfig.class.getField(field);
                Object cur = f.get(VisualsConfig.I);
                Object[] all = cur.getClass().getEnumConstants();
                f.set(VisualsConfig.I, all[(((Enum) cur).ordinal() + 1) % all.length]);
            } catch (Exception ignored) {}
            btn.setMessage(enumLabel(name, field, label));
        }).dimensions(px(), py(), 150, 20).build();
        addDrawableChild(b);
        idx++;
    }

    private Text enumLabel(String name, String field, Function<Object, String> label) {
        try {
            return Text.literal(name + ": §e" + label.apply(VisualsConfig.class.getField(field).get(VisualsConfig.I)));
        } catch (Exception e) { return Text.literal(name); }
    }

    @Override
    protected void init() {
        idx = 0;
        bool("FPS", "showFps");
        bool("Пинг", "showPing");
        bool("Координаты", "showCoords");
        bool("HP игрока", "showHp");
        bool("HP цели", "showTargetHp");
        bool("Еда (сколько восстановит)", "showFood");
        bool("Прочность брони", "showArmor");
        bool("Убрать туман", "removeFog");
        bool("Хитмаркер", "hitMarker");
        bool("Плавные анимации в PvP", "smoothPvp");
        cycle("Частицы удара", "hitParticle", o -> ((VisualsConfig.HitParticle) o).label);
        cycle("Звук удара", "hitSound", o -> ((VisualsConfig.HitSound) o).label);
        cycle("Анимация меча", "swordSwing", o -> ((VisualsConfig.SwingStyle) o).label);
        cycle("Анимация кирки/топора", "toolSwing", o -> ((VisualsConfig.SwingStyle) o).label);
        cycle("Анимация копья", "spearSwing", o -> ((VisualsConfig.SwingStyle) o).label);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("§lВизуалы"), width / 2, 14, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("ESC — закрыть, настройки сохраняются автоматически"), width / 2, height - 16, 0xFFAAAAAA);
    }

    @Override
    public void removed() { VisualsConfig.save(); }

    @Override
    public boolean shouldPause() { return false; }
}
