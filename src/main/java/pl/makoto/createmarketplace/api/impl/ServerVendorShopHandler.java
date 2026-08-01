package pl.makoto.createmarketplace.api.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import pl.makoto.createmarketplace.api.IShopHandler;
import pl.makoto.createmarketplace.api.ShopResult;
import pl.makoto.createmarketplace.block.ServerVendorBlockEntity;
import pl.makoto.createmarketplace.data.StockInfo;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Rozpoznaje blok Server Vendor dla MarketApi (registration_book).
 *
 * Niedokończony blok (bez przedmiotu lub bez ceny kupna) zwraca {@code Optional.empty()} —
 * nie da się go opublikować na Global Market.
 */
public class ServerVendorShopHandler implements IShopHandler {

    /** Server Vendor handluje w imieniu serwera — nie ma magazynu, nigdy się nie kończy. */
    @Override
    public OptionalInt getStock(BlockEntity be, Level level, BlockPos pos) {
        if (!(be instanceof ServerVendorBlockEntity sv)) return OptionalInt.empty();
        if (sv.getTradeItem().isEmpty()) return OptionalInt.of(0);
        return OptionalInt.of(StockInfo.INFINITE);
    }

    @Override
    public Optional<ShopResult> tryResolve(BlockEntity be, Level level, BlockPos pos) {
        if (!(be instanceof ServerVendorBlockEntity sv)) return Optional.empty();
        ItemStack item = sv.getTradeItem();
        if (item.isEmpty()) return Optional.empty();
        ItemStack buy = sv.getBuyPrice();
        if (buy.isEmpty()) return Optional.empty();
        return Optional.of(new ShopResult(item.copy(), buy.copy()));
    }
}
