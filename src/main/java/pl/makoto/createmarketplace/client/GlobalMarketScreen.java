package pl.makoto.createmarketplace.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
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

    // Cached render data — recomputed only on data/search change
    private List<MarketOffer> cachedFiltered = List.of();
    private Map<String, Map<String, List<MarketOffer>>> cachedGrouped = Map.of();

    private final Map<String, Boolean> expandedPlayers = new HashMap<>();
    private final Map<String, Boolean> expandedShops = new HashMap<>();
    private boolean showFavoritesOnly = false;

    // Tracks scrollable widgets and their base Y positions (Y at scrollOffset=0)
    private final List<WidgetPosition> scrollableWidgets = new ArrayList<>();

    private record WidgetPosition(AbstractWidget widget, int baseY) {}

    public GlobalMarketScreen(List<MarketOffer> offers) {
        super(Component.translatable("gui.create_marketplace.global_market.title"));
        this.offers = offers;
    }

    public void updateOffers(List<MarketOffer> offers) {
        this.offers = offers;
        recomputeCache();
        rebuildWidgets();
    }

    private void recomputeCache() {
        String query = (searchBox != null) ? searchBox.getValue().toLowerCase() : "";
        cachedFiltered = offers.stream()
                .filter(o -> {
                    if (showFavoritesOnly && !FavoritesManager.isFavorite(o.pos()))
                        return false;
                    if (query.isEmpty())
                        return true;
                    boolean matchShop = o.shopName().toLowerCase().contains(query);
                    boolean matchOwner = o.ownerName().toLowerCase().contains(query);
                    boolean matchItem = o.item() != null && !o.item().isEmpty()
                            && o.item().getHoverName().getString().toLowerCase().contains(query);
                    return matchShop || matchOwner || matchItem;
                })
                .toList();
        cachedGrouped = cachedFiltered.stream()
                .sorted((o1, o2) -> Boolean.compare(FavoritesManager.isFavorite(o2.pos()), FavoritesManager.isFavorite(o1.pos())))
                .collect(Collectors.groupingBy(MarketOffer::ownerName, java.util.LinkedHashMap::new,
                        Collectors.groupingBy(MarketOffer::shopName, java.util.LinkedHashMap::new, Collectors.toList())));
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int centerX = this.width / 2;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.create_marketplace.global_market.my_shops"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new MyShopsScreen(this, this.offers));
            }
        }).bounds(10, 10, 80, 20).build());

        this.searchBox = new EditBox(this.font, centerX - 100, 10, 200, 20, Component.translatable("gui.create_marketplace.global_market.search"));
        this.searchBox.setResponder(s -> {
            this.scrollOffset = 0;
            recomputeCache();
            this.rebuildWidgets();
        });
        this.addRenderableWidget(this.searchBox);

        recomputeCache();
        this.rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        this.clearWidgets();
        this.scrollableWidgets.clear();
        this.searchBox.setX(this.width / 2 - 100);
        this.addRenderableWidget(this.searchBox);
        this.addRenderableWidget(Button.builder(Component.translatable("gui.create_marketplace.global_market.my_shops"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new MyShopsScreen(this, this.offers));
            }
        }).bounds(10, 10, 80, 20).build());

        int screenCenterX = this.width / 2;
        this.addRenderableWidget(Button.builder(Component.translatable(showFavoritesOnly ? "gui.create_marketplace.global_market.favorites_only" : "gui.create_marketplace.global_market.all_shops"), button -> {
            this.showFavoritesOnly = !this.showFavoritesOnly;
            this.scrollOffset = 0;
            recomputeCache();
            this.rebuildWidgets();
        }).bounds(screenCenterX + 110, 10, 80, 20).build());

        String query = this.searchBox.getValue().toLowerCase();

        List<MarketOffer> filtered = offers.stream()
                .filter(o -> {
                    if (showFavoritesOnly && !FavoritesManager.isFavorite(o.pos()))
                        return false;
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
                .sorted((o1, o2) -> Boolean.compare(FavoritesManager.isFavorite(o2.pos()), FavoritesManager.isFavorite(o1.pos())))
                .collect(Collectors.groupingBy(MarketOffer::ownerName, java.util.LinkedHashMap::new, 
                        Collectors.groupingBy(MarketOffer::shopName, java.util.LinkedHashMap::new, Collectors.toList())));

        int centerX = this.width / 2;
        int baseY = 50;
        int rowHeight = 35;

        for (Map.Entry<String, Map<String, List<MarketOffer>>> playerEntry : grouped.entrySet()) {
            String playerName = playerEntry.getKey();
            expandedPlayers.putIfAbsent(playerName, true);
            boolean pExpanded = expandedPlayers.get(playerName);

            int py = baseY;
            Button playerToggle = Button.builder(Component.literal(pExpanded ? "-" : "+"), button -> {
                expandedPlayers.put(playerName, !pExpanded);
                this.rebuildWidgets();
            }).bounds(centerX - 180, py - scrollOffset, 15, 15).build();
            this.addRenderableWidget(playerToggle);
            scrollableWidgets.add(new WidgetPosition(playerToggle, py));

            baseY += 20;

            if (pExpanded) {
                for (Map.Entry<String, List<MarketOffer>> shopEntry : playerEntry.getValue().entrySet()) {
                    String shopName = shopEntry.getKey();
                    String shopKey = playerName + ":" + shopName;
                    expandedShops.putIfAbsent(shopKey, true);
                    boolean sExpanded = expandedShops.get(shopKey);

                    int sy = baseY;
                    Button shopToggle = Button.builder(Component.literal(sExpanded ? "-" : "+"), button -> {
                        expandedShops.put(shopKey, !sExpanded);
                        this.rebuildWidgets();
                    }).bounds(centerX - 160, sy - scrollOffset, 15, 15).build();
                    this.addRenderableWidget(shopToggle);
                    scrollableWidgets.add(new WidgetPosition(shopToggle, sy));

                    baseY += 20;

                    if (sExpanded) {
                        for (MarketOffer offer : shopEntry.getValue()) {
                            int oy = baseY;

                            boolean isFav = FavoritesManager.isFavorite(offer.pos());
                            Button favButton = Button.builder(Component.literal(isFav ? "★" : "☆"), button -> {
                                FavoritesManager.toggleFavorite(offer.pos());
                                this.rebuildWidgets();
                            }).bounds(centerX + 105, oy + 7 - scrollOffset, 25, 20).build();
                            this.addRenderableWidget(favButton);
                            scrollableWidgets.add(new WidgetPosition(favButton, oy + 7));

                            Button navButton = Button.builder(Component.translatable("gui.create_marketplace.global_market.navigate"), button -> {
                                XaeroCompat.addWaypoint(offer.shopName(), offer.pos().getX(), offer.pos().getY(),
                                        offer.pos().getZ());
                                if (this.minecraft != null && this.minecraft.player != null) {
                                    this.minecraft.player.sendSystemMessage(Component.translatable("gui.create_marketplace.global_market.navigate_success", offer.shopName())
                                            .withStyle(net.minecraft.ChatFormatting.GREEN));
                                    this.onClose();
                                }
                            }).bounds(centerX + 135, oy + 7 - scrollOffset, 60, 20).build();
                            this.addRenderableWidget(navButton);
                            scrollableWidgets.add(new WidgetPosition(navButton, oy + 7));

                            baseY += rowHeight;
                        }
                    }
                }
            }
        }
        maxScroll = Math.max(0, baseY - this.height + 20);
        repositionWidgets();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (offers.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.create_marketplace.global_market.no_offers"), this.width / 2,
                    this.height / 2, 0xAAAAAA);
            return;
        }

        String query = this.searchBox.getValue().toLowerCase();
        List<MarketOffer> filtered = offers.stream()
                .filter(o -> {
                    if (showFavoritesOnly && !FavoritesManager.isFavorite(o.pos()))
                        return false;
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
                .sorted((o1, o2) -> Boolean.compare(FavoritesManager.isFavorite(o2.pos()), FavoritesManager.isFavorite(o1.pos())))
                .collect(Collectors.groupingBy(MarketOffer::ownerName, java.util.LinkedHashMap::new, 
                        Collectors.groupingBy(MarketOffer::shopName, java.util.LinkedHashMap::new, Collectors.toList())));

        int centerX = this.width / 2;
        int y = 50 - scrollOffset;
        int rowHeight = 35;

        for (Map.Entry<String, Map<String, List<MarketOffer>>> playerEntry : grouped.entrySet()) {
            String playerName = playerEntry.getKey();
            boolean pExpanded = expandedPlayers.getOrDefault(playerName, true);

            if (y > 20 && y < this.height) {
                guiGraphics.drawString(this.font, Component.translatable("gui.create_marketplace.global_market.player", playerName), centerX - 155, y + 4, 0xFFD700);
            }
            y += 20;

            if (pExpanded) {
                for (Map.Entry<String, List<MarketOffer>> shopEntry : playerEntry.getValue().entrySet()) {
                    String shopName = shopEntry.getKey();
                    String shopKey = playerName + ":" + shopName;
                    boolean sExpanded = expandedShops.getOrDefault(shopKey, true);

                    if (y > 20 && y < this.height) {
                        guiGraphics.drawString(this.font, Component.translatable("gui.create_marketplace.global_market.shop", shopName), centerX - 135, y + 4, 0x55FF55);
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
        scrollOffset -= (int)(scrollY * 20);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        // Only reposition widgets, do NOT call rebuildWidgets()
        repositionWidgets();
        return true;
    }

    private void repositionWidgets() {
        for (WidgetPosition wp : scrollableWidgets) {
            int newY = wp.baseY() - scrollOffset;
            wp.widget().setY(newY);
            wp.widget().visible = (newY > 35 && newY < this.height - 20);
        }
    }

    @Override
    public void onClose() {
        FavoritesManager.flush();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
