package pl.makoto.createmarketplace.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import pl.makoto.createmarketplace.network.ServerVendorTradePayload;
import pl.makoto.createmarketplace.util.Coinage;

import java.util.ArrayList;
import java.util.List;

import static pl.makoto.createmarketplace.client.MarketTheme.*;

/**
 * Ekran handlu Server Vendor (gracz). Dwie kolumny: BUY (kupno od serwera)
 * i SELL (sprzedaż serwerowi). Jeśli jedna ze stron wyłączona — kolumna znika.
 * Cena wyświetlana jako ikona stacka + count + format spursowy (dla monet Numismatics).
 */
public class ServerVendorTradeScreen extends Screen {

    private static final int PANEL_W = 280;
    private static final int PANEL_H = 204;

    private final BlockPos pos;
    private final ItemStack tradeItem;
    private final ItemStack buyPrice;
    private final ItemStack sellPrice;
    private final boolean buyEnabled;
    private final boolean sellEnabled;

    private EditBox buyQtyBox;
    private EditBox sellQtyBox;
    private long lastActionAt = 0L;
    private String resultKey = null;
    private boolean resultOk = false;
    private int resultUnits = 0;

    private final List<Hit> hits = new ArrayList<>();

    private record Hit(int x1, int y1, int x2, int y2, Runnable action) {
        boolean in(double mx, double my) { return mx >= x1 && mx < x2 && my >= y1 && my < y2; }
    }

    public ServerVendorTradeScreen(BlockPos pos, ItemStack tradeItem, ItemStack buyPrice, ItemStack sellPrice,
                                    boolean buyEnabled, boolean sellEnabled) {
        super(Component.translatable("gui.create_marketplace.server_vendor.trade.title"));
        this.pos = pos;
        this.tradeItem = tradeItem;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.buyEnabled = buyEnabled;
        this.sellEnabled = sellEnabled;
    }

    @Override
    protected void init() {
        super.init();
        int px = (this.width - PANEL_W) / 2;
        int py = (this.height - PANEL_H) / 2;

        boolean both = buyEnabled && sellEnabled;
        int colW = both ? (PANEL_W - 24) / 2 : PANEL_W - 16;
        int buyColX = both ? px + 8 : (buyEnabled ? px + 8 : -10000);
        int sellColX = both ? px + 16 + colW : (sellEnabled ? px + 8 : -10000);

        // qtyY w renderColumn: priceIconY + 36, gdzie priceIconY = iconY + 40, iconY = py + 22 + 14
        int qtyY = py + 22 + 14 + 40 + 36;
        if (buyEnabled) {
            int boxX = buyColX + colW / 2 - 18;
            buyQtyBox = new EditBox(this.font, boxX + 4, qtyY + 4, 28, 8,
                    Component.translatable("gui.create_marketplace.server_vendor.trade.quantity"));
            buyQtyBox.setMaxLength(5);
            buyQtyBox.setValue("1");
            buyQtyBox.setBordered(false);
            buyQtyBox.setTextColor(0xD8C79A);
            this.addRenderableWidget(buyQtyBox);
        }
        if (sellEnabled) {
            int boxX = sellColX + colW / 2 - 18;
            sellQtyBox = new EditBox(this.font, boxX + 4, qtyY + 4, 28, 8,
                    Component.translatable("gui.create_marketplace.server_vendor.trade.quantity"));
            sellQtyBox.setMaxLength(5);
            sellQtyBox.setValue("1");
            sellQtyBox.setBordered(false);
            sellQtyBox.setTextColor(0xD8C79A);
            this.addRenderableWidget(sellQtyBox);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g, mx, my, pt);
        hits.clear();
        int px = (this.width - PANEL_W) / 2;
        int py = (this.height - PANEL_H) / 2;

        bevel(g, px, py, px + PANEL_W, py + PANEL_H, PARCH, PARCH, FRAME);
        g.fill(px + 1, py + 1, px + PANEL_W - 1, py + 16, PATINA);
        g.drawString(this.font, this.title, px + 8, py + 5, WHITE, false);

        boolean both = buyEnabled && sellEnabled;
        int colW = both ? (PANEL_W - 24) / 2 : PANEL_W - 16;
        int buyColX = both ? px + 8 : (buyEnabled ? px + 8 : -10000);
        int sellColX = both ? px + 16 + colW : (sellEnabled ? px + 8 : -10000);

        if (buyEnabled) renderColumn(g, mx, my, buyColX, py + 22, colW, true);
        if (sellEnabled) renderColumn(g, mx, my, sellColX, py + 22, colW, false);
        if (both) {
            int sepX = px + PANEL_W / 2;
            g.fill(sepX - 1, py + 22, sepX, py + PANEL_H - 8, LINE);
        }

        if (resultKey != null) {
            int color = resultOk ? PATINA_DARK : RED;
            String txt = Component.translatable(resultKey, resultUnits).getString();
            g.drawString(this.font, txt, px + (PANEL_W - this.font.width(txt)) / 2, py + PANEL_H - 14, color, false);
        }

        if (buyQtyBox != null)  buyQtyBox.render(g, mx, my, pt);
        if (sellQtyBox != null) sellQtyBox.render(g, mx, my, pt);
    }

    private void renderColumn(GuiGraphics g, int mx, int my, int x, int y, int w, boolean buy) {
        String title = Component.translatable(buy
                ? "gui.create_marketplace.server_vendor.trade.buy"
                : "gui.create_marketplace.server_vendor.trade.sell").getString();
        g.drawString(this.font, title, x + (w - this.font.width(title)) / 2, y, INK, false);

        // ---- Trade item (ikona + nazwa) ----
        int iconX = x + w / 2 - 12;
        int iconY = y + 14;
        slot(g, iconX, iconY);
        if (!tradeItem.isEmpty()) {
            g.renderItem(tradeItem, iconX + 1, iconY + 1);
            g.renderItemDecorations(this.font, tradeItem, iconX + 1, iconY + 1);
        }
        String name = tradeItem.isEmpty() ? "—" : tradeItem.getHoverName().getString();
        String trimmedName = trim(this.font, name, w - 8);
        g.drawString(this.font, trimmedName,
                x + (w - this.font.width(trimmedName)) / 2, iconY + 22, INK, false);

        // ---- Strzałka wskazująca w dół ----
        String arrow = buy ? "↑" : "↓";
        g.drawString(this.font, arrow, x + (w - this.font.width(arrow)) / 2, iconY + 32, INK_SOFT, false);

        // ---- Cena (ikona ceny) ----
        ItemStack priceStack = buy ? buyPrice : sellPrice;
        int priceIconY = iconY + 40;
        int priceIconX = x + w / 2 - 12;
        slot(g, priceIconX, priceIconY);
        if (!priceStack.isEmpty()) {
            g.renderItem(priceStack, priceIconX + 1, priceIconY + 1);
            g.renderItemDecorations(this.font, priceStack, priceIconX + 1, priceIconY + 1);
        }
        // Tekst pomocniczy (= X spurs) tylko dla monet
        if (!priceStack.isEmpty() && Coinage.isNumismaticsCoin(priceStack)) {
            int spurs = priceStack.getCount() * Coinage.coinValueOf(priceStack);
            String help = "= " + Coinage.formatPlain(spurs);
            String trimmed = trim(this.font, help, w - 8);
            g.drawString(this.font, trimmed,
                    x + (w - this.font.width(trimmed)) / 2, priceIconY + 22, INK_SOFT, false);
        } else if (priceStack.isEmpty()) {
            String none = "—";
            g.drawString(this.font, none, x + (w - this.font.width(none)) / 2, priceIconY + 22, INK_SOFT, false);
        }

        // ---- Rząd ilości [-][box][+][MAX] ----
        EditBox box = buy ? buyQtyBox : sellQtyBox;
        int qtyY = priceIconY + 36;
        int boxX = x + w / 2 - 18;
        g.fill(boxX, qtyY, boxX + 36, qtyY + 16, FRAME);
        g.fill(boxX + 1, qtyY + 1, boxX + 35, qtyY + 15, 0xFF211B12);

        int minusX = boxX - 18;
        boolean hMinus = mx >= minusX && mx < minusX + 16 && my >= qtyY && my < qtyY + 16;
        bevel(g, minusX, qtyY, minusX + 16, qtyY + 16, hMinus ? BRASS_LIGHT : BRASS, BRASS_DARK, FRAME);
        g.drawString(this.font, "-", minusX + 6, qtyY + 4, FRAME, false);
        hits.add(new Hit(minusX, qtyY, minusX + 16, qtyY + 16, () -> adjustQty(box, -1)));

        int plusX = boxX + 38;
        boolean hPlus = mx >= plusX && mx < plusX + 16 && my >= qtyY && my < qtyY + 16;
        bevel(g, plusX, qtyY, plusX + 16, qtyY + 16, hPlus ? BRASS_LIGHT : BRASS, BRASS_DARK, FRAME);
        g.drawString(this.font, "+", plusX + 5, qtyY + 4, FRAME, false);
        hits.add(new Hit(plusX, qtyY, plusX + 16, qtyY + 16, () -> adjustQty(box, +1)));

        int maxX = plusX + 18;
        boolean hMax = mx >= maxX && mx < maxX + 24 && my >= qtyY && my < qtyY + 16;
        bevel(g, maxX, qtyY, maxX + 24, qtyY + 16, hMax ? BRASS_LIGHT : BRASS, BRASS_DARK, FRAME);
        String maxLbl = Component.translatable("gui.create_marketplace.server_vendor.trade.max").getString();
        g.drawString(this.font, maxLbl, maxX + (24 - this.font.width(maxLbl)) / 2, qtyY + 4, FRAME, false);
        hits.add(new Hit(maxX, qtyY, maxX + 24, qtyY + 16, () -> setQty(box, computeMax(buy))));

        // ---- Total: ikona + count + helper (= X spurs) ----
        int qty = readQty(box);
        int totalY = qtyY + 22;
        int totalIconX = x + w / 2 - 12;
        slot(g, totalIconX, totalY);
        if (!priceStack.isEmpty() && qty > 0) {
            int totalCount = priceStack.getCount() * qty;
            int displayCount = Math.min(totalCount, priceStack.getMaxStackSize());
            ItemStack disp = priceStack.copyWithCount(displayCount);
            g.renderItem(disp, totalIconX + 1, totalY + 1);
            // Sami rysujemy count żeby pokazać prawdziwą sumę (nie ograniczoną maxStackSize)
            String countStr = "×" + totalCount;
            g.drawString(this.font, countStr, totalIconX + 22, totalY + 5, INK, false);
            if (Coinage.isNumismaticsCoin(priceStack)) {
                int totalSpurs = totalCount * Coinage.coinValueOf(priceStack);
                String tspurs = "= " + Coinage.formatPlain(totalSpurs);
                String trimmed = trim(this.font, tspurs, w - 8);
                g.drawString(this.font, trimmed,
                        x + (w - this.font.width(trimmed)) / 2, totalY + 22, INK_SOFT, false);
            }
        }

        // ---- Action button ----
        int btnW = Math.min(w - 16, 90);
        int btnX = x + (w - btnW) / 2;
        int btnY = totalY + 34;
        int btnH = 16;
        boolean hAct = mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH;
        int top = buy ? (hAct ? PATINA_LIGHT : PATINA) : (hAct ? BRASS_LIGHT : BRASS);
        int bot = buy ? PATINA_DARK : BRASS_DARK;
        bevel(g, btnX, btnY, btnX + btnW, btnY + btnH, top, bot, FRAME);
        String actLbl = Component.translatable(buy
                ? "gui.create_marketplace.server_vendor.trade.buy"
                : "gui.create_marketplace.server_vendor.trade.sell").getString();
        g.drawString(this.font, actLbl, btnX + (btnW - this.font.width(actLbl)) / 2, btnY + 4,
                buy ? WHITE : FRAME, false);
        hits.add(new Hit(btnX, btnY, btnX + btnW, btnY + btnH, () -> sendTrade(buy, qty)));
    }

    /** "5× cog (= 320 spurs)" lub "3× emerald" */
    private String formatPrice(ItemStack price) {
        if (price.isEmpty()) return "—";
        if (Coinage.isNumismaticsCoin(price)) {
            int spurs = price.getCount() * Coinage.coinValueOf(price);
            return price.getCount() + "× " + price.getHoverName().getString()
                    + " (= " + Coinage.formatPlain(spurs) + ")";
        }
        return price.getCount() + "× " + price.getHoverName().getString();
    }

    private String formatTotal(ItemStack price, int qty) {
        if (price.isEmpty()) return "—";
        int totalCount = Math.max(0, qty) * price.getCount();
        if (Coinage.isNumismaticsCoin(price)) {
            int spurs = totalCount * Coinage.coinValueOf(price);
            return Coinage.formatPlain(spurs);
        }
        return totalCount + "× " + price.getHoverName().getString();
    }

    private void adjustQty(EditBox box, int delta) {
        if (box == null) return;
        int v = Math.max(1, readQty(box) + delta);
        box.setValue(Integer.toString(v));
    }

    private void setQty(EditBox box, int qty) {
        if (box == null) return;
        box.setValue(Integer.toString(Math.max(1, qty)));
    }

    private int readQty(EditBox box) {
        if (box == null) return 1;
        try { return Math.max(1, Integer.parseInt(box.getValue().trim())); }
        catch (NumberFormatException e) { return 1; }
    }

    private int computeMax(boolean buy) {
        if (this.minecraft == null || this.minecraft.player == null) return 1;
        var inv = this.minecraft.player.getInventory();
        ItemStack price = buy ? buyPrice : sellPrice;
        if (buy) {
            if (price.isEmpty()) return 64;
            if (Coinage.isNumismaticsCoin(price)) {
                int unitSpurs = price.getCount() * Coinage.coinValueOf(price);
                if (unitSpurs <= 0) return 64;
                return Math.max(1, Coinage.countSpursInInventory(inv) / unitSpurs);
            } else {
                int unit = price.getCount();
                if (unit <= 0) return 64;
                return Math.max(1, Coinage.countCustom(inv, price) / unit);
            }
        } else {
            int unit = Math.max(1, tradeItem.getCount());
            int have = 0;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, tradeItem)) have += s.getCount();
            }
            return Math.max(1, have / unit);
        }
    }

    private void sendTrade(boolean buy, int qty) {
        long now = System.currentTimeMillis();
        if (now - lastActionAt < 200) return;
        lastActionAt = now;
        PacketDistributor.sendToServer(new ServerVendorTradePayload(pos, buy, Math.max(1, qty)));
    }

    public void onResult(boolean ok, String key, int units) {
        this.resultOk = ok;
        this.resultKey = key;
        this.resultUnits = units;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (button == 0) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (h.in(mx, my)) { h.action().run(); return true; }
            }
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
