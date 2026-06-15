package pl.makoto.createmarketplace.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Wspólny motyw GUI Create: Marketplace — paleta "mosiądz + patyna" oraz
 * drobne helpery rysujące, używane przez wszystkie ekrany rynku.
 */
public final class MarketTheme {
    private MarketTheme() {}

    public static final int FRAME       = 0xFF2C1F10;
    public static final int BRASS       = 0xFFD7A648;
    public static final int BRASS_DARK  = 0xFFB9842F;
    public static final int BRASS_LIGHT = 0xFFECC873;
    public static final int PARCH       = 0xFFECE0C2;
    public static final int PARCH_2     = 0xFFE4D4AF;
    public static final int PARCH_ROW   = 0xFFF2E8CD;
    public static final int PARCH_HOVER = 0xFFFBF3DA;
    public static final int PATINA      = 0xFF3A9189;
    public static final int PATINA_DARK = 0xFF2A6B64;
    public static final int PATINA_LIGHT= 0xFF5CB8AC;
    public static final int INK         = 0xFF43331C;
    public static final int INK_SOFT    = 0xFF8A7148;
    public static final int INK_MUTE    = 0xFF9A875F;
    public static final int LINK        = 0xFF2F6DB5;
    public static final int LINE        = 0xFFCBB98A;
    public static final int WHITE       = 0xFFFFFFFF;
    public static final int RED         = 0xFFB23B2E;
    public static final int RED_DARK    = 0xFF8A2A1F;
    public static final int RED_LIGHT   = 0xFFD9695B;
    public static final int RED_BORDER  = 0xFF5A1A12;

    /** prostokąt z fazowaną ramką: jasny→ciemny gradient + obramowanie */
    public static void bevel(GuiGraphics g, int x1, int y1, int x2, int y2, int top, int bottom, int border) {
        g.fill(x1, y1, x2, y2, border);
        g.fillGradient(x1 + 1, y1 + 1, x2 - 1, y2 - 1, top, bottom);
    }

    /** slot przedmiotu (gniazdo ekwipunku) 18×18 w (x,y) */
    public static void slot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF6E5A2C);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF2B2336);
    }

    /** skraca tekst do szerokości w px, dodając wielokropek */
    public static String trim(Font font, String s, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (font.width(s) <= maxWidth) return s;
        return font.plainSubstrByWidth(s, maxWidth - font.width("…")) + "…";
    }

    /** deterministyczny kolor "awatara" z nazwy (gdy brak skina) */
    public static int avatarColor(String key) {
        int h = key.hashCode();
        int r = 80 + ((h >> 16) & 0x7F);
        int g = 60 + ((h >> 8) & 0x7F);
        int b = 50 + (h & 0x7F);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
