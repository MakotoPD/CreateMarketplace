package pl.makoto.createmarketplace.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import pl.makoto.createmarketplace.data.MarketOffer;
import pl.makoto.createmarketplace.network.PublishShopPayload;

import java.util.ArrayList;
import java.util.List;

import static pl.makoto.createmarketplace.client.MarketTheme.*;

/**
 * Rejestracja sklepu — własny ekran w stylu rynku (mosiądz + patyna).
 */
public class ShopRegistrationScreen extends Screen {
    private final BlockPos pos;
    private final ItemStack item;
    private final ItemStack currency;
    private final List<String> existingShops;

    private EditBox nameBox;
    private final List<Hit> hits = new ArrayList<>();

    private record Hit(int x1, int y1, int x2, int y2, Runnable action) {
        boolean in(double mx, double my) { return mx >= x1 && mx < x2 && my >= y1 && my < y2; }
    }

    public ShopRegistrationScreen(BlockPos pos, ItemStack item, ItemStack currency, List<String> existingShops) {
        super(Component.translatable("gui.create_marketplace.registration.title"));
        this.pos = pos;
        this.item = item;
        this.currency = currency;
        this.existingShops = existingShops;
    }

    private record Layout(int px, int py, int pw, int ph, int sellY, int priceY,
                          int nameLabelY, int boxX, int boxY, int boxW, int boxH,
                          int existLabelY, int chipsY, int btnY, int chipCount) {}

    private Layout layout() {
        int pw = 240;
        int px = (this.width - pw) / 2;
        int chipCount = Math.min(existingShops.size(), 4);
        int chipRows = (chipCount + 1) / 2;

        int contentH = 18      // nagłówek
                + 4 + 22       // sprzedajesz
                + 18           // cena
                + 6 + 10 + 20  // nazwa (label + box)
                + (chipCount > 0 ? 10 + chipRows * 20 + 4 : 0)
                + 26;          // przyciski
        int ph = contentH + 6;
        int py = (this.height - ph) / 2;

        int y = py + 18 + 4;
        int sellY = y; y += 22;
        int priceY = y; y += 18 + 6;
        int nameLabelY = y; y += 10;
        int boxX = px + 12, boxY = y, boxW = pw - 24, boxH = 18; y += 20;
        int existLabelY = 0, chipsY = 0;
        if (chipCount > 0) { existLabelY = y; y += 10; chipsY = y; y += chipRows * 20 + 4; }
        int btnY = y;
        return new Layout(px, py, pw, ph, sellY, priceY, nameLabelY, boxX, boxY, boxW, boxH,
                existLabelY, chipsY, btnY, chipCount);
    }

    @Override
    protected void init() {
        super.init();
        Layout l = layout();
        this.nameBox = new EditBox(this.font, l.boxX + 4, l.boxY + 4, l.boxW - 8, 12,
                Component.translatable("gui.create_marketplace.registration.name"));
        this.nameBox.setMaxLength(32);
        this.nameBox.setBordered(false);
        this.nameBox.setTextColor(0xD8C79A);
        this.addRenderableWidget(this.nameBox);
        this.setInitialFocus(this.nameBox);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        this.renderBackground(g, mx, my, partialTick);
        hits.clear();
        Layout l = layout();

        // panel + nagłówek
        bevel(g, l.px, l.py, l.px + l.pw, l.py + l.ph, PARCH, PARCH, FRAME);
        g.fill(l.px + 1, l.py + 1, l.px + l.pw - 1, l.py + 16, PATINA);
        g.drawString(this.font, Component.translatable("gui.create_marketplace.registration.title"),
                l.px + 8, l.py + 5, WHITE, false);

        // sprzedajesz
        g.drawString(this.font, Component.translatable("gui.create_marketplace.registration.selling"),
                l.px + 12, l.sellY, INK_SOFT, false);
        int slotX = l.px + 80, slotY = l.sellY - 5;
        slot(g, slotX, slotY);
        if (!item.isEmpty()) {
            g.renderItem(item, slotX + 1, slotY + 1);
            g.renderItemDecorations(this.font, item, slotX + 1, slotY + 1);
        }
        String sellTxt = (item.isEmpty() ? Component.translatable("gui.create_marketplace.my_shops.no_item").getString()
                : item.getHoverName().getString() + " ×" + item.getCount());
        g.drawString(this.font, trim(sellTxt, l.px + l.pw - (slotX + 22) - 6), slotX + 22, l.sellY, INK, false);

        // cena
        g.drawString(this.font, Component.translatable("gui.create_marketplace.registration.price"),
                l.px + 12, l.priceY, INK_SOFT, false);
        if (currency.isEmpty()) {
            g.drawString(this.font, Component.translatable("gui.create_marketplace.my_shops.free"),
                    l.px + 80, l.priceY, PATINA_DARK, false);
        } else {
            g.renderItem(currency, l.px + 80, l.priceY - 5);
            g.renderItemDecorations(this.font, currency, l.px + 80, l.priceY - 5);
            g.drawString(this.font, "×" + currency.getCount() + " " + currency.getHoverName().getString(),
                    l.px + 100, l.priceY, INK, false);
        }

        // nazwa
        g.drawString(this.font, Component.translatable("gui.create_marketplace.registration.name"),
                l.px + 12, l.nameLabelY, INK_SOFT, false);
        g.fill(l.boxX, l.boxY, l.boxX + l.boxW, l.boxY + l.boxH, FRAME);
        g.fill(l.boxX + 1, l.boxY + 1, l.boxX + l.boxW - 1, l.boxY + l.boxH - 1, 0xFF211B12);

        // istniejące sklepy
        if (l.chipCount > 0) {
            g.drawString(this.font, Component.translatable("gui.create_marketplace.registration.add_to_existing"),
                    l.px + 12, l.existLabelY, INK_SOFT, false);
            int chipW = (l.pw - 30) / 2, chipH = 18, gap = 6;
            for (int i = 0; i < l.chipCount; i++) {
                int col = i % 2, row = i / 2;
                int cx = l.px + 12 + col * (chipW + gap);
                int cy = l.chipsY + row * 20;
                String name = existingShops.get(i);
                boolean hover = mx >= cx && mx < cx + chipW && my >= cy && my < cy + chipH;
                bevel(g, cx, cy, cx + chipW, cy + chipH, hover ? BRASS_LIGHT : PARCH_2, BRASS_DARK, FRAME);
                g.drawString(this.font, trim(name, chipW - 8), cx + 4, cy + 5, INK, false);
                hits.add(new Hit(cx, cy, cx + chipW, cy + chipH, () -> this.nameBox.setValue(name)));
            }
        }

        // przyciski: Opublikuj / Anuluj
        int bw = (l.pw - 30) / 2, bh = 20, by = l.btnY;
        int pubX = l.px + 12, cancelX = l.px + l.pw - 12 - bw;
        boolean hPub = mx >= pubX && mx < pubX + bw && my >= by && my < by + bh;
        boolean hCancel = mx >= cancelX && mx < cancelX + bw && my >= by && my < by + bh;
        bevel(g, pubX, by, pubX + bw, by + bh, hPub ? PATINA_LIGHT : PATINA, PATINA_DARK, FRAME);
        bevel(g, cancelX, by, cancelX + bw, by + bh, hCancel ? BRASS_LIGHT : BRASS, BRASS_DARK, FRAME);
        String pub = Component.translatable("gui.create_marketplace.registration.publish").getString();
        String cancel = Component.translatable("gui.create_marketplace.confirm.no").getString();
        g.drawString(this.font, pub, pubX + (bw - this.font.width(pub)) / 2, by + 6, WHITE, false);
        g.drawString(this.font, cancel, cancelX + (bw - this.font.width(cancel)) / 2, by + 6, 0xFF2C1F10, false);
        hits.add(new Hit(pubX, by, pubX + bw, by + bh, this::publish));
        hits.add(new Hit(cancelX, by, cancelX + bw, by + bh, this::onClose));

        // pole nazwy na wierzchu (bez super.render — uniknięcie podwójnego blura)
        this.nameBox.render(g, mx, my, partialTick);

        // tooltip przedmiotu
        if (mx >= slotX && mx < slotX + 18 && my >= slotY && my < slotY + 18 && !item.isEmpty()) {
            g.renderTooltip(this.font, item, mx, my);
        }
    }

    private void publish() {
        String shopName = nameBox.getValue();
        if (shopName.trim().isEmpty() || this.minecraft == null || this.minecraft.player == null) return;
        // właściciela i tak ustala serwer (zwykły gracz albo "Serwer" w admin mode)
        MarketOffer offer = new MarketOffer(
                this.minecraft.player.getUUID(),
                this.minecraft.player.getName().getString(),
                shopName, this.pos, this.minecraft.player.level().dimension().location(),
                this.item, this.currency, System.currentTimeMillis());
        PacketDistributor.sendToServer(new PublishShopPayload(offer));
        this.onClose();
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
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            publish();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private String trim(String s, int maxWidth) {
        return MarketTheme.trim(this.font, s, maxWidth);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
