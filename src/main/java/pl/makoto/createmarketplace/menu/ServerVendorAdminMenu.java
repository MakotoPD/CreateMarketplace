package pl.makoto.createmarketplace.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import pl.makoto.createmarketplace.block.ServerVendorBlockEntity;
import pl.makoto.createmarketplace.registry.MenuRegistry;
import pl.makoto.createmarketplace.util.Coinage;

/**
 * Menu konfiguracji Server Vendor. Trzy ghost-sloty: przedmiot, cena kupna, cena sprzedaży.
 *
 * Klikanie:
 *  - lewy klik z przedmiotem na kursorze → kopiuje stack do slotu (kursor nietknięty),
 *  - prawy klik → czyści slot,
 *  - scroll na slocie → zwiększa/zmniejsza count (obsługa po stronie ekranu przez przyciski INC/DEC).
 *
 * Wartość ceny:
 *  - dla monety Numismatics: count × wartość_w_spurach (np. 5 cogs = 320 spurs),
 *  - dla custom: count sztuk tego przedmiotu (np. 3 emerald = 3 emerald).
 */
public class ServerVendorAdminMenu extends AbstractContainerMenu {

    // ID akcji menu
    public static final int BTN_CLEAR_TRADE   = 0;
    public static final int BTN_CLEAR_BUY     = 1;
    public static final int BTN_CLEAR_SELL    = 2;
    public static final int BTN_TOGGLE_BUY    = 3;
    public static final int BTN_TOGGLE_SELL   = 4;
    public static final int BTN_SUGGEST_60    = 5;
    public static final int BTN_TRADE_INC     = 6;
    public static final int BTN_TRADE_DEC     = 7;
    public static final int BTN_BUY_INC       = 8;
    public static final int BTN_BUY_DEC       = 9;
    public static final int BTN_SELL_INC      = 10;
    public static final int BTN_SELL_DEC      = 11;

    public final ServerVendorBlockEntity be;
    public final BlockPos pos;

    public final SimpleContainer tradeSlot = new SimpleContainer(1);
    public final SimpleContainer buyPriceSlot = new SimpleContainer(1);
    public final SimpleContainer sellPriceSlot = new SimpleContainer(1);

    public boolean buyEnabled;
    public boolean sellEnabled;

    /** Konstruktor klienta — dane z bufora. */
    public ServerVendorAdminMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, null,
                buf.readBlockPos(),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                buf.readBoolean(),
                buf.readBoolean());
    }

    /** Konstruktor serwera — bezpośrednio z BE. */
    public ServerVendorAdminMenu(int id, Inventory inv, ServerVendorBlockEntity be) {
        this(id, inv, be,
                be.getBlockPos(),
                be.getTradeItem().copy(),
                be.getBuyPrice().copy(),
                be.getSellPrice().copy(),
                be.isBuyEnabled(),
                be.isSellEnabled());
    }

    private ServerVendorAdminMenu(int id, Inventory inv, ServerVendorBlockEntity be,
                                  BlockPos pos, ItemStack tradeItem, ItemStack buyPrice, ItemStack sellPrice,
                                  boolean buyEn, boolean sellEn) {
        super(MenuRegistry.SERVER_VENDOR_ADMIN.get(), id);
        this.be = be;
        this.pos = pos;
        this.tradeSlot.setItem(0, tradeItem == null ? ItemStack.EMPTY : tradeItem);
        this.buyPriceSlot.setItem(0, buyPrice == null ? ItemStack.EMPTY : buyPrice);
        this.sellPriceSlot.setItem(0, sellPrice == null ? ItemStack.EMPTY : sellPrice);
        this.buyEnabled = buyEn;
        this.sellEnabled = sellEn;

        // Ghost sloty: kopia z kursora bez konsumpcji.
        this.addSlot(new GhostSlot(tradeSlot, 0, 24, 24));
        this.addSlot(new GhostSlot(buyPriceSlot, 0, 24, 52));
        this.addSlot(new GhostSlot(sellPriceSlot, 0, 24, 80));

        // Ekwipunek gracza na dole, wycentrowany.
        int invX = 47, invY = 132;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, invX + col * 18, invY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, invX + col * 18, invY + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        if (be == null) return true; // klient
        return be.getLevel() != null
                && be.getLevel().getBlockEntity(pos) == be
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    /** Klik na ghost slot: lewy = kopia z kursora, prawy = czyść. Inne sloty: standardowo. */
    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < 3 && clickType == ClickType.PICKUP) {
            Slot slot = this.slots.get(slotId);
            ItemStack carried = this.getCarried();
            if (dragType == 0) {
                if (!carried.isEmpty()) {
                    int max = Math.min(carried.getMaxStackSize(), carried.getCount());
                    slot.set(carried.copyWithCount(max));
                } else {
                    slot.set(ItemStack.EMPTY);
                }
            } else if (dragType == 1) {
                // Prawy klik: czyść slot (lub jeśli niepusty + kursor pusty → zmniejsz o 1)
                if (carried.isEmpty() && !slot.getItem().isEmpty()) {
                    ItemStack s = slot.getItem();
                    if (s.getCount() > 1) {
                        s.shrink(1);
                        slot.set(s);
                    } else {
                        slot.set(ItemStack.EMPTY);
                    }
                } else if (!carried.isEmpty()) {
                    // Prawy klik z przedmiotem na kursorze: zwiększ count w slocie o 1
                    ItemStack s = slot.getItem();
                    if (s.isEmpty()) {
                        slot.set(carried.copyWithCount(1));
                    } else if (ItemStack.isSameItemSameComponents(s, carried) && s.getCount() < s.getMaxStackSize()) {
                        s.grow(1);
                        slot.set(s);
                    }
                }
            }
            return;
        }
        super.clicked(slotId, dragType, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Shift-click z inwentarza do pierwszego pustego ghost-slotu (tylko trade item przez shift).
        if (index >= 3) {
            Slot src = this.slots.get(index);
            if (src.hasItem()) {
                ItemStack template = src.getItem().copy();
                if (this.tradeSlot.getItem(0).isEmpty()) {
                    this.tradeSlot.setItem(0, template);
                } else if (this.buyPriceSlot.getItem(0).isEmpty()) {
                    this.buyPriceSlot.setItem(0, template);
                } else if (this.sellPriceSlot.getItem(0).isEmpty()) {
                    this.sellPriceSlot.setItem(0, template);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        switch (id) {
            case BTN_CLEAR_TRADE -> tradeSlot.setItem(0, ItemStack.EMPTY);
            case BTN_CLEAR_BUY   -> buyPriceSlot.setItem(0, ItemStack.EMPTY);
            case BTN_CLEAR_SELL  -> sellPriceSlot.setItem(0, ItemStack.EMPTY);
            case BTN_TOGGLE_BUY  -> buyEnabled = !buyEnabled;
            case BTN_TOGGLE_SELL -> sellEnabled = !sellEnabled;

            case BTN_TRADE_INC -> incCount(tradeSlot);
            case BTN_TRADE_DEC -> decCount(tradeSlot);
            case BTN_BUY_INC   -> incCount(buyPriceSlot);
            case BTN_BUY_DEC   -> decCount(buyPriceSlot);
            case BTN_SELL_INC  -> incCount(sellPriceSlot);
            case BTN_SELL_DEC  -> decCount(sellPriceSlot);

            case BTN_SUGGEST_60 -> applySuggest60();

            default -> { return false; }
        }
        return true;
    }

    private void incCount(SimpleContainer c) {
        ItemStack s = c.getItem(0);
        if (s.isEmpty()) return;
        if (s.getCount() < s.getMaxStackSize()) {
            s.grow(1);
            c.setItem(0, s);
        }
    }

    private void decCount(SimpleContainer c) {
        ItemStack s = c.getItem(0);
        if (s.isEmpty()) return;
        if (s.getCount() > 1) {
            s.shrink(1);
            c.setItem(0, s);
        } else {
            c.setItem(0, ItemStack.EMPTY);
        }
    }

    /**
     * Wstaw do {@code sellPriceSlot} cenę = 60% wartości kupna.
     *
     * Dla monety Numismatics: 60% w spursach → wybiera największą monetę, w której
     * wartość dzieli się bez reszty (np. buy=1 bevel=8 spurs, 60%=4 spurs → 4 spur).
     * Dla custom item: count × 60% (truncated), zachowuje ten sam typ przedmiotu.
     */
    private void applySuggest60() {
        ItemStack buy = buyPriceSlot.getItem(0);
        if (buy.isEmpty()) return;
        if (Coinage.isNumismaticsCoin(buy)) {
            int buySpurs = buy.getCount() * Coinage.coinValueOf(buy);
            int sellSpurs = buySpurs * 60 / 100;
            ItemStack result = bestFitCoin(sellSpurs);
            sellPriceSlot.setItem(0, result);
        } else {
            int sellCount = Math.max(0, buy.getCount() * 60 / 100);
            if (sellCount == 0) sellPriceSlot.setItem(0, ItemStack.EMPTY);
            else sellPriceSlot.setItem(0, buy.copyWithCount(Math.min(sellCount, buy.getMaxStackSize())));
        }
    }

    /** Największa moneta Numismatics, w której {@code spurs} dzieli się bez reszty. */
    private static ItemStack bestFitCoin(int spurs) {
        if (spurs <= 0) return ItemStack.EMPTY;
        for (int i = 0; i < Coinage.NAMES.length; i++) {
            int v = Coinage.VALUES[i];
            if (spurs >= v && spurs % v == 0) {
                int count = spurs / v;
                if (count <= 64) {
                    ItemStack s = pl.makoto.createmarketplace.util.ShopScanner.getCoinItemByName(Coinage.NAMES[i], count);
                    if (!s.isEmpty()) return s;
                }
            }
        }
        // Fallback: spur stack ograniczony do 64
        return pl.makoto.createmarketplace.util.ShopScanner.getCoinItemByName("spur", Math.min(spurs, 64));
    }

    public ItemStack getCurrentTradeItem() { return tradeSlot.getItem(0); }
    public ItemStack getCurrentBuyPrice()  { return buyPriceSlot.getItem(0); }
    public ItemStack getCurrentSellPrice() { return sellPriceSlot.getItem(0); }

    /** Ghost slot: count zachowywany w slocie, ale kursor nie jest konsumowany — robi to nasz {@link #clicked}. */
    private static final class GhostSlot extends Slot {
        GhostSlot(Container c, int idx, int x, int y) { super(c, idx, x, y); }

        @Override public int getMaxStackSize() { return 64; }
        @Override public boolean mayPlace(ItemStack stack) { return true; }
        @Override public boolean mayPickup(Player player) { return false; }
    }
}
