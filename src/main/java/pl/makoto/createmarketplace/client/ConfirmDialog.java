package pl.makoto.createmarketplace.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import static pl.makoto.createmarketplace.client.MarketTheme.*;

/**
 * Modalne okno potwierdzenia (np. usunięcia) — wspólne dla ekranów rynku.
 * Ekran deleguje render() oraz mouseClicked()/keyPressed(); gdy aktywne,
 * pochłania wejście.
 */
public class ConfirmDialog {
    private Component msg;
    private Runnable action;
    private int yesX1, yesY1, yesX2, yesY2, noX1, noY1, noX2, noY2;

    public boolean active() {
        return msg != null;
    }

    public void open(Component message, Runnable onConfirm) {
        this.msg = message;
        this.action = onConfirm;
    }

    public void close() {
        this.msg = null;
        this.action = null;
    }

    public void render(GuiGraphics g, Font font, int width, int height, int mx, int my) {
        if (msg == null) return;
        g.fill(0, 0, width, height, 0xB0000000);
        int pw = 220, ph = 70, px = (width - pw) / 2, py = (height - ph) / 2;
        bevel(g, px, py, px + pw, py + ph, PARCH, PARCH, FRAME);
        g.fill(px + 1, py + 1, px + pw - 1, py + 15, PATINA);
        g.drawString(font, Component.translatable("gui.create_marketplace.confirm.title"), px + 6, py + 4, WHITE, false);
        g.drawCenteredString(font, trim(font, msg.getString(), pw - 14), px + pw / 2, py + 26, INK);

        int bw = 90, bh = 18, by = py + ph - bh - 8;
        yesX1 = px + 10; yesY1 = by; yesX2 = yesX1 + bw; yesY2 = by + bh;
        noX1 = px + pw - 10 - bw; noY1 = by; noX2 = px + pw - 10; noY2 = by + bh;
        boolean hYes = mx >= yesX1 && mx < yesX2 && my >= yesY1 && my < yesY2;
        boolean hNo = mx >= noX1 && mx < noX2 && my >= noY1 && my < noY2;
        bevel(g, yesX1, yesY1, yesX2, yesY2, hYes ? RED_LIGHT : RED, RED_DARK, RED_BORDER);
        bevel(g, noX1, noY1, noX2, noY2, hNo ? BRASS_LIGHT : BRASS, BRASS_DARK, FRAME);
        String yes = Component.translatable("gui.create_marketplace.confirm.yes").getString();
        String no = Component.translatable("gui.create_marketplace.confirm.no").getString();
        g.drawString(font, yes, yesX1 + (bw - font.width(yes)) / 2, yesY1 + 5, WHITE, false);
        g.drawString(font, no, noX1 + (bw - font.width(no)) / 2, noY1 + 5, 0xFF2C1F10, false);
    }

    /** @return true gdy modal aktywny (kliknięcie pochłonięte) */
    public boolean mouseClicked(double mx, double my) {
        if (msg == null) return false;
        if (mx >= yesX1 && mx < yesX2 && my >= yesY1 && my < yesY2) {
            Runnable a = action;
            close();
            if (a != null) a.run();
        } else if (mx >= noX1 && mx < noX2 && my >= noY1 && my < noY2) {
            close();
        }
        return true;
    }

    /** @return true gdy modal aktywny (klawisz pochłonięty) */
    public boolean keyPressed(int key) {
        if (msg == null) return false;
        if (key == GLFW.GLFW_KEY_ESCAPE) close();
        return true;
    }
}
