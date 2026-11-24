package dev.netrixvisual;

import dev.netrixvisual.VisualsConfig.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Supplier;

/** Своё меню: вкладки, анимированные переключатели, слайдеры, палитра цветов. */
public class VisualsScreen extends Screen {
    private static final int SB = 104; // ширина боковой панели
    private static int lastTab = 0;

    private static final int HEADER = 0, TOGGLE = 1, CYCLE = 2, SLIDER = 3, BUTTON = 4, SWATCH = 5;

    private static final class Row {
        int kind; String name, desc;
        BooleanSupplier getB; Consumer<Boolean> setB;
        Supplier<String> label; Runnable next, prev, action;
        double min, max, step; DoubleSupplier getF; DoubleConsumer setF;
        BooleanSupplier show;
        float anim = -1f;
        int x, y, w, h; boolean vis;
        Row(int k, String n, String d) { kind = k; name = n; desc = d; }
        int height() { return kind == HEADER ? 18 : kind == SWATCH ? 38 : 24; }
    }

    private final String[] tabNames = {"HUD", "Цель", "Удар", "Косметика", "Зрение", "Анимации", "Мир", "Тема"};
    private final List<List<Row>> tabs = new ArrayList<>();
    private int tab = lastTab;
    private float scroll, scrollT;
    private long openNs = System.nanoTime();
    private boolean pl = true, pr = true;
    private Row activeSlider;
    private int wx, wy, ww, wh;

    public VisualsScreen() { super(Text.literal("Визуалы")); }

    // ---------------------------------------------------------- конструкторы строк
    private static Row header(String n) { return new Row(HEADER, n, null); }

    private static Row toggle(String n, String d, BooleanSupplier g, Consumer<Boolean> s) {
        Row r = new Row(TOGGLE, n, d); r.getB = g; r.setB = s; return r;
    }

    private static <E extends Enum<E>> Row cycle(String n, String d, Supplier<E> g, Consumer<E> s, Function<E, String> lab) {
        Row r = new Row(CYCLE, n, d);
        r.label = () -> lab.apply(g.get());
        r.next = () -> { E cur = g.get(); E[] all = cur.getDeclaringClass().getEnumConstants(); s.accept(all[(cur.ordinal() + 1) % all.length]); };
        r.prev = () -> { E cur = g.get(); E[] all = cur.getDeclaringClass().getEnumConstants(); s.accept(all[(cur.ordinal() + all.length - 1) % all.length]); };
        return r;
    }

    private static Row slider(String n, String d, double min, double max, double step, String fmt, DoubleSupplier g, DoubleConsumer s) {
        Row r = new Row(SLIDER, n, d);
        r.min = min; r.max = max; r.step = step; r.getF = g; r.setF = s;
        r.label = () -> String.format(fmt, g.getAsDouble());
        return r;
    }

    private static Row button(String n, String d, Runnable a) { Row r = new Row(BUTTON, n, d); r.action = a; return r; }

    private static Row swatches() { return new Row(SWATCH, "Цвет акцента", "Цвет меню, HUD и эффектов"); }

    @Override
    protected void init() {
        VisualsConfig c = VisualsConfig.I;
        tabs.clear();

        List<Row> hud = new ArrayList<>();
        hud.add(header("Редактор"));
        hud.add(button("Редактор HUD — перетаскивание", "Открывает экран, где виджеты двигаются мышью",
                () -> MinecraftClient.getInstance().setScreen(new HudEditorScreen(this))));
        hud.add(button("Сбросить позиции виджетов", "Вернуть всё на места по умолчанию", HudWidgets::resetPositions));
        hud.add(header("Виджеты"));
        for (HudWidget w : HudWidgets.ALL) {
            hud.add(toggle(w.name, "Показывать «" + w.name + "»", () -> c.state(w).on, v -> c.state(w).on = v));
        }
        hud.add(header("Стиль"));
        hud.add(cycle("Фон виджетов", "Стекло / тёмный / без фона", () -> c.hudStyle, v -> c.hudStyle = v, v -> v.label));
        hud.add(slider("Прозрачность фона", "Насколько виден фон виджетов", 0.1, 1.0, 0.05, "%.2f",
                () -> c.hudOpacity, v -> c.hudOpacity = (float) v));
        tabs.add(hud);

        List<Row> tg = new ArrayList<>();
        tg.add(header("Эффект вокруг цели"));
        tg.add(toggle("Включить", "Крутящийся эффект вокруг существа под прицелом", () -> c.targetFx, v -> c.targetFx = v));
        tg.add(cycle("Вид эффекта", "Кольца, нимб, орбы, спираль, круг", () -> c.targetFxMode, v -> c.targetFxMode = v, v -> v.label));
        tg.add(toggle("Только игроки", "Не показывать на мобах", () -> c.targetOnlyPlayers, v -> c.targetOnlyPlayers = v));
        tg.add(slider("Скорость", "Скорость вращения", 0.2, 3.0, 0.1, "%.1fx", () -> c.fxSpeed, v -> c.fxSpeed = (float) v));
        tg.add(slider("Размер", "Радиус эффекта", 0.5, 2.0, 0.1, "%.1fx", () -> c.fxSize, v -> c.fxSize = (float) v));
        tabs.add(tg);

        List<Row> hit = new ArrayList<>();
        hit.add(header("Эффект удара"));
        hit.add(cycle("Частицы удара", "Свои частицы (не ванильные)", () -> c.hitFx, v -> c.hitFx = v, v -> v.label));
        Row cnt = slider("Количество частиц", "Сколько частиц при ударе", 4, 50, 1, "%.0f", () -> c.hitFxCount, v -> c.hitFxCount = (int) v);
        cnt.show = () -> c.hitFx != HitFx.OFF;
        hit.add(cnt);
        hit.add(toggle("Цифры урона", "Всплывающий урон над целью", () -> c.damageNumbers, v -> c.damageNumbers = v));
        hit.add(toggle("Хитмаркер", "Крестик в прицеле при попадании", () -> c.hitMarker, v -> c.hitMarker = v));
        hit.add(cycle("Звук удара", "Звук при попадании", () -> c.hitSound, v -> c.hitSound = v, v -> v.label));
        tabs.add(hit);

        List<Row> cs = new ArrayList<>();
        cs.add(header("Головной убор"));
        cs.add(cycle("Шапка", "Видна от 3-го лица (F5)", () -> c.hat, v -> c.hat = v, v -> v.label));
        cs.add(slider("Размер шапки", "Масштаб головного убора", 0.6, 1.6, 0.05, "%.2f", () -> c.hatScale, v -> c.hatScale = (float) v));
        cs.add(header("Спина"));
        cs.add(cycle("Рюкзак / крылья", "Рюкзаки, ранец и крылья на спине", () -> c.pack, v -> c.pack = v, v -> v.label));
        cs.add(toggle("Цвет по теме", "Красить шапки и рюкзаки в цвет акцента", () -> c.themedCosmetics, v -> c.themedCosmetics = v));
        cs.add(toggle("Эффект вокруг себя", "Кольца/нимб вокруг тебя в 3-м лице", () -> c.selfFx, v -> c.selfFx = v));
        cs.add(header("Движение"));
        cs.add(cycle("След за игроком", "Частицы позади при беге", () -> c.trail, v -> c.trail = v, v -> v.label));
        cs.add(toggle("Волны при прыжке", "Круг на земле при прыжке и приземлении", () -> c.jumpWave, v -> c.jumpWave = v));
        cs.add(header("Бой"));
        cs.add(toggle("Эффект убийства", "Взрыв и надпись KILL, когда добил цель", () -> c.killFx, v -> c.killFx = v));
        cs.add(toggle("Красные края при уроне", "Вспышка по краям экрана, когда бьют тебя", () -> c.hurtVignette, v -> c.hurtVignette = v));
        cs.add(toggle("Пульс при малом HP", "Края экрана пульсируют, когда HP <= 6", () -> c.lowHpVignette, v -> c.lowHpVignette = v));
        tabs.add(cs);

        List<Row> vs = new ArrayList<>();
        vs.add(header("Свет"));
        vs.add(toggle("Ночное зрение (клавиша N)", "Убирает всю тьму, как мод на гамму", () -> c.fullbright, v -> c.fullbright = v));
        vs.add(header("Дебафы"));
        vs.add(toggle("Без слепоты", "Слепота не затемняет экран", () -> c.noBlindness, v -> c.noBlindness = v));
        vs.add(toggle("Без тьмы Вардена", "Эффект «Тьма» не мешает видеть", () -> c.noDarkness, v -> c.noDarkness = v));
        vs.add(toggle("Без тошноты", "Экран не качается от тошноты и портала", () -> c.noNausea, v -> c.noNausea = v));
        vs.add(header("Видимость в среде"));
        vs.add(toggle("Чистая лава", "Видно далеко под лавой", () -> c.clearLava, v -> c.clearLava = v));
        vs.add(toggle("Чистая вода", "Видно далеко под водой", () -> c.clearWater, v -> c.clearWater = v));
        vs.add(toggle("Чистый снег", "Видно сквозь сыпучий снег", () -> c.clearSnow, v -> c.clearSnow = v));
        vs.add(toggle("Убрать туман", "Дальняя видимость без дымки", () -> c.removeFog, v -> c.removeFog = v));
        tabs.add(vs);

        List<Row> an = new ArrayList<>();
        an.add(header("Стили взмаха"));
        an.add(cycle("Меч", "Анимация удара мечом", () -> c.swordSwing, v -> c.swordSwing = v, v -> v.label));
        an.add(cycle("Кирка / топор / лопата", "Анимация инструментов", () -> c.toolSwing, v -> c.toolSwing = v, v -> v.label));
        an.add(cycle("Копьё / трезубец", "Анимация копья", () -> c.spearSwing, v -> c.spearSwing = v, v -> v.label));
        an.add(cycle("Остальное", "Анимация для прочих предметов и руки", () -> c.otherSwing, v -> c.otherSwing = v, v -> v.label));
        an.add(toggle("Плавность в PvP", "Ванильную анимацию в бою заменить плавной", () -> c.smoothPvp, v -> c.smoothPvp = v));
        an.add(slider("Сила анимации", "Размах движений", 0.3, 1.6, 0.1, "%.1fx", () -> c.swingIntensity, v -> c.swingIntensity = (float) v));
        an.add(slider("Скорость взмаха", "Больше — быстрее", 0.5, 2.5, 0.1, "%.1fx", () -> c.swingSpeed, v -> c.swingSpeed = (float) v));
        an.add(header("Положение предмета в руке"));
        an.add(slider("Смещение X", "Влево / вправо", -0.5, 0.5, 0.02, "%.2f", () -> c.handX, v -> c.handX = (float) v));
        an.add(slider("Смещение Y", "Вниз / вверх", -0.5, 0.5, 0.02, "%.2f", () -> c.handY, v -> c.handY = (float) v));
        an.add(slider("Смещение Z", "Ближе / дальше", -0.5, 0.5, 0.02, "%.2f", () -> c.handZ, v -> c.handZ = (float) v));
        an.add(slider("Размер", "Масштаб предмета в руке", 0.5, 1.5, 0.05, "%.2f", () -> c.handScale, v -> c.handScale = (float) v));
        an.add(button("Сбросить положение", "Вернуть руку как в ваниле", () -> { c.handX = c.handY = c.handZ = 0f; c.handScale = 1f; }));
        tabs.add(an);

        List<Row> wd = new ArrayList<>();
        wd.add(header("Мир"));
        wd.add(toggle("Новое главное меню", "Анимированное меню Netrixvisual вместо ванильного", () -> c.customTitle, v -> c.customTitle = v));
        wd.add(toggle("Зум (клавиша C)", "Приближение, пока зажата клавиша", () -> c.zoom, v -> c.zoom = v));
        wd.add(slider("Сила зума (FOV)", "Меньше — сильнее приближение", 10, 70, 1, "%.0f", () -> c.zoomFov, v -> c.zoomFov = (int) v));
        tabs.add(wd);

        List<Row> th = new ArrayList<>();
        th.add(header("Тема"));
        th.add(swatches());
        th.add(header("Производительность"));
        th.add(slider("Качество эффектов", "Меньше — больше FPS (меньше точек в эффектах)", 0.3, 1.0, 0.05, "%.2f", () -> c.fxQuality, v -> c.fxQuality = (float) v));
        tabs.add(th);

        if (tab >= tabs.size()) tab = 0;
        pl = pr = true; // не ловить клик, с которым открыли экран
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    // --------------------------------------------------------------- отрисовка
    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        MinecraftClient mc = MinecraftClient.getInstance();
        long win = mc.getWindow().getHandle();
        boolean L = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean R = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean pressL = L && !pl, pressR = R && !pr;
        pl = L; pr = R;

        float open = Ui.ease((System.nanoTime() - openNs) / 220e6f);
        Ui.alpha = open;
        try {
            ww = Math.min(440, width - 12);
            wh = Math.min(290, height - 12);
            wx = (width - ww) / 2;
            wy = (height - wh) / 2 + (int) ((1f - open) * 14f);

            // окно
            Ui.round(ctx, wx - 2, wy - 2, ww + 4, wh + 4, 7, 0x50000000);
            Ui.round(ctx, wx, wy, ww, wh, 5, 0xF2111419);
            Ui.round(ctx, wx, wy, SB, wh, 5, 0xFF0B0D11);
            Ui.rect(ctx, wx + SB - 5, wy, 5, wh, 0xFF0B0D11);
            Ui.rect(ctx, wx + SB, wy + 6, 1, wh - 12, 0x18FFFFFF);
            for (int x = wx + 5; x < wx + ww - 5; x += 3) {
                Ui.rect(ctx, x, wy, 3, 1, Ui.accent((x - wx) / (float) ww * 0.6f));
            }
            long nowMs = System.currentTimeMillis();
            for (int i = 0; i < 22; i++) {
                double rx = Math.abs(Math.sin(i * 12.9898) * 43758.5453) % 1.0, ry = Math.abs(Math.sin(i * 78.233) * 43758.5453) % 1.0;
                int dx = wx + 4 + (int) (rx * (ww - 8));
                int dy = wy + wh - 4 - (int) (((ry * wh) + nowMs / (60.0 + i * 7)) % (wh - 8));
                float tw = 0.25f + 0.35f * (0.5f + 0.5f * (float) Math.sin(nowMs / 400.0 + i));
                Ui.rect(ctx, dx, dy, 1, 1, Ui.argb(Ui.accent(i * 0.04f) & 0xFFFFFF, tw));
            }

            // заголовок
            Ui.gradientText(ctx, textRenderer, "NETRIXVISUAL", wx + 10, wy + 12);
            Ui.textFlat(ctx, textRenderer, "v2.0", wx + 10, wy + 23, 0xFF666C78);

            // вкладки
            for (int i = 0; i < tabNames.length; i++) {
                int ty = wy + 42 + i * 24;
                boolean sel = i == tab, hov = in(mx, my, wx + 6, ty, SB - 12, 22);
                if (sel) {
                    Ui.round(ctx, wx + 6, ty, SB - 12, 22, 4, Ui.argb(Ui.accent(i * 0.08f) & 0xFFFFFF, 0.22f));
                    Ui.rect(ctx, wx + 6, ty + 4, 2, 14, Ui.accent(i * 0.08f));
                } else if (hov) {
                    Ui.round(ctx, wx + 6, ty, SB - 12, 22, 4, 0x18FFFFFF);
                }
                Ui.text(ctx, textRenderer, tabNames[i], wx + 16, ty + 7, sel ? 0xFFFFFFFF : hov ? 0xFFDDDDDD : 0xFF8A909C);
                if (pressL && hov && !sel) { tab = i; lastTab = i; scrollT = scroll = 0; activeSlider = null; }
            }

            // содержимое
            int cx = wx + SB + 10, cw = ww - SB - 20, top = wy + 34, bot = wy + wh - 22;
            Ui.text(ctx, textRenderer, tabNames[tab], cx, wy + 13, 0xFFFFFFFF);
            Ui.rect(ctx, cx, wy + 26, cw, 1, 0x18FFFFFF);

            List<Row> rows = tabs.get(tab);
            int yy = top - (int) scroll;
            for (Row r : rows) {
                if (r.show != null && !r.show.getAsBoolean()) { r.vis = false; continue; }
                r.vis = true;
                r.x = cx; r.y = yy; r.w = cw; r.h = r.height();
                yy += r.h + (r.kind == HEADER ? 0 : 2);
            }
            int contentH = yy + (int) scroll - top;
            float maxScroll = Math.max(0, contentH - (bot - top));
            scrollT = MathHelper.clamp(scrollT, 0, maxScroll);
            scroll += (scrollT - scroll) * 0.35f;
            if (Math.abs(scrollT - scroll) < 0.3f) scroll = scrollT;

            // ввод
            Row hover = null;
            if (in(mx, my, cx, top, cw, bot - top)) {
                for (Row r : rows) if (r.vis && r.kind != HEADER && in(mx, my, r.x, r.y, r.w, r.h - 2)) hover = r;
            }
            Runnable deferred = null;
            if (pressL && hover != null) {
                switch (hover.kind) {
                    case TOGGLE -> hover.setB.accept(!hover.getB.getAsBoolean());
                    case CYCLE -> hover.next.run();
                    case BUTTON -> deferred = hover.action;
                    case SLIDER -> activeSlider = hover;
                    case SWATCH -> {
                        Accent[] all = Accent.values();
                        int idx = (mx - (hover.x + 8)) / 24;
                        if (my >= hover.y + 15 && idx >= 0 && idx < all.length && mx < hover.x + 8 + all.length * 24) {
                            VisualsConfig.I.accent = all[idx];
                        }
                    }
                    default -> {}
                }
            }
            if (pressR && hover != null && hover.kind == CYCLE) hover.prev.run();
            if (!L) activeSlider = null;
            if (activeSlider != null && L) {
                Row s = activeSlider;
                int tw = 100, tx = s.x + s.w - 8 - tw;
                double t = MathHelper.clamp((mx - tx) / (double) tw, 0.0, 1.0);
                double v = s.min + t * (s.max - s.min);
                v = Math.round(v / s.step) * s.step;
                s.setF.accept(MathHelper.clamp(v, s.min, s.max));
            }

            // рисуем строки
            ctx.enableScissor(cx - 2, top, cx + cw + 2, bot);
            for (Row r : rows) {
                if (!r.vis || r.y + r.h < top || r.y > bot) continue;
                drawRow(ctx, r, r == hover || r == activeSlider);
            }
            ctx.disableScissor();

            // полоса прокрутки
            if (maxScroll > 0) {
                int trackH = bot - top;
                int th = Math.max(16, (int) (trackH * trackH / (float) contentH));
                int ty = top + (int) ((trackH - th) * (scroll / maxScroll));
                Ui.round(ctx, wx + ww - 6, ty, 3, th, 1, 0x40FFFFFF);
            }

            // подвал
            String hint = hover != null && hover.desc != null ? hover.desc : "";
            Ui.textFlat(ctx, textRenderer, hint, cx, wy + wh - 14, 0xFF8A909C);
            String esc = "ESC — закрыть";
            Ui.textFlat(ctx, textRenderer, esc, wx + ww - 10 - textRenderer.getWidth(esc), wy + wh - 14, 0xFF555B66);

            if (deferred != null) deferred.run();
        } finally {
            Ui.alpha = 1f;
        }
    }

    private void drawRow(DrawContext ctx, Row r, boolean hov) {
        int x = r.x, y = r.y, w = r.w, h = r.h - 2;
        if (r.kind == HEADER) {
            Ui.textFlat(ctx, textRenderer, r.name.toUpperCase(), x + 2, y + 8, Ui.accent());
            return;
        }
        Ui.round(ctx, x, y, w, h, 3, hov ? 0x2CFFFFFF : 0x1AFFFFFF);
        int ty = y + (h - 8) / 2;
        switch (r.kind) {
            case TOGGLE -> {
                Ui.text(ctx, textRenderer, r.name, x + 8, ty, 0xFFEAEAEA);
                boolean on = r.getB.getAsBoolean();
                if (r.anim < 0) r.anim = on ? 1f : 0f;
                r.anim += ((on ? 1f : 0f) - r.anim) * 0.3f;
                int px = x + w - 8 - 26, py = y + (h - 12) / 2;
                Ui.round(ctx, px, py, 26, 12, 6, Ui.mix(0xFF3A3F4B, Ui.accent(), r.anim));
                Ui.round(ctx, px + 2 + (int) (r.anim * 14), py + 2, 8, 8, 4, 0xFFFFFFFF);
            }
            case CYCLE -> {
                Ui.text(ctx, textRenderer, r.name, x + 8, ty, 0xFFEAEAEA);
                String v = r.label.get();
                int cw = textRenderer.getWidth(v) + 22;
                int px = x + w - 8 - cw;
                Ui.round(ctx, px, y + 3, cw, h - 6, 3, Ui.argb(Ui.accent() & 0xFFFFFF, 0.25f));
                Ui.textFlat(ctx, textRenderer, "<", px + 4, ty, 0xFF9AA0AC);
                Ui.textFlat(ctx, textRenderer, ">", px + cw - 10, ty, 0xFF9AA0AC);
                Ui.ctext(ctx, textRenderer, v, px + cw / 2, ty, 0xFFFFFFFF);
            }
            case SLIDER -> {
                Ui.text(ctx, textRenderer, r.name, x + 8, ty, 0xFFEAEAEA);
                int tw = 100, tx = x + w - 8 - tw, tyy = y + h / 2 - 2;
                double t = MathHelper.clamp((r.getF.getAsDouble() - r.min) / (r.max - r.min), 0.0, 1.0);
                Ui.round(ctx, tx, tyy, tw, 4, 2, 0xFF2A2E38);
                int fw = Math.max(3, (int) (tw * t));
                Ui.round(ctx, tx, tyy, fw, 4, 2, Ui.accent());
                Ui.round(ctx, tx + fw - 3, tyy - 2, 6, 8, 3, 0xFFFFFFFF);
                String v = r.label.get();
                Ui.text(ctx, textRenderer, v, tx - 8 - textRenderer.getWidth(v), ty, 0xFFBBBBBB);
            }
            case BUTTON -> {
                Ui.round(ctx, x, y, w, h, 3, hov ? Ui.argb(Ui.accent() & 0xFFFFFF, 0.5f) : Ui.argb(Ui.accent() & 0xFFFFFF, 0.22f));
                Ui.ctext(ctx, textRenderer, r.name, x + w / 2, ty, 0xFFFFFFFF);
            }
            case SWATCH -> {
                Ui.text(ctx, textRenderer, r.name, x + 8, y + 5, 0xFFEAEAEA);
                Accent[] all = Accent.values();
                for (int i = 0; i < all.length; i++) {
                    int sx = x + 8 + i * 24, sy = y + 16;
                    int col = all[i] == Accent.RAINBOW
                            ? Ui.hsv((System.currentTimeMillis() % 3000L) / 3000f + i * 0.1f, 0.6f, 1f)
                            : 0xFF000000 | all[i].rgb;
                    Ui.round(ctx, sx, sy, 20, 16, 3, col);
                    if (all[i] == VisualsConfig.I.accent) Ui.outline(ctx, sx - 2, sy - 2, 24, 20, 0xFFFFFFFF);
                }
            }
            default -> {}
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollT -= (float) verticalAmount * 26f;
        return true;
    }

    @Override
    public void removed() { VisualsConfig.save(); }

    @Override
    public boolean shouldPause() { return false; }
}
