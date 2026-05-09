package pl.makoto.createmarketplace.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import pl.makoto.createmarketplace.data.MarketOffer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GlobalMarketScreen extends Screen {
    private List<MarketOffer> offers;
    private EditBox searchBox;

    private int scrollOffset = 0;
    private int maxScroll = 0;

    private final Map<String, Boolean> expandedPlayers = new HashMap<>();
    private final Map<String, Boolean> expandedShops = new HashMap<>();

    public GlobalMarketScreen(List<MarketOffer> offers) {
        super(Component.literal("Globalny Rynek"));
        this.offers = offers;
    }

    public void updateOffers(List<MarketOffer> offers) {
        this.offers = offers;
        this.init(); // Wymusza przebudowę listy
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int centerX = this.width / 2;

        this.addRenderableWidget(Button.builder(Component.literal("Moje Sklepy"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new MyShopsScreen(this, this.offers));
            }
        }).bounds(10, 10, 80, 20).build());

        this.searchBox = new EditBox(this.font, centerX - 100, 10, 200, 20, Component.literal("Szukaj..."));
        this.searchBox.setResponder(s -> {
            this.scrollOffset = 0;
            this.rebuildWidgets();
        });
        this.addRenderableWidget(this.searchBox);

        this.rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        this.clearWidgets();
        this.addRenderableWidget(this.searchBox);
        this.addRenderableWidget(Button.builder(Component.literal("Moje Sklepy"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new MyShopsScreen(this, this.offers));
            }
        }).bounds(10, 10, 80, 20).build());

        String query = this.searchBox.getValue().toLowerCase();

        List<MarketOffer> filtered = offers.stream()
                .filter(o -> {
                    if (query.isEmpty())
                        return true;
                    boolean matchShop = o.shopName().toLowerCase().contains(query);
                    boolean matchOwner = o.ownerName().toLowerCase().contains(query);
                    boolean matchItem = o.item() != null && !o.item().isEmpty()
                            && o.item().getHoverName().getString().toLowerCase().contains(query);
                    return matchShop || matchOwner || matchItem;
                })
                .toList();

        Map<String, Map<String, List<MarketOffer>>> grouped = filtered.stream()
                .collect(Collectors.groupingBy(MarketOffer::ownerName, Collectors.groupingBy(MarketOffer::shopName)));

        int centerX = this.width / 2;
        int y = 50 - scrollOffset;
        int rowHeight = 35;

        for (Map.Entry<String, Map<String, List<MarketOffer>>> playerEntry : grouped.entrySet()) {
            String playerName = playerEntry.getKey();
            expandedPlayers.putIfAbsent(playerName, true);
            boolean pExpanded = expandedPlayers.get(playerName);

            int py = y;
            if (py > 35 && py < this.height - 20) {
                this.addRenderableWidget(Button.builder(Component.literal(pExpanded ? "-" : "+"), button -> {
                    expandedPlayers.put(playerName, !pExpanded);
                    this.rebuildWidgets();
                }).bounds(centerX - 180, py, 15, 15).build());
            }

            y += 20;

            if (pExpanded) {
                for (Map.Entry<String, List<MarketOffer>> shopEntry : playerEntry.getValue().entrySet()) {
                    String shopName = shopEntry.getKey();
                    String shopKey = playerName + ":" + shopName;
                    expandedShops.putIfAbsent(shopKey, true);
                    boolean sExpanded = expandedShops.get(shopKey);

                    int sy = y;
                    if (sy > 35 && sy < this.height - 20) {
                        this.addRenderableWidget(Button.builder(Component.literal(sExpanded ? "-" : "+"), button -> {
                            expandedShops.put(shopKey, !sExpanded);
                            this.rebuildWidgets();
                        }).bounds(centerX - 160, sy, 15, 15).build());
                    }

                    y += 20;

                    if (sExpanded) {
                        for (MarketOffer offer : shopEntry.getValue()) {
                            int oy = y;
                            if (oy > 35 && oy < this.height - 20) {
                                this.addRenderableWidget(Button.builder(Component.literal("Nawiguj"), button -> {
                                    XaeroCompat.addWaypoint(offer.shopName(), offer.pos().getX(), offer.pos().getY(),
                                            offer.pos().getZ());
                                    if (this.minecraft != null && this.minecraft.player != null) {
                                        this.minecraft.player.sendSystemMessage(Component
                                                .literal("Przesłano punkt '" + offer.shopName() + "' do Xaero's Minimap!")
                                                .withStyle(net.minecraft.ChatFormatting.GREEN));
                                        this.onClose();
                                    }
                                }).bounds(centerX + 110, oy + 7, 60, 20).build());
                            }
                            y += rowHeight;
                        }
                    }
                }
            }
        }
        maxScroll = Math.max(0, y + scrollOffset - this.height + 20);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (offers.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, "Brak dostępnych ofert na rynku.", this.width / 2,
                    this.height / 2, 0xAAAAAA);
            return;
        }

        String query = this.searchBox.getValue().toLowerCase();
        List<MarketOffer> filtered = offers.stream()
                .filter(o -> {
                    if (query.isEmpty())
                        return true;
                    boolean matchShop = o.shopName().toLowerCase().contains(query);
                    boolean matchOwner = o.ownerName().toLowerCase().contains(query);
                    boolean matchItem = o.item() != null && !o.item().isEmpty()
                            && o.item().getHoverName().getString().toLowerCase().contains(query);
                    return matchShop || matchOwner || matchItem;
                })
                .toList();

        Map<String, Map<String, List<MarketOffer>>> grouped = filtered.stream()
                .collect(Collectors.groupingBy(MarketOffer::ownerName, Collectors.groupingBy(MarketOffer::shopName)));

        int centerX = this.width / 2;
        int y = 50 - scrollOffset;
        int rowHeight = 35;

        for (Map.Entry<String, Map<String, List<MarketOffer>>> playerEntry : grouped.entrySet()) {
            String playerName = playerEntry.getKey();
            boolean pExpanded = expandedPlayers.getOrDefault(playerName, true);

            if (y > 20 && y < this.height) {
                guiGraphics.drawString(this.font, "Gracz: " + playerName, centerX - 155, y + 4, 0xFFD700);
            }
            y += 20;

            if (pExpanded) {
                for (Map.Entry<String, List<MarketOffer>> shopEntry : playerEntry.getValue().entrySet()) {
                    String shopName = shopEntry.getKey();
                    String shopKey = playerName + ":" + shopName;
                    boolean sExpanded = expandedShops.getOrDefault(shopKey, true);

                    if (y > 20 && y < this.height) {
                        guiGraphics.drawString(this.font, "Sklep: " + shopName, centerX - 135, y + 4, 0x55FF55);
                    }
                    y += 20;

                    if (sExpanded) {
                        for (MarketOffer offer : shopEntry.getValue()) {
                            if (y > 20 && y < this.height - 20) {
                                guiGraphics.fill(centerX - 120, y, centerX + 100, y + rowHeight - 5, 0x55000000);

                                // Produkt
                                guiGraphics.renderItem(offer.item(), centerX - 110, y + 5);
                                guiGraphics.renderItemDecorations(this.font, offer.item(), centerX - 110, y + 5);

                                String itemName = offer.item().isEmpty() ? "Nieznany"
                                        : offer.item().getHoverName().getString();
                                String currencyName = offer.currency().isEmpty() ? "Darmowe"
                                        : offer.currency().getHoverName().getString();
                                String currencyAmount = offer.currency().isEmpty() ? ""
                                        : offer.currency().getCount() + "x ";

                                guiGraphics.drawString(this.font, itemName, centerX - 85, y + 5, 0xFFFFFF);
                                guiGraphics.drawString(this.font, currencyAmount + currencyName, centerX - 85, y + 15,
                                        0xAAAAAA);

                                if (mouseX >= centerX - 110 && mouseX <= centerX - 110 + 16 && mouseY >= y + 5
                                        && mouseY <= y + 5 + 16) {
                                    guiGraphics.renderTooltip(this.font, offer.item(), mouseX, mouseY);
                                }
                                // Usunięto ikonę waluty i jej tooltip
                            }
                            y += rowHeight;
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset -= scrollY * 20;
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        this.rebuildWidgets();
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
