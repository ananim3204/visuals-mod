package dev.netrixvisual;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Новое главное меню: анимированный фон с северным сиянием, пиксельный логотип и стеклянные кнопки. */
public class NetrixTitleScreen extends Screen {
    private static final String[] LABELS = {"Одиночная игра", "Сетевая игра", "Настройки Minecraft", "Netrixvisual", "Выйти"};
    private final float[] hover = new float[LABELS.length];
    private final long start = System.nanoTime();
    private boolean pl = true;

    public NetrixTitleScreen() { super(Text.literal("Netrixvisual")); }

    // Пиксельный шрифт 5x7 только для букв логотипа
    private static final String[] G_N = {"X...X", "XX..X", "X.X.X", "X..XX", "X...X", "X...X", "X...X"};
    private static final String[] G_E = {"XXXXX", "X....", "X....", "XXXX.", "X....", "X....", "XXXXX"};
    private static final String[] G_T = {"XXXXX", "..X..", "..X..", "..X..", "..X..", "..X..", "..X.."};
    private static final String[] G_R = {"XXXX.", "X...X", "X...X", "XXXX.", "X.X..", "X..X.", "X...X"};
    private static final String[] G_I = {"XXXXX", "..X..", "..X..", "..X..", "..X..", "..X..", "XXXXX"};
    private static final String[] G_X = {"X...X", "X...X", ".X.X.", "..X..", ".X.X.", "X...X", "X...X"};
    private static final String[] G_V = {"X...X", "X...X", "X...X", "X...X", "X...X", ".X.X.", "..X.."};
    private static final String[] G_S = {".XXXX", "X....", "X....", ".XXX.", "....X", "....X", "XXXX."};
    private static final String[] G_U = {"X...X", "X...X", "X...X", "X...X", "X...X", "X...X", ".XXX."};
    private static final String[] G_A = {".XXX.", "X...X", "X...X", "XXXXX", "X...X", "X...X", "X...X"};
    private static final String[] G_L = {"X....", "X....", "X....", "X....", "X....", "X....", "XXXXX"};

    private static String[] glyph(char ch) {
        return switch (ch) {
            case 'N' -> G_N; case 'E' -> G_E; case 'T' -> G_T; case 'R' -> G_R; case 'I' -> G_I; case 'X' -> G_X;
            case 'V' -> G_V; case 'S' -> G_S; case 'U' -> G_U; case 'A' -> G_A; default -> G_L;
        };
    }

    private int drawWord(DrawContext ctx, String word, int cx, int y, int cs, float time, float hueBase) {
        int lw = 5 * cs, gap = cs;
        int total = word.length() * lw + (word.length() - 1) * gap;
        int x = cx - total / 2;
        for (int i = 0; i < word.length(); i++) {
            String[] g = glyph(word.charAt(i));
            for (int ro = 0; ro < 7; ro++) {
                for (int co = 0; co < 5; co++) {
                    if (g[ro].charAt(co) != 'X') continue;
                    int px = x + co * cs, py = y + ro * cs;
                    float shimmer = (float) Math.sin(time * 2.0 + (px + py) * 0.03) * 0.5f + 0.5f;
                    int colr = Ui.mix(Ui.accent(hueBase + (px - (cx - total / 2)) / (float) total * 0.5f), 0xFFFFFFFF, 0.15f + 0.25f * shimmer);
                    ctx.fill(px + cs / 2 + 1, py + cs / 2 + 1, px + cs + cs / 2 + 1, py + cs + cs / 2 + 1, 0x66000000);
                    ctx.fill(px, py, px + cs, py + cs, colr);
                }
            }
            x += lw + gap;
        }
        return total;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float time = (System.nanoTime() - start) / 1e9f;
        boolean L = GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean press = L && !pl;
        pl = L;

        int acc = Ui.accent() & 0xFFFFFF;
        ctx.fillGradient(0, 0, width, height, Ui.mix(0xFF07080F, 0xFF000000 | acc, 0.14f), Ui.mix(0xFF0B0D18, 0xFF000000 | acc, 0.32f));

        // северное сияние
        for (int x = 0; x < width; x += 4) {
            float cy = height * 0.42f + (float) Math.sin(x * 0.012 + time * 0.6) * height * 0.08f + (float) Math.sin(x * 0.031 - time * 0.9) * height * 0.03f;
            float hh = height * 0.14f * (0.8f + 0.2f * (float) Math.sin(x * 0.02 + time));
            int col = Ui.accent(x / (float) width * 0.5f) & 0xFFFFFF;
            ctx.fillGradient(x, (int) (cy - hh), x + 4, (int) cy, Ui.argb(col, 0f), Ui.argb(col, 0.14f));
            ctx.fillGradient(x, (int) cy, x + 4, (int) (cy + hh), Ui.argb(col, 0.14f), Ui.argb(col, 0f));
        }

        // звёздочки / пылинки
        for (int i = 0; i < 70; i++) {
            float rx = frac(i * 12.9898f), ry = frac(i * 78.233f), sp = 4f + frac(i * 3.7f) * 14f;
            float x = (rx * width + (float) Math.sin(time * 0.4 + i) * 6f);
            float y = ((ry * height - time * sp) % height + height) % height;
            float tw = 0.35f + 0.65f * (0.5f + 0.5f * (float) Math.sin(time * 1.5 + i * 2.1));
            int sz = i % 9 == 0 ? 2 : 1;
            ctx.fill((int) x, (int) y, (int) x + sz, (int) y + sz, Ui.argb(Ui.accent(rx * 0.4f) & 0xFFFFFF, tw * 0.8f));
        }

        // логотип
        int cs = Math.max(3, Math.min(6, height / 58));
        int logoY = Math.max(10, height / 2 - 6 * (cs * 7) / 2 - 20);
        logoY = Math.max(8, height / 7);
        drawWord(ctx, "NETRIX", width / 2, logoY, cs, time, 0f);
        int cs2 = Math.max(2, cs - 2);
        int vy = logoY + 7 * cs + cs * 2;
        int vw = drawWord(ctx, "VISUAL", width / 2, vy, cs2, time, 0.3f);
        int lineY = vy + 7 * cs2 + 6;
        for (int x = width / 2 - vw / 2; x < width / 2 + vw / 2; x += 2) {
            ctx.fill(x, lineY, x + 2, lineY + 1, Ui.argb(Ui.accent((x - width / 2) / (float) width) & 0xFFFFFF, 0.8f));
        }

        // кнопки
        int bw = Math.min(210, width - 40), bh = 22, gap = 7;
        int top = lineY + 18;
        int bx = (width - bw) / 2;
        int clicked = -1;
        for (int i = 0; i < LABELS.length; i++) {
            int by = top + i * (bh + gap);
            boolean hov = mx >= bx && mx < bx + bw && my >= by && my < by + bh;
            hover[i] += ((hov ? 1f : 0f) - hover[i]) * 0.25f;
            float h = hover[i];
            Ui.round(ctx, bx - (int) (h * 4), by, bw + (int) (h * 8), bh, 4, Ui.mix(0xAA10131C, Ui.argb(Ui.accent(i * 0.1f) & 0xFFFFFF, 0.6f), h));
            Ui.round(ctx, bx - (int) (h * 4), by, 3, bh, 1, Ui.accent(i * 0.1f));
            Ui.ctext(ctx, textRenderer, LABELS[i], width / 2, by + 7, 0xFFFFFFFF);
            if (hov && press) clicked = i;
        }

        String ver = "Netrixvisual 2.0  •  Minecraft 1.21.11";
        Ui.textFlat(ctx, textRenderer, ver, 6, height - 12, 0xFF70768A);

        if (clicked >= 0) {
            switch (clicked) {
                case 0 -> mc.setScreen(new SelectWorldScreen(this));
                case 1 -> mc.setScreen(new MultiplayerScreen(this));
                case 2 -> mc.setScreen(new OptionsScreen(this, mc.options));
                case 3 -> mc.setScreen(new VisualsScreen());
                default -> mc.scheduleStop();
            }
        }
    }

    private static float frac(float v) {
        double s = Math.sin(v) * 43758.5453;
        return (float) (s - Math.floor(s));
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean shouldPause() { return false; }
}
