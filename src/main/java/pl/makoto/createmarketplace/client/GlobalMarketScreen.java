package pl.makoto.createmarketplace.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import pl.makoto.createmarketplace.AdminMode;
import pl.makoto.createmarketplace.data.MarketOffer;
import pl.makoto.createmarketplace.network.DeleteShopPayload;

import java.util.Optional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static pl.makoto.createmarketplace.client.MarketTheme.*;

/**
 * Globalny rynek — własny ekran w stylu "Create / mosiądz + patyna".
 * Renderowany ręcznie (poza polem wyszukiwania): nagłówek z segmentami,
 * panel filtrów, lista ofert z sortowaniem i akcją "Nawiguj".
 */
public class GlobalMarketScreen extends Screen {

    // --- sortowanie ---
    private static final int SORT_ITEM = 0, SORT_PRICE = 1, SORT_OWNER = 2, SORT_DIM = 3;

    private List<MarketOffer> offers;
    private EditBox searchBox;

    private int sortColumn = SORT_ITEM;
    private boolean sortAsc = true;
    private boolean favoritesOnly = false;
    private String selectedOwner = null;
    private String selectedCurrency = null;
    private String selectedDimension = null;

    private int scrollOffset = 0;
    private int maxScroll = 0;

    private List<MarketOffer> view = List.of();
    private Map<String, Integer> ownerCounts = Map.of();
    private Map<String, Integer> currencyCounts = Map.of();
    private Map<String, Integer> dimCounts = Map.of();
    private Map<String, UUID> ownerIds = Map.of();          // nick → UUID (do główki)
    private Map<String, ItemStack> currencySamples = Map.of(); // waluta → przykładowy item (do ikony)

    private interface IconRenderer {
        void draw(GuiGraphics g, int x, int y);
    }

    // strefy klikalne budowane podczas renderu i sprawdzane w mouseClicked
    private final List<Hit> hits = new ArrayList<>();
    private final ConfirmDialog confirm = new ConfirmDialog();

    private record Hit(int x1, int y1, int x2, int y2, Runnable action) {
        boolean in(double mx, double my) {
            return mx >= x1 && mx < x2 && my >= y1 && my < y2;
        }
    }

    public GlobalMarketScreen(List<MarketOffer> offers) {
        super(Component.translatable("gui.create_marketplace.global_market.title"));
        this.offers = offers;
    }

    public void updateOffers(List<MarketOffer> offers) {
        this.offers = offers;
        recompute();
    }

    // --- filtrowanie / sortowanie ---

    private static final String FREE_KEY = Component.translatable("gui.create_marketplace.my_shops.free").getString();

    private static String currencyKey(MarketOffer o) {
        return o.currency() == null || o.currency().isEmpty()
                ? FREE_KEY : o.currency().getHoverName().getString();
    }

    private void recompute() {
        String query = (searchBox != null) ? searchBox.getValue().toLowerCase() : "";

        List<MarketOffer> base = offers.stream().filter(o -> {
            if (favoritesOnly && !FavoritesManager.isFavorite(o.pos())) return false;
            if (query.isEmpty()) return true;
            boolean mShop = o.shopName().toLowerCase().contains(query);
            boolean mOwner = o.ownerName().toLowerCase().contains(query);
            boolean mItem = o.item() != null && !o.item().isEmpty()
                    && o.item().getHoverName().getString().toLowerCase().contains(query);
            return mShop || mOwner || mItem;
        }).toList();

        // liczniki facetów z listy zawężonej wyszukiwarką (nie facetami)
        Map<String, Integer> oc = new LinkedHashMap<>();
        Map<String, Integer> cc = new LinkedHashMap<>();
        Map<String, Integer> dc = new LinkedHashMap<>();
        Map<String, UUID> oid = new LinkedHashMap<>();
        Map<String, ItemStack> cs = new LinkedHashMap<>();
        for (MarketOffer o : base) {
            oc.merge(o.ownerName(), 1, Integer::sum);
            cc.merge(currencyKey(o), 1, Integer::sum);
            dc.merge(o.dimension().toString(), 1, Integer::sum);
            oid.putIfAbsent(o.ownerName(), o.ownerId());
            cs.putIfAbsent(currencyKey(o), o.currency());
        }
        ownerCounts = oc;
        currencyCounts = cc;
        dimCounts = dc;
        ownerIds = oid;
        currencySamples = cs;

        List<MarketOffer> filtered = new ArrayList<>(base.stream().filter(o -> {
            if (selectedOwner != null && !selectedOwner.equals(o.ownerName())) return false;
            if (selectedCurrency != null && !selectedCurrency.equals(currencyKey(o))) return false;
            if (selectedDimension != null && !selectedDimension.equals(o.dimension().toString())) return false;
            return true;
        }).toList());

        Comparator<MarketOffer> cmp = switch (sortColumn) {
            case SORT_PRICE -> Comparator.comparingInt((MarketOffer o) -> o.currency() == null || o.currency().isEmpty() ? 0 : o.currency().getCount());
            case SORT_OWNER -> Comparator.comparing((MarketOffer o) -> o.ownerName().toLowerCase());
            case SORT_DIM -> Comparator.comparing((MarketOffer o) -> o.dimension().toString());
            default -> Comparator.comparing((MarketOffer o) -> o.item() == null || o.item().isEmpty()
                    ? "" : o.item().getHoverName().getString().toLowerCase());
        };
        // ulubione zawsze na górze, potem wg kolumny
        filtered.sort(Comparator.comparing((MarketOffer o) -> !FavoritesManager.isFavorite(o.pos()))
                .thenComparing(sortAsc ? cmp : cmp.reversed()));
        view = filtered;
    }

    @Override
    protected void init() {
        super.init();
        Layout l = layout();
        this.searchBox = new EditBox(this.font, l.searchX + 4, l.searchY + 4, l.searchW - 8, 12,
                Component.translatable("gui.create_marketplace.global_market.search"));
        this.searchBox.setBordered(false);
        this.searchBox.setTextColor(0xD8C79A);
        this.searchBox.setResponder(s -> { scrollOffset = 0; recompute(); });
        this.addRenderableWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);
        recompute();
    }

    // --- układ (liczony z rozmiaru ekranu) ---

    private record Layout(
            int left, int right, int top, int bottom,
            int headerY, int headerH,
            int searchX, int searchY, int searchW, int searchH,
            int sideX, int sideW, int bodyTop, int bodyBottom,
            int listX, int listHeadY, int listHeadH, int rowsTop, int rowH,
            int footY) {}

    private Layout layout() {
        int left = 6, right = this.width - 6, top = 6, bottom = this.height - 6;
        int headerH = 22;
        int headerY = top;
        int searchH = 18;
        int searchY = headerY + headerH + 2;
        int searchW = (right - left) - 2 * 22 - 4; // miejsce na 2 ikony
        int searchX = left;
        int footH = 14;
        int bodyTop = searchY + searchH + 4;
        int bodyBottom = bottom - footH - 4;
        int sideX = left, sideW = 104;
        int listX = sideX + sideW + 4;
        int listHeadH = 14;
        int footY = bodyBottom + 6;
        return new Layout(left, right, top, bottom, headerY, headerH,
                searchX, searchY, searchW, searchH, sideX, sideW, bodyTop, bodyBottom,
                listX, bodyTop, listHeadH, bodyTop + listHeadH, 20, footY);
    }

    // --- render ---

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        hits.clear();
        Layout l = layout();

        // reposition search (na wypadek zmiany rozmiaru)
        searchBox.setX(l.searchX + 4);
        searchBox.setY(l.searchY + 4);
        searchBox.setWidth(l.searchW - 8);

        renderHeader(g, l, mouseX, mouseY);
        renderSearchBar(g, l, mouseX, mouseY);
        renderSidebar(g, l, mouseX, mouseY);
        renderList(g, l, mouseX, mouseY);
        renderFooter(g, l);

        // UWAGA: NIE wołamy super.render() — Screen.render() ponownie wywołałby
        // renderBackground() (processBlurEffect), rozmazując już narysowane GUI.
        // Pole wyszukiwania (jedyny widget) renderujemy ręcznie na wierzchu.
        this.searchBox.render(g, mouseX, mouseY, partialTick);

        // modal potwierdzenia na samej górze
        confirm.render(g, this.font, this.width, this.height, mouseX, mouseY);
    }

    private void renderHeader(GuiGraphics g, Layout l, int mx, int my) {
        bevel(g, l.left, l.headerY, l.right, l.headerY + l.headerH, BRASS_LIGHT, BRASS_DARK, FRAME);
        String title = Component.translatable("gui.create_marketplace.global_market.title").getString();
        g.drawString(this.font, title, l.left + 8, l.headerY + 7, 0xFF2C1F10, false);
        if (ClientAdminState.isActive()) {
            String adminTag = Component.translatable("gui.create_marketplace.admin_badge").getString();
            int bx = l.left + 8 + this.font.width(title) + 6;
            int bw = this.font.width(adminTag) + 8;
            bevel(g, bx, l.headerY + 5, bx + bw, l.headerY + 16, RED_LIGHT, RED_DARK, RED_BORDER);
            g.drawString(this.font, adminTag, bx + 4, l.headerY + 7, WHITE, false);
        }

        // segmenty po prawej, budowane od prawej: Ulubione, Moje sklepy, Rynek
        int x = l.right - 4;
        x = segment(g, l, x, Component.translatable("gui.create_marketplace.tab.favorites").getString(), favoritesOnly, mx, my, () -> {
            favoritesOnly = true; scrollOffset = 0; recompute();
        });
        x = segment(g, l, x, Component.translatable("gui.create_marketplace.global_market.my_shops").getString(), false, mx, my, () -> {
            if (this.minecraft != null) this.minecraft.setScreen(new MyShopsScreen(this, this.offers));
        });
        segment(g, l, x, Component.translatable("gui.create_marketplace.tab.market").getString(), !favoritesOnly, mx, my, () -> {
            favoritesOnly = false; scrollOffset = 0; recompute();
        });
    }

    /** rysuje segment kończący się na prawej krawędzi `xRight`, zwraca lewą krawędź (z marginesem) */
    private int segment(GuiGraphics g, Layout l, int xRight, String text, boolean active, int mx, int my, Runnable action) {
        int w = this.font.width(text) + 14;
        int x1 = xRight - w, y1 = l.headerY + 2, x2 = xRight, y2 = l.headerY + l.headerH - 2;
        boolean hover = mx >= x1 && mx < x2 && my >= y1 && my < y2;
        if (active) bevel(g, x1, y1, x2, y2, PATINA_LIGHT, PATINA_DARK, FRAME);
        else bevel(g, x1, y1, x2, y2, hover ? BRASS_LIGHT : 0xFFE7CD8F, BRASS_DARK, FRAME);
        g.drawString(this.font, text, x1 + 7, y1 + 5, active ? WHITE : 0xFF3A2C14, false);
        hits.add(new Hit(x1, y1, x2, y2, action));
        return x1 - 4;
    }

    private void renderSearchBar(GuiGraphics g, Layout l, int mx, int my) {
        // tło inset pola
        g.fill(l.searchX, l.searchY, l.searchX + l.searchW, l.searchY + l.searchH, FRAME);
        g.fill(l.searchX + 1, l.searchY + 1, l.searchX + l.searchW - 1, l.searchY + l.searchH - 1, 0xFF211B12);
        if (searchBox.getValue().isEmpty() && !searchBox.isFocused()) {
            g.drawString(this.font, Component.translatable("gui.create_marketplace.global_market.search"),
                    l.searchX + 5, l.searchY + 5, 0xFF7D6F4E, false);
        }
        // ikony odświeżania i zamknięcia
        int by = l.searchY;
        int rx = l.searchX + l.searchW + 2;
        iconButton(g, rx, by, l.searchH, "↻", mx, my, () -> { recompute(); });
        int rx2 = rx + l.searchH + 2;
        iconButton(g, rx2, by, l.searchH, "✕", mx, my, this::onClose);
    }

    private void iconButton(GuiGraphics g, int x, int y, int size, String glyph, int mx, int my, Runnable action) {
        boolean hover = mx >= x && mx < x + size && my >= y && my < y + size;
        bevel(g, x, y, x + size, y + size, hover ? BRASS_LIGHT : BRASS, BRASS_DARK, FRAME);
        int tw = this.font.width(glyph);
        g.drawString(this.font, glyph, x + (size - tw) / 2, y + (size - 8) / 2, 0xFF2C1F10, false);
        hits.add(new Hit(x, y, x + size, y + size, action));
    }

    private void renderSidebar(GuiGraphics g, Layout l, int mx, int my) {
        bevel(g, l.sideX, l.bodyTop, l.sideX + l.sideW, l.bodyBottom, PARCH_2, PARCH_2, FRAME);
        int x1 = l.sideX + 1, x2 = l.sideX + l.sideW - 1;
        // tytuł
        g.fill(x1, l.bodyTop + 1, x2, l.bodyTop + 15, PATINA);
        g.drawString(this.font, Component.translatable("gui.create_marketplace.filters.title"), x1 + 6, l.bodyTop + 4, WHITE, false);

        g.enableScissor(x1, l.bodyTop + 16, x2, l.bodyBottom - 1);
        int y = l.bodyTop + 18;

        y = facetHeader(g, x1, x2, y, Component.translatable("gui.create_marketplace.filters.owner").getString());
        for (Map.Entry<String, Integer> e : ownerCounts.entrySet()) {
            String owner = e.getKey();
            y = facetRow(g, x1, x2, y, ownerLabel(owner), e.getValue(), owner.equals(selectedOwner),
                    (gg, ix, iy) -> drawOwnerHead(gg, owner, ownerIds.get(owner), ix, iy, 8), mx, my, () -> {
                        selectedOwner = owner.equals(selectedOwner) ? null : owner;
                        scrollOffset = 0; recompute();
                    });
        }

        y += 4;
        y = facetHeader(g, x1, x2, y, Component.translatable("gui.create_marketplace.filters.currency").getString());
        for (Map.Entry<String, Integer> e : currencyCounts.entrySet()) {
            String cur = e.getKey();
            y = facetRow(g, x1, x2, y, cur, e.getValue(), cur.equals(selectedCurrency),
                    (gg, ix, iy) -> drawCurrencyIcon(gg, currencySamples.get(cur), ix, iy), mx, my, () -> {
                        selectedCurrency = cur.equals(selectedCurrency) ? null : cur;
                        scrollOffset = 0; recompute();
                    });
        }

        y += 4;
        y = facetHeader(g, x1, x2, y, Component.translatable("gui.create_marketplace.filters.dimension").getString());
        for (Map.Entry<String, Integer> e : dimCounts.entrySet()) {
            String d = e.getKey();
            y = facetRow(g, x1, x2, y, dimLabel(d), e.getValue(), d.equals(selectedDimension),
                    (gg, ix, iy) -> gg.fill(ix, iy + 1, ix + 7, iy + 7, dimColor(d)), mx, my, () -> {
                        selectedDimension = d.equals(selectedDimension) ? null : d;
                        scrollOffset = 0; recompute();
                    });
        }
        g.disableScissor();
    }

    /** Główka gracza (serwer → kolor patyny). Rozmiar w px. */
    private void drawOwnerHead(GuiGraphics g, String name, UUID id, int x, int y, int size) {
        if (id != null && id.equals(AdminMode.SERVER_UUID)) {
            g.fill(x, y, x + size, y + size, PATINA);
            return;
        }
        PlayerSkin skin = resolveSkin(name, id);
        if (skin != null) PlayerFaceRenderer.draw(g, skin, x, y, size);
        else g.fill(x, y, x + size, y + size, avatarColor(name));
    }

    /**
     * Skin gracza po NAZWIE (z listy graczy) — działa na offline-mode i z SkinsRestorer,
     * bo bierze skin zastosowany u klienta. Fallback: insecure skin po profilu.
     */
    private PlayerSkin resolveSkin(String name, UUID id) {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener conn = mc.getConnection();
        if (conn != null && name != null) {
            PlayerInfo info = conn.getPlayerInfo(name);
            if (info != null) return info.getSkin();
        }
        if (id != null) {
            return mc.getSkinManager().getInsecureSkin(new GameProfile(id, name));
        }
        return null;
    }

    /** Ikona przedmiotu-waluty (przeskalowana, by zmieściła się w wierszu). */
    private void drawCurrencyIcon(GuiGraphics g, ItemStack stack, int x, int y) {
        if (stack == null || stack.isEmpty()) {
            g.fill(x + 1, y + 1, x + 7, y + 7, BRASS_DARK); // "za darmo"/barter bez itemu
            return;
        }
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(0.55f, 0.55f, 1f);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
    }

    /** czytelna nazwa wymiaru (waniliowe przez tłumaczenie, modowe = ścieżka id) */
    private static String dimLabel(String dim) {
        return switch (dim) {
            case "minecraft:overworld" -> Component.translatable("gui.create_marketplace.dim.overworld").getString();
            case "minecraft:the_nether" -> Component.translatable("gui.create_marketplace.dim.nether").getString();
            case "minecraft:the_end" -> Component.translatable("gui.create_marketplace.dim.end").getString();
            default -> {
                int i = dim.indexOf(':');
                yield i >= 0 ? dim.substring(i + 1) : dim;
            }
        };
    }

    private static int dimColor(String dim) {
        return switch (dim) {
            case "minecraft:the_nether" -> 0xFFA4452F;
            case "minecraft:the_end" -> 0xFF6E4FA0;
            default -> PATINA_DARK;
        };
    }

    private int facetHeader(GuiGraphics g, int x1, int x2, int y, String text) {
        g.fill(x1, y, x2, y + 12, 0xFFD8BD82);
        g.drawString(this.font, text, x1 + 5, y + 2, 0xFF3A2C14, false);
        return y + 13;
    }

    private int facetRow(GuiGraphics g, int x1, int x2, int y, String label, int count, boolean on, IconRenderer icon, int mx, int my, Runnable action) {
        int h = 12;
        boolean hover = mx >= x1 && mx < x2 && my >= y && my < y + h;
        if (on) {
            g.fill(x1, y, x2, y + h, 0xFFD3C08C);
            g.fill(x1, y, x1 + 2, y + h, PATINA);
        } else if (hover) {
            g.fill(x1, y, x2, y + h, PARCH_HOVER);
        }
        icon.draw(g, x1 + 4, y + 2); // główka / ikona przedmiotu / kolor
        String cnt = String.valueOf(count);
        int cntW = this.font.width(cnt);
        String name = trim(label, (x2 - cntW - 6) - (x1 + 14));
        g.drawString(this.font, name, x1 + 14, y + 2, INK, false);
        g.drawString(this.font, cnt, x2 - cntW - 3, y + 2, INK_MUTE, false);
        hits.add(new Hit(x1, y, x2, y + h, action));
        return y + h;
    }

    private void renderList(GuiGraphics g, Layout l, int mx, int my) {
        int lx1 = l.listX, lx2 = l.right;
        bevel(g, lx1, l.bodyTop, lx2, l.bodyBottom, PARCH, PARCH, FRAME);

        // kolumny
        int innerX1 = lx1 + 1, innerX2 = lx2 - 1;
        int colFav = 14, colAction = ClientAdminState.isActive() ? 92 : 70;
        int mid = (innerX2 - innerX1) - colFav - colAction;
        int cItem = innerX1 + colFav;
        int cPrice = cItem + (int) (mid * 0.42);
        int cOwner = cPrice + (int) (mid * 0.20);
        int cShop = cOwner + (int) (mid * 0.20);
        int cAction = innerX2 - colAction;

        // nagłówek kolumn (patyna)
        g.fill(innerX1, l.bodyTop + 1, innerX2, l.bodyTop + 1 + l.listHeadH, PATINA_DARK);
        colHeader(g, cItem + 4, l.bodyTop + 4, Component.translatable("gui.create_marketplace.col.item").getString(), SORT_ITEM, cItem, cPrice, l, mx, my);
        colHeader(g, cPrice + 4, l.bodyTop + 4, Component.translatable("gui.create_marketplace.col.price").getString(), SORT_PRICE, cPrice, cOwner, l, mx, my);
        colHeader(g, cOwner + 4, l.bodyTop + 4, Component.translatable("gui.create_marketplace.col.owner").getString(), SORT_OWNER, cOwner, cShop, l, mx, my);
        colHeader(g, cShop + 4, l.bodyTop + 4, Component.translatable("gui.create_marketplace.col.dimension").getString(), SORT_DIM, cShop, cAction, l, mx, my);
        g.drawString(this.font, Component.translatable("gui.create_marketplace.col.action"), cAction + 4, l.bodyTop + 4, WHITE, false);

        int rowsTop = l.bodyTop + 1 + l.listHeadH;
        int rowsBottom = l.bodyBottom - 1;
        int areaH = rowsBottom - rowsTop;
        int rowH = l.rowH;

        if (view.isEmpty()) {
            g.drawCenteredString(this.font, Component.translatable("gui.create_marketplace.global_market.no_offers"),
                    (lx1 + lx2) / 2, rowsTop + areaH / 2 - 4, INK_MUTE);
            maxScroll = 0;
            return;
        }

        int totalH = view.size() * rowH;
        maxScroll = Math.max(0, totalH - areaH);
        scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll);

        g.enableScissor(innerX1, rowsTop, innerX2, rowsBottom);
        int y = rowsTop - scrollOffset;
        for (MarketOffer o : view) {
            if (y + rowH >= rowsTop && y <= rowsBottom) {
                renderRow(g, o, innerX1, innerX2, y, rowH, cItem, cPrice, cOwner, cShop, cAction, mx, my);
            }
            y += rowH;
        }
        g.disableScissor();

        // pasek przewijania
        if (maxScroll > 0) {
            int trackX = lx2 - 4;
            int thumbH = Math.max(16, (int) ((float) areaH * areaH / totalH));
            int thumbY = rowsTop + (int) ((float) (areaH - thumbH) * scrollOffset / maxScroll);
            g.fill(trackX, rowsTop, trackX + 3, rowsBottom, 0x40000000);
            g.fill(trackX, thumbY, trackX + 3, thumbY + thumbH, BRASS_DARK);
        }
    }

    private void colHeader(GuiGraphics g, int tx, int ty, String text, int col, int x1, int x2, Layout l, int mx, int my) {
        boolean active = sortColumn == col;
        String label = text + (active ? (sortAsc ? " ▲" : " ▼") : "");
        g.drawString(this.font, label, tx, ty, WHITE, false);
        hits.add(new Hit(x1, l.bodyTop + 1, x2, l.bodyTop + 1 + l.listHeadH, () -> {
            if (sortColumn == col) sortAsc = !sortAsc;
            else { sortColumn = col; sortAsc = true; }
            recompute();
        }));
    }

    private void renderRow(GuiGraphics g, MarketOffer o, int x1, int x2, int y, int rowH,
                           int cItem, int cPrice, int cOwner, int cShop, int cAction, int mx, int my) {
        boolean hover = mx >= x1 && mx < x2 && my >= y && my < y + rowH;
        boolean fav = FavoritesManager.isFavorite(o.pos());
        int bg = hover ? PARCH_HOVER : (((y / rowH) & 1) == 0 ? PARCH_ROW : PARCH_2);
        g.fill(x1, y, x2, y + rowH, bg);
        if (hover) g.fill(x1, y, x1 + 3, y + rowH, PATINA);
        g.fill(x1, y + rowH - 1, x2, y + rowH, LINE);

        int midY = y + (rowH - 8) / 2;

        // ★ ulubione
        String star = fav ? "★" : "☆";
        g.drawString(this.font, star, x1 + 3, midY, fav ? 0xFFE8A91F : 0xFFC8B079, false);
        hits.add(new Hit(x1, y, cItem, y + rowH, () -> { FavoritesManager.toggleFavorite(o.pos()); recompute(); }));

        // przedmiot: slot + ikona + nazwa ×ilość
        int slotX = cItem + 2, slotY = y + (rowH - 18) / 2;
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
        String itemText = trim(iname + qty, cPrice - (slotX + 22));
        g.drawString(this.font, itemText, slotX + 22, midY, INK, false);

        // cena
        if (o.currency() == null || o.currency().isEmpty()) {
            g.drawString(this.font, Component.translatable("gui.create_marketplace.my_shops.free"), cPrice + 2, midY, PATINA_DARK, false);
        } else {
            g.renderItem(o.currency(), cPrice + 2, y + (rowH - 16) / 2);
            String priceText = trim("×" + o.currency().getCount(), cOwner - (cPrice + 20));
            g.drawString(this.font, priceText, cPrice + 20, midY, INK, false);
        }

        // właściciel (główka gracza + nick; serwer → kolor)
        drawOwnerHead(g, o.ownerName(), o.ownerId(), cOwner + 2, midY - 1, 9);
        g.drawString(this.font, trim(ownerLabel(o.ownerName()), cShop - (cOwner + 14)), cOwner + 14, midY, LINK, false);

        // sklep
        String dim = o.dimension().toString();
        g.drawString(this.font, trim(dimLabel(dim), cAction - (cShop + 4)), cShop + 4, midY, dimColor(dim), false);

        // akcja: Nawiguj (+ usuń w admin mode — kasuje sklep DOWOLNEGO gracza)
        int by1 = y + (rowH - 16) / 2, by2 = by1 + 16;
        int navX1 = cAction + 2, navX2 = x2 - 3;
        if (ClientAdminState.isActive()) {
            int delX2 = x2 - 3, delX1 = delX2 - 16;
            navX2 = delX1 - 3;
            boolean dh = mx >= delX1 && mx < delX2 && my >= by1 && my < by2;
            bevel(g, delX1, by1, delX2, by2, dh ? RED_LIGHT : RED, RED_DARK, RED_BORDER);
            g.drawString(this.font, "X", delX1 + 5, by1 + 4, WHITE, false);
            hits.add(new Hit(delX1, by1, delX2, by2, () -> confirm.open(
                    Component.translatable("gui.create_marketplace.confirm.delete_offer"),
                    () -> PacketDistributor.sendToServer(
                            new DeleteShopPayload(Optional.of(o.pos()), Optional.empty())))));
        }
        boolean bh = mx >= navX1 && mx < navX2 && my >= by1 && my < by2;
        bevel(g, navX1, by1, navX2, by2, bh ? PATINA_LIGHT : PATINA, PATINA_DARK, 0xFF1F4F49);
        String nav = Component.translatable("gui.create_marketplace.global_market.navigate").getString();
        int navW = this.font.width(nav);
        g.drawString(this.font, trim(nav, navX2 - navX1 - 4), navX1 + Math.max(3, (navX2 - navX1 - navW) / 2), by1 + 4, WHITE, false);
        hits.add(new Hit(navX1, by1, navX2, by2, () -> navigate(o)));

        // tooltip przedmiotu
        if (mx >= slotX && mx < slotX + 18 && my >= slotY && my < slotY + 18
                && o.item() != null && !o.item().isEmpty()) {
            g.renderTooltip(this.font, o.item(), mx, my);
        }
    }

    private void renderFooter(GuiGraphics g, Layout l) {
        bevel(g, l.left, l.footY, l.right, l.footY + 14, PARCH_2, PARCH_2, FRAME);
        int shown = view.size();
        g.drawString(this.font, Component.literal(shown + " / " + offers.size()), l.left + 6, l.footY + 3, 0xFF5A4828, false);
        Component hint = Component.translatable("gui.create_marketplace.footer.hint");
        int hw = this.font.width(hint);
        g.drawString(this.font, hint, l.right - hw - 6, l.footY + 3, 0xFF7A663C, false);
    }

    // --- interakcje ---

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (confirm.active()) {
            if (button == 0) confirm.mouseClicked(mx, my);
            return true;
        }
        if (super.mouseClicked(mx, my, button)) return true;
        if (button == 0) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (h.in(mx, my)) {
                    h.action().run();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (confirm.keyPressed(key)) return true;
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollOffset = Mth.clamp(scrollOffset - (int) (scrollY * 20), 0, maxScroll);
        return true;
    }

    private void navigate(MarketOffer o) {
        if (this.minecraft == null || this.minecraft.player == null) return;
        boolean ok = XaeroCompat.addShopWaypoint(o);
        this.minecraft.player.sendSystemMessage(ok
                ? Component.translatable("gui.create_marketplace.global_market.navigate_success", o.shopName())
                        .withStyle(ChatFormatting.GREEN)
                : Component.translatable("gui.create_marketplace.global_market.navigate_fallback",
                        o.pos().toShortString(), dimLabel(o.dimension().toString()))
                        .withStyle(ChatFormatting.YELLOW));
        this.onClose();
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

    // --- pomocnicze ---

    private String trim(String s, int maxWidth) {
        return MarketTheme.trim(this.font, s, maxWidth);
    }

    /** zlokalizowana nazwa właściciela — "Serwer"/"Server" zależnie od języka gry */
    private static String ownerLabel(String name) {
        return AdminMode.SERVER_NAME.equals(name)
                ? Component.translatable("gui.create_marketplace.owner.server").getString()
                : name;
    }
}
