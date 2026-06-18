package pl.makoto.createmarketplace.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Konwersje pomiędzy nominałami Numismatics a pojedynczą wartością w "spurs"
 * oraz operacje na ekwipunku gracza (liczenie, pobieranie, wypłacanie).
 *
 * Skala: spur=1, bevel=8, sprocket=16, cog=64, crown=512, sun=4096.
 *
 * Pobranie i wydanie obsługuje zarówno walutę Numismatics (auto-konwersja
 * w pełnych nominałach) jak i dowolny przedmiot użyty jako custom currency.
 */
public final class Coinage {
    private Coinage() {}

    public static final String[] NAMES  = {"sun", "crown", "cog", "sprocket", "bevel", "spur"};
    public static final int[]    VALUES = {4096,  512,    64,   16,         8,       1};

    public static int valueOf(String name) {
        for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equals(name)) return VALUES[i];
        return 0;
    }

    /** Rozkłada wartość w spurs na monety od najwyższej do najniższej (greedy). */
    public static Map<String, Integer> fromSpurs(int spurs) {
        Map<String, Integer> out = new LinkedHashMap<>();
        int remaining = Math.max(0, spurs);
        for (int i = 0; i < NAMES.length; i++) {
            int c = remaining / VALUES[i];
            if (c > 0) {
                out.put(NAMES[i], c);
                remaining -= c * VALUES[i];
            }
        }
        return out;
    }

    /** Buduje listę ItemStacków monet odpowiadającą wartości w spurs. Stacki łamane do 64. */
    public static List<ItemStack> coinStacks(int spurs) {
        List<ItemStack> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : fromSpurs(spurs).entrySet()) {
            int left = e.getValue();
            while (left > 0) {
                int take = Math.min(left, 64);
                ItemStack s = ShopScanner.getCoinItemByName(e.getKey(), take);
                if (s.isEmpty()) break;
                out.add(s);
                left -= take;
            }
        }
        return out;
    }

    /** Najwyższy "jedno-stackowy" odpowiednik (np. 600 spurs → 1 crown). Używane dla ShopResult na rynku. */
    public static ItemStack highestSingleCoin(int spurs) {
        if (spurs <= 0) return ItemStack.EMPTY;
        for (int i = 0; i < NAMES.length; i++) {
            if (spurs >= VALUES[i]) {
                int count = Math.max(1, spurs / VALUES[i]);
                if (count > 64) count = 64;
                ItemStack s = ShopScanner.getCoinItemByName(NAMES[i], count);
                if (!s.isEmpty()) return s;
            }
        }
        return ShopScanner.getCoinItemByName("spur", Math.min(spurs, 64));
    }

    /** Czytelny tekst typu "1 crown, 2 cogs, 3 spurs". */
    public static String formatPlain(int spurs) {
        if (spurs <= 0) return "0 spurs";
        Map<String, Integer> m = fromSpurs(spurs);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(e.getValue()).append(' ').append(e.getKey());
            if (e.getValue() > 1 && !e.getKey().endsWith("s")) sb.append('s');
            first = false;
        }
        return sb.toString();
    }

    /** Suma wartości w spurs ze wszystkich monet Numismatics w ekwipunku. */
    public static int countSpursInInventory(Inventory inv) {
        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.isEmpty()) continue;
            int v = coinValueOf(st);
            if (v > 0) total += v * st.getCount();
        }
        return total;
    }

    public static boolean canDeductSpurs(Inventory inv, int amount) {
        return countSpursInInventory(inv) >= amount;
    }

    /**
     * Atomowo pobiera podaną wartość w spurs z ekwipunku — najpierw rozmienia (jeśli trzeba)
     * przez wypłacenie reszty. Implementacja: liczy wartość, pobiera kolejne stacki monet
     * od najmniejszych, na koniec wypłaca resztę jako monety wyższych nominałów.
     * Zwraca false gdy brak wystarczających środków (ekwipunek niezmieniony).
     */
    public static boolean deductSpurs(Inventory inv, int amount) {
        if (amount <= 0) return true;
        if (!canDeductSpurs(inv, amount)) return false;

        int remaining = amount;
        // Pobieraj od najmniejszych monet — minimalizuje konieczność wydania reszty.
        for (int i = NAMES.length - 1; i >= 0 && remaining > 0; i--) {
            int coinValue = VALUES[i];
            for (int slot = 0; slot < inv.getContainerSize() && remaining > 0; slot++) {
                ItemStack st = inv.getItem(slot);
                if (st.isEmpty()) continue;
                if (coinValueOf(st) != coinValueOf(coinTemplate(NAMES[i]))) continue;
                while (!st.isEmpty() && remaining >= coinValue) {
                    st.shrink(1);
                    remaining -= coinValue;
                }
            }
        }
        if (remaining == 0) return true;

        // Trzeba "rozmienić": pobierz większą monetę i wypłać resztę.
        for (int i = 0; i < NAMES.length && remaining > 0; i++) {
            int coinValue = VALUES[i];
            if (coinValue <= remaining) continue;
            for (int slot = 0; slot < inv.getContainerSize(); slot++) {
                ItemStack st = inv.getItem(slot);
                if (st.isEmpty()) continue;
                if (coinValueOf(st) != coinValue) continue;
                st.shrink(1);
                int change = coinValue - remaining;
                remaining = 0;
                // Wypłać resztę z powrotem
                giveAmount(inv, change);
                return true;
            }
        }
        return remaining == 0;
    }

    /** Wsadza monety o łącznej wartości {@code amount} do ekwipunku gracza; nadmiar dropuje. */
    public static void giveSpurs(ServerPlayer p, int amount) {
        for (ItemStack s : coinStacks(amount)) {
            if (!p.getInventory().add(s)) {
                p.drop(s, false);
            }
        }
    }

    private static void giveAmount(Inventory inv, int amount) {
        for (ItemStack s : coinStacks(amount)) {
            if (!inv.add(s)) {
                inv.player.drop(s, false);
            }
        }
    }

    private static ItemStack coinTemplate(String name) {
        return ShopScanner.getCoinItemByName(name, 1);
    }

    /** Zwraca wartość pojedynczej sztuki podanego stacka jeśli to moneta Numismatics, inaczej 0. */
    public static int coinValueOf(ItemStack st) {
        if (st.isEmpty()) return 0;
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(st.getItem());
        if (!"numismatics".equals(id.getNamespace())) return 0;
        return valueOf(id.getPath());
    }

    /** True, jeśli stack to moneta Numismatics (spur/bevel/sprocket/cog/crown/sun). */
    public static boolean isNumismaticsCoin(ItemStack st) {
        return coinValueOf(st) > 0;
    }

    /**
     * Tryb monet — gdy waluta-custom jest pusta LUB jest monetą Numismatics.
     * W tym trybie ceny przechowywane są w spursach, a gracz płaci dowolną
     * mieszanką monet Numismatics. Moneta w slocie waluty to tylko podpowiedź
     * w jakim nominale admin chce myśleć o cenie.
     */
    public static boolean isCoinageMode(ItemStack currency) {
        return currency.isEmpty() || isNumismaticsCoin(currency);
    }

    // ---- Custom currency ----------------------------------------------------

    /** Liczy ile sztuk przedmiotu {@code template} jest w ekwipunku (ignorując count templatki). */
    public static int countCustom(Inventory inv, ItemStack template) {
        if (template.isEmpty()) return 0;
        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (!st.isEmpty() && ItemStack.isSameItemSameComponents(st, template)) {
                total += st.getCount();
            }
        }
        return total;
    }

    public static boolean canDeductCustom(Inventory inv, ItemStack template, int amount) {
        return countCustom(inv, template) >= amount;
    }

    public static boolean deductCustom(Inventory inv, ItemStack template, int amount) {
        if (amount <= 0) return true;
        if (!canDeductCustom(inv, template, amount)) return false;
        int remaining = amount;
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack st = inv.getItem(i);
            if (st.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(st, template)) continue;
            int take = Math.min(remaining, st.getCount());
            st.shrink(take);
            remaining -= take;
        }
        return remaining == 0;
    }

    public static void giveCustom(ServerPlayer p, ItemStack template, int amount) {
        int maxStack = template.getMaxStackSize();
        int left = amount;
        while (left > 0) {
            int take = Math.min(left, maxStack);
            ItemStack s = template.copyWithCount(take);
            if (!p.getInventory().add(s)) {
                p.drop(s, false);
            }
            left -= take;
        }
    }
}
