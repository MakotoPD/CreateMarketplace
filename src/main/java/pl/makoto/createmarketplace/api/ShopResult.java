package pl.makoto.createmarketplace.api;

import net.minecraft.world.item.ItemStack;

/**
 * Rezultat skanowania sklepu.
 * @param sellingItem Przedmiot, który jest sprzedawany.
 * @param currencyItem Przedmiot używany jako waluta (cena).
 */
public record ShopResult(ItemStack sellingItem, ItemStack currencyItem) {
}
