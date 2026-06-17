package pl.makoto.createmarketplace.api.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import pl.makoto.createmarketplace.api.IShopHandler;
import pl.makoto.createmarketplace.api.ShopResult;
import pl.makoto.createmarketplace.block.ServerVendorBlockEntity;

import java.util.Optional;

/**
 * Rozpoznaje blok Server Vendor dla MarketApi (registration_book).
 *
 * Niedokończony blok (bez przedmiotu lub bez ceny kupna) zwraca {@code Optional.empty()} —
 * nie da się go opublikować na Global Market.
 */
public class ServerVendorShopHandler implements IShopHandler {

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
