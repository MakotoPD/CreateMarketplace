package pl.makoto.createmarketplace.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import pl.makoto.createmarketplace.menu.ServerVendorAdminMenu;
import pl.makoto.createmarketplace.network.SaveServerVendorPayload;
import pl.makoto.createmarketplace.util.Coinage;

import java.util.ArrayList;
import java.util.List;

import static pl.makoto.createmarketplace.client.MarketTheme.*;

/**
 * Prosty ekran konfiguracji Server Vendor (admin). Trzy sloty na przedmiot/kupno/sprzedaż.
 * Admin wkłada monety lub przedmioty bezpośrednio; count w slocie definuje cenę.
 * Każdy slot ma [+]/[−]/[Clear]; sloty sell mają dodatkowo [Auto 60%].
 */
public class ServerVendorAdminScreen extends AbstractContainerScreen<ServerVendorAdminMenu> {

    private static final int PANEL_W = 232;
    private static final int PANEL_H = 232;

    private final List<Hit> hits = new ArrayList<>();

    private record Hit(int x1, int y1, int x2, int y2, Runnable action) {
        boolean in(double mx, double my) { return mx >= x1 && mx < x2 && my >= y1 && my < y2; }
    }

    public ServerVendorAdminScreen(ServerVendorAdminMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
        this.titleLabelX = -10000;
        this.inventoryLabelX = -10000;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        hits.clear();
        super.render(g, mx, my, pt);
        renderOverlay(g, mx, my);
        this.renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        // Panel + nagłówek
        bevel(g, x, y, x + PANEL_W, y + PANEL_H, PARCH, PARCH, FRAME);
        g.fill(x + 1, y + 1, x + PANEL_W - 1, y + 16, PATINA);
        g.drawString(this.font, this.title.getString(), x + 8, y + 5, WHITE, false);

        // Tła slotów (bg na slot.x-1, slot.y-1)
        slot(g, x + 23, y + 23);   // tradeSlot @ (24, 24)
        slot(g, x + 23, y + 51);   // buyPriceSlot @ (24, 52)
        slot(g, x + 23, y + 79);   // sellPriceSlot @ (24, 80)

        // Tła ekwipunku — menu inv @ (47, 132); ramki na (46, 131).
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                slot(g, x + 46 + col * 18, y + 131 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            slot(g, x + 46 + col * 18, y + 131 + 58);
        }

        // Separator nad sekcją inv
        g.fill(x + 8, y + 124, x + PANEL_W - 8, y + 125, LINE);
    }

    private void renderOverlay(GuiGraphics g, int mx, int my) {
        int x = leftPos, y = topPos;

        // ROW 1: Trade item
        renderRow(g, mx, my, x, y + 24,
                Component.translatable("gui.create_marketplace.server_vendor.admin.trade_item").getString(),
                menu.getCurrentTradeItem(), false,
                ServerVendorAdminMenu.BTN_TRADE_DEC,
                ServerVendorAdminMenu.BTN_TRADE_INC,
                ServerVendorAdminMenu.BTN_CLEAR_TRADE,
                -1);

        // ROW 2: Buy price
        renderRow(g, mx, my, x, y + 52,
                Component.translatable("gui.create_marketplace.server_vendor.admin.buy_price").getString(),
                menu.getCurrentBuyPrice(), true,
                ServerVendorAdminMenu.BTN_BUY_DEC,
                ServerVendorAdminMenu.BTN_BUY_INC,
                ServerVendorAdminMenu.BTN_CLEAR_BUY,
                -1);

        // ROW 3: Sell price (z dodatkowym [Auto 60%])
        renderRow(g, mx, my, x, y + 80,
                Component.translatable("gui.create_marketplace.server_vendor.admin.sell_price").getString(),
                menu.getCurrentSellPrice(), true,
                ServerVendorAdminMenu.BTN_SELL_DEC,
                ServerVendorAdminMenu.BTN_SELL_INC,
                ServerVendorAdminMenu.BTN_CLEAR_SELL,
                ServerVendorAdminMenu.BTN_SUGGEST_60);

        // Checkboxy + Save/Cancel (y=108)
        int rowY = y + 108;
        renderCheckbox(g, x + 8, rowY, menu.buyEnabled, "gui.create_marketplace.server_vendor.admin.allow_buy",
                mx, my, ServerVendorAdminMenu.BTN_TOGGLE_BUY);
        renderCheckbox(g, x + 92, rowY, menu.sellEnabled, "gui.create_marketplace.server_vendor.admin.allow_sell",
                mx, my, ServerVendorAdminMenu.BTN_TOGGLE_SELL);

        // Save/Cancel pod ekwipunkiem (y=210) — daleko od checkboxów
        int btnW = 50, btnH = 14;
        int saveX = x + PANEL_W - btnW * 2 - 14;
        int cancelX = x + PANEL_W - btnW - 8;
        int btnY = y + 211;
        boolean hSave = mx >= saveX && mx < saveX + btnW && my >= btnY && my < btnY + btnH;
        boolean hCancel = mx >= cancelX && mx < cancelX + btnW && my >= btnY && my < btnY + btnH;
        bevel(g, saveX, btnY, saveX + btnW, btnY + btnH, hSave ? PATINA_LIGHT : PATINA, PATINA_DARK, FRAME);
        bevel(g, cancelX, btnY, cancelX + btnW, btnY + btnH, hCancel ? BRASS_LIGHT : BRASS, BRASS_DARK, FRAME);
        String sv = Component.translatable("gui.create_marketplace.server_vendor.admin.save").getString();
        String cn = Component.translatable("gui.create_marketplace.server_vendor.admin.cancel").getString();
        g.drawString(this.font, sv, saveX + (btnW - this.font.width(sv)) / 2, btnY + 3, WHITE, false);
        g.drawString(this.font, cn, cancelX + (btnW - this.font.width(cn)) / 2, btnY + 3, FRAME, false);
        hits.add(new Hit(saveX, btnY, saveX + btnW, btnY + btnH, this::save));
        hits.add(new Hit(cancelX, btnY, cancelX + btnW, btnY + btnH, this::onClose));
    }

    /**
     * Wiersz konfiguracji: slot (rysowany w renderBg) + etykieta + helper (= X spurs) + przyciski [-][+][Clear] (+ opcjonalnie [60%]).
     * Helper jest przycinany żeby zawsze zostawić miejsce na przyciski.
     */
    private void renderRow(GuiGraphics g, int mx, int my, int x, int slotY, String label, ItemStack stack,
                           boolean showPriceValue, int btnDec, int btnInc, int btnClear, int btnSuggest) {
        // Stałe szerokości przycisków
        final int wMinus = 14, wPlus = 14, wClear = 16, wSuggest = 28, gap = 3;
        int btnTotalW = wPlus + gap + wMinus + gap + wClear;
        if (btnSuggest >= 0) btnTotalW += gap + wSuggest;

        // Etykieta nad linią pomocniczą
        g.drawString(this.font, label, x + 46, slotY - 2, INK, false);

        // Helper text przycięty żeby nie nachodzić na przyciski
        int textW = PANEL_W - 8 - 46 - btnTotalW - 6;
        String helper;
        if (stack.isEmpty()) {
            helper = "—";
        } else if (showPriceValue && Coinage.isNumismaticsCoin(stack)) {
            int spurs = stack.getCount() * Coinage.coinValueOf(stack);
            helper = "= " + Coinage.formatPlain(spurs);
        } else {
            helper = ""; // slot pokazuje już count i przedmiot
        }
        if (!helper.isEmpty()) {
            g.drawString(this.font, trim(this.font, helper, textW), x + 46, slotY + 8, INK_SOFT, false);
        }

        // Przyciski na prawym końcu wiersza, kolejność right-to-left
        int rowY = slotY;
        int rightX = x + PANEL_W - 8;
        rightX -= wClear;
        chip(g, rightX, rowY, wClear, 12, "x", mx, my, () -> sendBtn(btnClear));
        if (btnSuggest >= 0) {
            rightX -= gap + wSuggest;
            chip(g, rightX, rowY, wSuggest, 12, "60%", mx, my, () -> sendBtn(btnSuggest));
        }
        rightX -= gap + wPlus;
        chip(g, rightX, rowY, wPlus, 12, "+", mx, my, () -> sendBtn(btnInc));
        rightX -= gap + wMinus;
        chip(g, rightX, rowY, wMinus, 12, "-", mx, my, () -> sendBtn(btnDec));
    }

    private void chip(GuiGraphics g, int x, int y, int w, int h, String label, int mx, int my, Runnable act) {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + h;
        bevel(g, x, y, x + w, y + h, hover ? BRASS_LIGHT : BRASS, BRASS_DARK, FRAME);
        int tw = this.font.width(label);
        g.drawString(this.font, label, x + (w - tw) / 2, y + (h - 8) / 2, FRAME, false);
        hits.add(new Hit(x, y, x + w, y + h, act));
    }

    private void renderCheckbox(GuiGraphics g, int x, int y, boolean checked, String labelKey,
                                 int mx, int my, int btnId) {
        boolean hover = mx >= x && mx < x + 12 && my >= y && my < y + 12;
        bevel(g, x, y, x + 12, y + 12, hover ? PARCH_HOVER : PARCH_2, BRASS_DARK, FRAME);
        if (checked) g.drawString(this.font, "✓", x + 3, y + 2, PATINA_DARK, false);
        g.drawString(this.font, Component.translatable(labelKey), x + 16, y + 2, INK, false);
        hits.add(new Hit(x, y, x + 12, y + 12, () -> sendBtn(btnId)));
    }

    private void sendBtn(int id) {
        if (this.minecraft == null || this.minecraft.gameMode == null || this.minecraft.player == null) return;
        menu.clickMenuButton(this.minecraft.player, id);
        this.minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private void save() {
        if (this.minecraft == null || this.minecraft.player == null) return;
        PacketDistributor.sendToServer(new SaveServerVendorPayload(
                menu.pos,
                menu.getCurrentTradeItem(),
                menu.getCurrentBuyPrice(),
                menu.getCurrentSellPrice(),
                menu.buyEnabled,
                menu.sellEnabled));
        this.onClose();
    }

    /** Scroll wheel na slotem przedmiotu/kupna/sprzedaży: regulacja count. */
    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int sx = leftPos + 24, sw = 18;
        int[] slotYs = {topPos + 24, topPos + 52, topPos + 80};
        int[] incIds = {ServerVendorAdminMenu.BTN_TRADE_INC, ServerVendorAdminMenu.BTN_BUY_INC, ServerVendorAdminMenu.BTN_SELL_INC};
        int[] decIds = {ServerVendorAdminMenu.BTN_TRADE_DEC, ServerVendorAdminMenu.BTN_BUY_DEC, ServerVendorAdminMenu.BTN_SELL_DEC};
        for (int i = 0; i < 3; i++) {
            if (mx >= sx && mx < sx + sw && my >= slotYs[i] && my < slotYs[i] + sw) {
                if (dy > 0) sendBtn(incIds[i]); else if (dy < 0) sendBtn(decIds[i]);
                return true;
            }
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (h.in(mx, my)) { h.action().run(); return true; }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
