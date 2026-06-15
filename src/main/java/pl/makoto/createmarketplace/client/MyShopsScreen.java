package pl.makoto.createmarketplace.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import pl.makoto.createmarketplace.AdminMode;
import pl.makoto.createmarketplace.data.MarketOffer;
import pl.makoto.createmarketplace.network.DeleteShopPayload;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static pl.makoto.createmarketplace.client.MarketTheme.*;

/**
 * "Moje sklepy" — własny ekran w stylu globalnego rynku (mosiądz + patyna).
 * Renderowany ręcznie z hit-testingiem; akcje: usuń ofertę / usuń cały sklep
 * (z potwierdzeniem).
 */
public class MyShopsScreen extends Screen {

    private final Screen parent;
    private List<MarketOffer> offers;

    private int scrollOffset = 0;
    private int maxScroll = 0;

    private Map<String, List<MarketOffer>> myShops = Map.of();

    private final List<Hit> hits = new ArrayList<>();

    private final ConfirmDialog confirm = new ConfirmDialog();

    private record Hit(int x1, int y1, int x2, int y2, Runnable action) {
        boolean in(double mx, double my) {
            return mx >= x1 && mx < x2 && my >= y1 && my < y2;
        }
    }

    public MyShopsScreen(Screen parent, List<MarketOffer> offers) {
        super(Component.translatable("gui.create_marketplace.my_shops.title"));
        this.parent = parent;
        this.offers = offers;
    }

    public void updateOffers(List<MarketOffer> offers) {
        this.offers = offers;
        recompute();
    }

    private void recompute() {
        if (minecraft == null || minecraft.player == null) { myShops = Map.of(); return; }
        // w admin mode pokazujemy sklepy "Serwera", inaczej własne gracza
        UUID filter = ClientAdminState.isActive() ? AdminMode.SERVER_UUID : minecraft.player.getUUID();
        Map<String, List<MarketOffer>> m = new LinkedHashMap<>();
        for (MarketOffer o : offers) {
            if (o.ownerId().equals(filter)) m.computeIfAbsent(o.shopName(), k -> new ArrayList<>()).add(o);
        }
        myShops = m;
    }

    @Override
    protected void init() {
        super.init();
        recompute();
    }

    // --- układ ---

    private record Layout(int left, int right, int top, int bottom,
                          int headerY, int headerH, int bodyTop, int bodyBottom, int footY) {}

    private Layout layout() {
        int left = 6, right = this.width - 6, top = 6, bottom = this.height - 6;
        int headerH = 22, headerY = top, footH = 14;
        int bodyTop = headerY + headerH + 4;
        int bodyBottom = bottom - footH - 4;
        return new Layout(left, right, top, bottom, headerY, headerH, bodyTop, bodyBottom, bodyBottom + 6);
    }

    // --- render ---

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        hits.clear();
        Layout l = layout();

        renderHeader(g, l, mouseX, mouseY);
        renderBody(g, l, mouseX, mouseY);
        renderFooter(g, l);
        confirm.render(g, this.font, this.width, this.height, mouseX, mouseY);
        // NIE wołamy super.render() — uniknięcie podwójnego renderBackground (blur)
    }

    private void renderHeader(GuiGraphics g, Layout l, int mx, int my) {
        bevel(g, l.left, l.headerY, l.right, l.headerY + l.headerH, BRASS_LIGHT, BRASS_DARK, FRAME);
        String title = Component.translatable("gui.create_marketplace.my_shops.title").getString();
        g.drawString(this.font, title, l.left + 8, l.headerY + 7, 0xFF2C1F10, false);
        drawAdminBadge(g, l.left + 8 + this.font.width(title) + 6, l.headerY);

        // segment "Wróć" po prawej
        String back = Component.translatable("gui.create_marketplace.global_market.back").getString();
        int w = this.font.width(back) + 16;
        int x1 = l.right - 4 - w, y1 = l.headerY + 2, x2 = l.right - 4, y2 = l.headerY + l.headerH - 2;
        boolean hover = mx >= x1 && mx < x2 && my >= y1 && my < y2;
        bevel(g, x1, y1, x2, y2, hover ? PATINA_LIGHT : PATINA, PATINA_DARK, FRAME);
        g.drawString(this.font, back, x1 + 8, y1 + 5, WHITE, false);
        hits.add(new Hit(x1, y1, x2, y2, this::goBack));
    }

    private void renderBody(GuiGraphics g, Layout l, int mx, int my) {
        bevel(g, l.left, l.bodyTop, l.right, l.bodyBottom, PARCH, PARCH, FRAME);
        int x1 = l.left + 2, x2 = l.right - 2;

        if (myShops.isEmpty()) {
            g.drawCenteredString(this.font, Component.translatable("gui.create_marketplace.my_shops.no_shops"),
                    (l.left + l.right) / 2, (l.bodyTop + l.bodyBottom) / 2 - 4, INK_MUTE);
            maxScroll = 0;
            return;
        }

        int areaTop = l.bodyTop + 1, areaBottom = l.bodyBottom - 1;
        int sectionH = 16, rowH = 20, gap = 6;

        // łączna wysokość (do scrolla)
        int total = 0;
        for (List<MarketOffer> off : myShops.values()) total += sectionH + off.size() * rowH + gap;
        maxScroll = Math.max(0, total - (areaBottom - areaTop));
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

        g.enableScissor(x1, areaTop, x2, areaBottom);
        int y = areaTop - scrollOffset;
        for (Map.Entry<String, List<MarketOffer>> e : myShops.entrySet()) {
            String shop = e.getKey();
            List<MarketOffer> off = e.getValue();

            // nagłówek sklepu
            if (y + sectionH >= areaTop && y <= areaBottom) {
                g.fill(x1, y, x2, y + sectionH, PATINA);
                g.drawString(this.font, Component.translatable("gui.create_marketplace.global_market.shop", shop), x1 + 6, y + 4, WHITE, false);
                String blocks = Component.translatable("gui.create_marketplace.my_shops.blocks", off.size()).getString();
                // przycisk "Usuń sklep" po prawej
                String del = Component.translatable("gui.create_marketplace.my_shops.delete").getString();
                int bw = this.font.width(del) + 12;
                int bx1 = x2 - bw - 3, by1 = y + 2, bx2 = x2 - 3, by2 = y + sectionH - 2;
                boolean bh = mx >= bx1 && mx < bx2 && my >= by1 && my < by2;
                bevel(g, bx1, by1, bx2, by2, bh ? RED_LIGHT : RED, RED_DARK, 0xFF5A1A12);
                g.drawString(this.font, del, bx1 + 6, by1 + 3, WHITE, false);
                hits.add(new Hit(bx1, by1, bx2, by2, () -> confirm.open(
                        Component.translatable("gui.create_marketplace.confirm.delete_shop", shop),
                        () -> PacketDistributor.sendToServer(new DeleteShopPayload(Optional.empty(), Optional.of(shop))))));
                int blocksW = this.font.width(blocks);
                g.drawString(this.font, blocks, bx1 - blocksW - 8, y + 4, 0xFFE7F4F0, false);
            }
            y += sectionH;

            // oferty
            for (int i = 0; i < off.size(); i++) {
                MarketOffer o = off.get(i);
                if (y + rowH >= areaTop && y <= areaBottom) {
                    renderOfferRow(g, o, x1, x2, y, rowH, i, mx, my);
                }
                y += rowH;
            }
            y += gap;
        }
        g.disableScissor();

        // pasek przewijania
        if (maxScroll > 0) {
            int trackX = l.right - 4, h = areaBottom - areaTop;
            int thumbH = Math.max(16, (int) ((float) h * h / total));
            int thumbY = areaTop + (int) ((float) (h - thumbH) * scrollOffset / maxScroll);
            g.fill(trackX, areaTop, trackX + 3, areaBottom, 0x40000000);
            g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, BRASS_DARK);
        }
    }

    private void renderOfferRow(GuiGraphics g, MarketOffer o, int x1, int x2, int y, int rowH, int idx, int mx, int my) {
        boolean hover = mx >= x1 && mx < x2 && my >= y && my < y + rowH;
        g.fill(x1, y, x2, y + rowH, hover ? PARCH_HOVER : ((idx & 1) == 0 ? PARCH_ROW : PARCH_2));
        g.fill(x1, y + rowH - 1, x2, y + rowH, LINE);

        int W = x2 - x1;
        int priceX = x1 + (int) (W * 0.46);
        int posX = x1 + (int) (W * 0.72);
        int delX2 = x2 - 4, delX1 = delX2 - 16;
        int midY = y + (rowH - 8) / 2;

        // slot + przedmiot
        int slotX = x1 + 4, slotY = y + (rowH - 18) / 2;
        g.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF6E5A2C);
        g.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF2B2336);
        if (o.item() != null && !o.item().isEmpty()) {
            g.renderItem(o.item(), slotX + 1, slotY + 1);
            g.renderItemDecorations(this.font, o.item(), slotX + 1, slotY + 1);
        }
        String iname = (o.item() == null || o.item().isEmpty())
                ? Component.translatable("gui.create_marketplace.my_shops.no_item").getString()
                : o.item().getHoverName().getString();
        String qty = (o.item() == null || o.item().isEmpty()) ? "" : " ×" + o.item().getCount();
        g.drawString(this.font, trim(iname + qty, priceX - (slotX + 22)), slotX + 22, midY, INK, false);

        // cena
        if (o.currency() == null || o.currency().isEmpty()) {
            g.drawString(this.font, Component.translatable("gui.create_marketplace.my_shops.free"), priceX, midY, PATINA_DARK, false);
        } else {
            g.renderItem(o.currency(), priceX, y + (rowH - 16) / 2);
            g.drawString(this.font, trim("×" + o.currency().getCount(), posX - (priceX + 20)), priceX + 18, midY, INK, false);
        }

        // pozycja
        g.drawString(this.font, trim(o.pos().toShortString(), delX1 - posX - 6), posX, midY, INK_SOFT, false);

        // usuń ofertę (X)
        int by1 = y + (rowH - 16) / 2;
        boolean bh = mx >= delX1 && mx < delX2 && my >= by1 && my < by1 + 16;
        bevel(g, delX1, by1, delX2, by1 + 16, bh ? RED_LIGHT : RED, RED_DARK, 0xFF5A1A12);
        g.drawString(this.font, "X", delX1 + 5, by1 + 4, WHITE, false);
        hits.add(new Hit(delX1, by1, delX2, by1 + 16, () -> confirm.open(
                Component.translatable("gui.create_marketplace.confirm.delete_offer"),
                () -> PacketDistributor.sendToServer(new DeleteShopPayload(Optional.of(o.pos()), Optional.empty())))));

        if (mx >= slotX && mx < slotX + 18 && my >= slotY && my < slotY + 18
                && o.item() != null && !o.item().isEmpty()) {
            g.renderTooltip(this.font, o.item(), mx, my);
        }
    }

    private void renderFooter(GuiGraphics g, Layout l) {
        bevel(g, l.left, l.footY, l.right, l.footY + 14, PARCH_2, PARCH_2, FRAME);
        int total = myShops.values().stream().mapToInt(List::size).sum();
        g.drawString(this.font, Component.literal(myShops.size() + " · " + total),
                l.left + 6, l.footY + 3, 0xFF5A4828, false);
    }

    // --- interakcje ---

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // modal potwierdzenia przechwytuje wszystkie kliknięcia
        if (confirm.active()) {
            if (button == 0) confirm.mouseClicked(mx, my);
            return true;
        }
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
    public boolean keyPressed(int key, int scan, int mods) {
        if (confirm.keyPressed(key)) return true; // modal pochłania klawisze
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset = Mth.clamp(scrollOffset - (int) (scrollY * 20), 0, maxScroll);
        return true;
    }

    private void goBack() {
        if (this.minecraft == null) return;
        if (parent instanceof GlobalMarketScreen gm) {
            gm.updateOffers(this.offers);
            this.minecraft.setScreen(parent);
        } else {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // --- pomocnicze ---

    private String trim(String s, int maxWidth) {
        return MarketTheme.trim(this.font, s, maxWidth);
    }

    /** plakietka "ADMIN" gdy włączony tryb administratora */
    private void drawAdminBadge(GuiGraphics g, int x, int headerY) {
        if (!ClientAdminState.isActive()) return;
        String tag = Component.translatable("gui.create_marketplace.admin_badge").getString();
        int w = this.font.width(tag) + 8;
        bevel(g, x, headerY + 5, x + w, headerY + 16, RED_LIGHT, RED_DARK, RED_BORDER);
        g.drawString(this.font, tag, x + 4, headerY + 7, WHITE, false);
    }
}
