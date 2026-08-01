package pl.makoto.createmarketplace.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import pl.makoto.createmarketplace.data.StockInfo;

import java.util.List;

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
    public static final int STOCK_OK      = 0xFF3FA34D;
    public static final int STOCK_STALE   = 0xFFD9A521;
    public static final int STOCK_UNKNOWN = 0xFF8A7148;

    // --- znacznik stanu magazynowego ---

    /**
     * Kolor kropki stanu. Świeże "dostępne" jest zielone, nieświeże — bursztynowe,
     * żeby gracz od razu widział różnicę między odczytem potwierdzonym a starym.
     * Pusty jest czerwony niezależnie od wieku: pomyłka w tę stronę tylko zniechęca
     * do podróży, a w drugą — wysyła gracza po nic.
     */
    public static int stockColor(StockInfo s) {
        if (!s.known()) return STOCK_UNKNOWN;
        if (s.infinite()) return PATINA;
        if (s.empty()) return RED;
        return s.fresh() ? STOCK_OK : STOCK_STALE;
    }

    /** Kropka 5×5 z ciemną obwódką — rysowana w rogu slotu przedmiotu. */
    public static void stockDot(GuiGraphics g, int x, int y, StockInfo s) {
        g.fill(x, y, x + 5, y + 5, FRAME);
        g.fill(x + 1, y + 1, x + 4, y + 4, stockColor(s));
    }

    /**
     * Opis stanu do dymka. Zawsze dwuwierszowy dla odczytów znanych: co widzieliśmy
     * i KIEDY. Bez drugiego wiersza gracz nie ma jak ocenić, czy informacja jest
     * jeszcze coś warta.
     */
    public static List<Component> stockTooltip(StockInfo s) {
        if (!s.known()) {
            return List.of(Component.translatable("gui.create_marketplace.stock.unknown")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (s.infinite()) {
            return List.of(Component.translatable("gui.create_marketplace.stock.unlimited")
                    .withStyle(ChatFormatting.AQUA));
        }
        Component head;
        if (s.empty()) {
            head = Component.translatable("gui.create_marketplace.stock.empty").withStyle(ChatFormatting.RED);
        } else if (s.fresh()) {
            head = Component.translatable("gui.create_marketplace.stock.available", s.units())
                    .withStyle(ChatFormatting.GREEN);
        } else {
            // celowo inne słowa niż przy świeżym odczycie — "ostatnio widziano" nie obiecuje,
            // że towar nadal tam jest
            head = Component.translatable("gui.create_marketplace.stock.available_stale", s.units())
                    .withStyle(ChatFormatting.YELLOW);
        }
        return List.of(head, formatAge(s.ageSeconds()).withStyle(ChatFormatting.DARK_GRAY));
    }

    private static net.minecraft.network.chat.MutableComponent formatAge(int seconds) {
        if (seconds < 60) {
            return Component.translatable("gui.create_marketplace.stock.age_now");
        }
        if (seconds < 3600) {
            return Component.translatable("gui.create_marketplace.stock.age_min", seconds / 60);
        }
        return Component.translatable("gui.create_marketplace.stock.age_hour", seconds / 3600);
    }

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
