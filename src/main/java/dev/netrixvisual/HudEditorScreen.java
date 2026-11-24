package dev.netrixvisual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Редактор HUD: тащишь виджеты мышью. ПКМ — включить/выключить. */
public class HudEditorScreen extends Screen {
    private final Screen parent;
    private HudWidget drag;
    private int offX, offY;
    private boolean pl = true, pr = true;
    private boolean guideV, guideH;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("Редактор HUD"));
        this.parent = parent;
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private int px(HudWidget w) {
        float fx = VisualsConfig.I.state(w).x;
        return Math.max(0, Math.min((int) (fx * width), Math.max(0, width - w.w)));
    }

    private int py(HudWidget w) {
        float fy = VisualsConfig.I.state(w).y;
        return Math.max(0, Math.min((int) (fy * height), Math.max(0, height - w.h)));
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        long win = mc.getWindow().getHandle();
        boolean L = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean R = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean pressL = L && !pl, pressR = R && !pr;
        pl = L; pr = R;

        ctx.fill(0, 0, width, height, 0x99000000);

        // кнопки снизу
        int bw = 120, bh = 20, by = height - 30;
        int doneX = width / 2 - bw - 4, resetX = width / 2 + 4;
        boolean overDone = in(mx, my, doneX, by, bw, bh), overReset = in(mx, my, resetX, by, bw, bh);
        boolean overButton = overDone || overReset;

        // выбор виджета под курсором (верхний — последний)
        HudWidget hover = null;
        if (drag == null && !overButton) {
            for (HudWidget w : HudWidgets.ALL) {
                if (w.w > 0 && in(mx, my, px(w), py(w), w.w, w.h)) hover = w;
            }
        }
        if (pressL && !overButton && hover != null) {
            drag = hover; offX = mx - px(hover); offY = my - py(hover);
        }
        if (pressR && hover != null) {
            VisualsConfig.WidgetState st = VisualsConfig.I.state(hover);
            st.on = !st.on;
        }
        if (!L) drag = null;

        guideV = guideH = false;
        if (drag != null) {
            int nx = Math.max(0, Math.min(mx - offX, width - drag.w));
            int ny = Math.max(0, Math.min(my - offY, height - drag.h));
            int cxs = (width - drag.w) / 2, cys = (height - drag.h) / 2;
            if (Math.abs(nx - cxs) < 5) { nx = cxs; guideV = true; }
            if (Math.abs(ny - cys) < 5) { ny = cys; guideH = true; }
            if (nx < 5) nx = 0;
            if (ny < 5) ny = 0;
            if (nx > width - drag.w - 5) nx = width - drag.w;
            if (ny > height - drag.h - 5) ny = height - drag.h;
            VisualsConfig.WidgetState st = VisualsConfig.I.state(drag);
            st.x = nx / (float) width;
            st.y = ny / (float) height;
        }

        if (guideV) Ui.rect(ctx, width / 2, 0, 1, height, Ui.argb(Ui.accent() & 0xFFFFFF, 0.6f));
        if (guideH) Ui.rect(ctx, 0, height / 2, width, 1, Ui.argb(Ui.accent() & 0xFFFFFF, 0.6f));

        for (HudWidget w : HudWidgets.ALL) {
            VisualsConfig.WidgetState st = VisualsConfig.I.state(w);
            int x = px(w), y = py(w);
            Ui.alpha = st.on ? 1f : 0.35f;
            w.draw(ctx, mc, x, y, true);
            Ui.alpha = 1f;
            int oc = w == drag ? 0xFFFFFFFF : w == hover ? Ui.accent() : st.on ? 0x30FFFFFF : 0x55FF5555;
            Ui.outline(ctx, x - 1, y - 1, w.w + 2, w.h + 2, oc);
            if (w == hover || w == drag) {
                String label = w.name + (st.on ? "" : "  (выкл)");
                Ui.text(ctx, textRenderer, label, x, Math.max(2, y - 11), 0xFFFFFFFF);
            }
        }

        if (mc.player == null) {
            Ui.ctext(ctx, textRenderer, "Зайди в мир, чтобы двигать виджеты", width / 2, height / 2, 0xFFFFD27F);
        }
        Ui.ctext(ctx, textRenderer, "Редактор HUD", width / 2, 10, 0xFFFFFFFF);
        Ui.ctext(ctx, textRenderer, "ЛКМ — перетащить   •   ПКМ — вкл/выкл виджет   •   ESC — выйти", width / 2, 22, 0xFFAAAAAA);

        button(ctx, doneX, by, bw, bh, "Готово", overDone);
        button(ctx, resetX, by, bw, bh, "Сбросить позиции", overReset);
        if (pressL && overDone) close();
        else if (pressL && overReset) HudWidgets.resetPositions();
    }

    private void button(DrawContext ctx, int x, int y, int w, int h, String s, boolean hov) {
        Ui.round(ctx, x, y, w, h, 3, hov ? Ui.argb(Ui.accent() & 0xFFFFFF, 0.55f) : 0xCC1A1D26);
        Ui.ctext(ctx, textRenderer, s, x + w / 2, y + 6, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        VisualsConfig.save();
        MinecraftClient.getInstance().setScreen(parent);
    }

    @Override
    public void removed() { VisualsConfig.save(); }

    @Override
    public boolean shouldPause() { return false; }
}
