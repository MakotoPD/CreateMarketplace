package pl.makoto.createmarketplace.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import pl.makoto.createmarketplace.data.MarketOffer;
import pl.makoto.createmarketplace.network.DeleteShopPayload;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class MyShopsScreen extends Screen {
    private final Screen parent;
    private List<MarketOffer> offers;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    // Cached render data — recomputed only on data change
    private Map<String, List<MarketOffer>> cachedMyShops = Map.of();

    public MyShopsScreen(Screen parent, List<MarketOffer> offers) {
        super(Component.translatable("gui.create_marketplace.my_shops.title"));
        this.parent = parent;
        this.offers = offers;
    }

    public void updateOffers(List<MarketOffer> offers) {
        this.offers = offers;
        recomputeCache();
        this.init();
    }

    private void recomputeCache() {
        if (minecraft == null || minecraft.player == null) return;
        UUID myUuid = minecraft.player.getUUID();
        cachedMyShops = offers.stream()
                .filter(o -> o.ownerId().equals(myUuid))
                .collect(Collectors.groupingBy(MarketOffer::shopName));
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        // Recompute cache if not already done (e.g. when called by Minecraft framework)
        recomputeCache();

        int centerX = this.width / 2;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.create_marketplace.global_market.back"), button -> {
            if (this.minecraft != null) {
                if (parent instanceof GlobalMarketScreen gm) {
                    gm.updateOffers(this.offers); // żeby przekazać ewentualne usunięcia
                    this.minecraft.setScreen(parent);
                } else {
                    this.minecraft.setScreen(null);
                }
            }
        }).bounds(10, 10, 60, 20).build());

        if (this.minecraft == null || this.minecraft.player == null) return;

        int y = 50 - scrollOffset;
        int calcY = 50; // do obliczenia maxScroll
        int rowHeight = 40;

        for (Map.Entry<String, List<MarketOffer>> entry : cachedMyShops.entrySet()) {
            String shopName = entry.getKey();
            List<MarketOffer> shopOffers = entry.getValue();

            // Przycisk usunięcia CAŁEGO sklepu (always created, visibility managed by repositionWidgets)
            Button deleteShopBtn = Button.builder(Component.translatable("gui.create_marketplace.my_shops.delete"), button -> {
                PacketDistributor.sendToServer(new DeleteShopPayload(Optional.empty(), Optional.of(shopName)));
            }).bounds(centerX + 100, y + 5, 80, 20).build();
            deleteShopBtn.visible = (y > 30 && y < this.height - 20);
            this.addRenderableWidget(deleteShopBtn);

            y += 30;
            calcY += 30;

            for (MarketOffer offer : shopOffers) {
                int offerY = y;
                // Przycisk usunięcia pojedynczej oferty (always created, visibility managed by repositionWidgets)
                Button deleteOfferBtn = Button.builder(Component.literal("X").withStyle(net.minecraft.ChatFormatting.RED), button -> {
                    PacketDistributor.sendToServer(new DeleteShopPayload(Optional.of(offer.pos()), Optional.empty()));
                }).bounds(centerX + 160, offerY + 10, 20, 20).build();
                deleteOfferBtn.visible = (offerY > 30 && offerY < this.height - 20);
                this.addRenderableWidget(deleteOfferBtn);

                y += rowHeight;
                calcY += rowHeight;
            }
            y += 10;
            calcY += 10;
        }
        
        maxScroll = Math.max(0, calcY - this.height + 20);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFD700);

        if (this.minecraft == null || this.minecraft.player == null) return;

        if (cachedMyShops.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.create_marketplace.my_shops.no_shops"), this.width / 2, this.height / 2, 0xAAAAAA);
            return;
        }

        int centerX = this.width / 2;
        int y = 50 - scrollOffset;
        int rowHeight = 40;

        for (Map.Entry<String, List<MarketOffer>> entry : cachedMyShops.entrySet()) {
            String shopName = entry.getKey();
            List<MarketOffer> shopOffers = entry.getValue();

            if (y > 20 && y < this.height) {
                guiGraphics.drawString(this.font, Component.translatable("gui.create_marketplace.global_market.shop", shopName), centerX - 180, y + 10, 0x55FF55);
                guiGraphics.drawString(this.font, Component.translatable("gui.create_marketplace.my_shops.blocks", shopOffers.size()), centerX - 20, y + 10, 0xAAAAAA);
            }

            y += 30;

            for (MarketOffer offer : shopOffers) {
                if (y > 20 && y < this.height - 20) {
                    guiGraphics.fill(centerX - 180, y, centerX + 150, y + rowHeight - 5, 0x55000000);

                    // Produkt
                    guiGraphics.renderItem(offer.item(), centerX - 170, y + 10);
                    guiGraphics.renderItemDecorations(this.font, offer.item(), centerX - 170, y + 10);

                    String itemName = offer.item().isEmpty() ? Component.translatable("gui.create_marketplace.my_shops.no_item").getString() : offer.item().getHoverName().getString();
                    String currencyName = offer.currency().isEmpty() ? Component.translatable("gui.create_marketplace.my_shops.free").getString() : offer.currency().getHoverName().getString();
                    String currencyAmount = offer.currency().isEmpty() ? "" : offer.currency().getCount() + "x ";

                    guiGraphics.drawString(this.font, itemName, centerX - 145, y + 5, 0xFFFFFF);
                    guiGraphics.drawString(this.font, Component.translatable("gui.create_marketplace.my_shops.price", currencyAmount + currencyName), centerX - 145, y + 15, 0xAAAAAA);
                    guiGraphics.drawString(this.font, Component.translatable("gui.create_marketplace.my_shops.pos", offer.pos().toShortString()), centerX - 145, y + 25, 0x777777);

                    if (mouseX >= centerX - 170 && mouseX <= centerX - 170 + 16 && mouseY >= y + 10 && mouseY <= y + 10 + 16) {
                        guiGraphics.renderTooltip(this.font, offer.item(), mouseX, mouseY);
                    }
                    // Usunięto ikonę waluty i jej tooltip
                }
                y += rowHeight;
            }
            y += 10;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset -= (int)(scrollY * 20);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        // Reposition without calling init()
        repositionWidgets();
        return true;
    }

    /**
     * Repositions existing widgets based on the current scrollOffset without recreating them.
     * Iterates through the cached shop data to compute expected Y positions and updates
     * widget positions accordingly. Widgets outside the visible area are moved off-screen.
     */
    private void repositionWidgets() {
        int centerX = this.width / 2;
        int y = 50 - scrollOffset;
        int rowHeight = 40;

        // The first widget (index 0) is the back button — skip it
        int widgetIndex = 1;

        for (Map.Entry<String, List<MarketOffer>> entry : cachedMyShops.entrySet()) {
            List<MarketOffer> shopOffers = entry.getValue();

            // Delete shop button
            if (widgetIndex < this.renderables.size() && this.renderables.get(widgetIndex) instanceof Button btn) {
                if (y > 30 && y < this.height - 20) {
                    btn.setY(y + 5);
                    btn.visible = true;
                } else {
                    btn.visible = false;
                }
            }
            widgetIndex++;

            y += 30;

            for (int i = 0; i < shopOffers.size(); i++) {
                int offerY = y;
                // Delete offer "X" button
                if (widgetIndex < this.renderables.size() && this.renderables.get(widgetIndex) instanceof Button btn) {
                    if (offerY > 30 && offerY < this.height - 20) {
                        btn.setY(offerY + 10);
                        btn.visible = true;
                    } else {
                        btn.visible = false;
                    }
                }
                widgetIndex++;
                y += rowHeight;
            }
            y += 10;
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
