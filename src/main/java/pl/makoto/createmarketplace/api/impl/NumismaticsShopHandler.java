package pl.makoto.createmarketplace.api.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import pl.makoto.createmarketplace.api.IShopHandler;
import pl.makoto.createmarketplace.api.ShopResult;
import pl.makoto.createmarketplace.data.StockInfo;
import pl.makoto.createmarketplace.util.Coinage;
import pl.makoto.createmarketplace.util.ShopScanner;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Handler wspierający bloki z moda Numismatics (Vendor i Table Cloth)
 * oraz wszystkie bloki dziedziczące po nich, np. półki z moda Tradeworks
 * (Shelf, Metal Shelf, Side Shelf, Inverted Table Cloth).
 */
public class NumismaticsShopHandler implements IShopHandler {

    /**
     * Sprawdza, czy klasa (lub którakolwiek z jej nadklas) zawiera podaną nazwę.
     * Dzięki temu rozpoznajemy też bloki addonów dziedziczące po Create/Numismatics,
     * np. SideShelfBlockEntity z Tradeworks, który rozszerza TableClothBlockEntity.
     */
    private static boolean isInHierarchy(Class<?> clazz, String classNamePart) {
        while (clazz != null && clazz != Object.class) {
            if (clazz.getName().contains(classNamePart)) {
                return true;
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }

    /**
     * Stan magazynowy: dla straganu Create pytamy jego własną metodę (obejmuje
     * całą sieć logistyczną — vaulty, skrzynie, Stock Link), dla vendora
     * Numismatics liczymy sloty jego magazynu.
     */
    @Override
    public OptionalInt getStock(BlockEntity be, Level level, BlockPos pos) {
        if (isOwnBlock(be)) return OptionalInt.empty();

        OptionalInt cloth = tableClothStock(be);
        if (cloth.isPresent()) return cloth;

        return vendorStock(be);
    }

    /**
     * Create Table Cloth: {@code getStockLevelForTrade(ShoppingList)} zwraca wprost
     * liczbę możliwych transakcji, sam odnajdując Stock Ticker i odpytując sieć.
     * Create trzyma podsumowania sieci w cache'u na 20 ticków, więc to tanie.
     */
    private static OptionalInt tableClothStock(BlockEntity be) {
        Object result = ShopScanner.invokeWithNullArg(be, "getStockLevelForTrade").orElse(null);
        if (!(result instanceof Integer level)) return OptionalInt.empty();
        // MAX_VALUE = zamówienie bez pozycji o dodatniej ilości (stragan skonfigurowany
        // wadliwie). Nie mamy z czego liczyć — lepiej "nie wiem" niż fałszywe "pełny".
        if (level == Integer.MAX_VALUE) return OptionalInt.empty();
        return OptionalInt.of(Math.max(0, level));
    }

    /**
     * Numismatics Vendor implementuje vanillowy {@code Container} (sloty 0..8 to jego
     * magazyn), więc czytamy go bez refleksji. Refleksja potrzebna tylko do trybu,
     * sprzedawanego przedmiotu i flagi kreatywnej.
     *
     * <p>W trybie BUY vendor skupuje od graczy — "dostępność" to wtedy wolne miejsce
     * w magazynie, nie towar.
     */
    private static OptionalInt vendorStock(BlockEntity be) {
        if (!(be instanceof Container container)) return OptionalInt.empty();
        if (!isInHierarchy(be.getClass(), "VendorBlockEntity")) return OptionalInt.empty();

        if (ShopScanner.invokeBoolean(be, "isCreativeVendor")) {
            return OptionalInt.of(StockInfo.INFINITE);
        }

        ItemStack selling = vendorItem(be);
        if (selling.isEmpty()) return OptionalInt.empty();
        int unit = Math.max(1, selling.getCount());

        boolean buying = "BUY".equals(String.valueOf(ShopScanner.invoke(be, "getMode").orElse("")));

        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (buying) {
                // wolne miejsce na przyjmowany towar
                if (stack.isEmpty()) total += selling.getMaxStackSize();
                else if (matches(be, selling, stack)) total += Math.max(0, stack.getMaxStackSize() - stack.getCount());
            } else if (matches(be, selling, stack)) {
                total += stack.getCount();
            }
        }
        return OptionalInt.of(total / unit);
    }

    /**
     * Porównanie przez {@code matchesFilterItem}/{@code matchesSellingItem} vendora, żeby respektować jego
     * własne reguły (Numismatics ignoruje np. koszt naprawy). Fallback na
     * porównanie samego przedmiotu, gdyby metoda zniknęła.
     */
    private static boolean matches(BlockEntity be, ItemStack selling, ItemStack candidate) {
        if (candidate.isEmpty()) return false;
        for (String name : new String[]{"matchesFilterItem", "matchesSellingItem"}) {
            try {
                if (be.getClass().getMethod(name, ItemStack.class).invoke(be, candidate) instanceof Boolean b) return b;
            } catch (Exception ignored) {}
        }
        return ItemStack.isSameItem(selling, candidate);
    }

    private static ItemStack vendorItem(BlockEntity be) {
        return ShopScanner.invokeMethodReturningItemStack(be, "getFilterItem")
                .or(() -> ShopScanner.invokeMethodReturningItemStack(be, "getSellingItem"))
                .orElse(ItemStack.EMPTY);
    }

    private static boolean isOwnBlock(BlockEntity be) {
        return be.getClass().getName().startsWith("pl.makoto.createmarketplace.");
    }

    @Override
    public Optional<ShopResult> tryResolve(BlockEntity be, Level level, BlockPos pos) {
        // Rozpoznawanie po nazwie klasy łapie też NASZE bloki (ServerVendorBlockEntity
        // zawiera "VendorBlockEntity"). Własne bloki obsługują dedykowane handlery,
        // więc odrzucamy cały nasz pakiet — odporne na dodanie kolejnego bloku.
        if (be.getClass().getName().startsWith("pl.makoto.createmarketplace.")) {
            return Optional.empty();
        }

        boolean isVendor = isInHierarchy(be.getClass(), "VendorBlockEntity");
        boolean isTableCloth = !isVendor && isInHierarchy(be.getClass(), "TableClothBlockEntity");

        if (!isVendor && !isTableCloth) {
            return Optional.empty();
        }

        try {
            ItemStack sellingItem = ItemStack.EMPTY;
            ItemStack currencyItem = ItemStack.EMPTY;
            CompoundTag nbt = be.saveWithFullMetadata(level.registryAccess());

            if (isVendor) {
                // Logika dla Vendor
                sellingItem = vendorItem(be);

                // Numismatics trzyma cenę rozbitą na nominały (SliderStylePriceBehaviour).
                // Sumujemy WSZYSTKIE do spurów — inaczej vendor za "1 cog + 3 spur"
                // pokazywałby na rynku tylko "1 cog".
                if (nbt.contains("Prices", 10)) {
                    CompoundTag prices = nbt.getCompound("Prices");
                    int totalSpurs = 0;
                    for (int i = 0; i < Coinage.NAMES.length; i++) {
                        String coinName = Coinage.NAMES[i];
                        if (prices.contains(coinName)) {
                            totalSpurs += Math.max(0, prices.getInt(coinName)) * Coinage.VALUES[i];
                        }
                    }
                    currencyItem = Coinage.highestSingleCoin(totalSpurs);
                }
            } else {
                // Logika dla Table Cloth
                sellingItem = ShopScanner.invokeMethodReturningItemStack(be, "getSellingItem")
                        .or(() -> ShopScanner.invokeMethodReturningItemStack(be, "getFilterItem"))
                        .or(() -> {
                            try {
                                java.lang.reflect.Method m = be.getClass().getMethod("getItemsForRender");
                                Object result = m.invoke(be);
                                if (result instanceof Iterable<?> iterable) {
                                    for (Object o : iterable) {
                                        if (o instanceof ItemStack rs && !rs.isEmpty())
                                            return java.util.Optional.of(rs.copy());
                                    }
                                }
                            } catch (Exception ignored) {}
                            return java.util.Optional.empty();
                        })
                        .orElse(ItemStack.EMPTY);

                if (nbt.contains("RequestData")) {
                    CompoundTag requestData = nbt.getCompound("RequestData");
                    if (requestData.contains("encoded_request")) {
                        CompoundTag encodedRequest = requestData.getCompound("encoded_request");
                        if (encodedRequest.contains("ordered_stacks")) {
                            CompoundTag orderedStacks = encodedRequest.getCompound("ordered_stacks");
                            if (orderedStacks.contains("entries")) {
                                net.minecraft.nbt.ListTag entries = orderedStacks.getList("entries", 10);
                                if (!entries.isEmpty()) {
                                    CompoundTag entry = entries.getCompound(0);
                                    if (sellingItem.isEmpty() && entry.contains("item_stack")) {
                                        sellingItem = ItemStack.parseOptional(level.registryAccess(),
                                                entry.getCompound("item_stack"));
                                    }
                                    if (!sellingItem.isEmpty() && entry.contains("count")) {
                                        sellingItem.setCount(entry.getInt("count"));
                                    }
                                }
                            }
                        }
                    }
                }
                if (nbt.contains("Filter")) {
                    currencyItem = ItemStack.parseOptional(level.registryAccess(), nbt.getCompound("Filter"));
                    if (nbt.contains("FilterAmount")) {
                        currencyItem.setCount(nbt.getInt("FilterAmount"));
                    } else if (nbt.contains("Price")) {
                        currencyItem.setCount(nbt.getInt("Price"));
                    } else if (nbt.contains("price")) {
                        currencyItem.setCount(nbt.getInt("price"));
                    }
                }
            }

            if (sellingItem.isEmpty() && currencyItem.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new ShopResult(sellingItem, currencyItem));

        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
