package pl.makoto.createmarketplace.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Interfejs dla handlerów sklepów. Pozwala na dodanie wsparcia dla nowych typów bloków.
 */
public interface IShopHandler {

    /**
     * Próbuje wyciągnąć dane o ofercie z danego BlockEntity.
     * @param be Blok encji do sprawdzenia.
     * @param level Poziom (świat).
     * @param pos Pozycja bloku.
     * @return Optional z wynikiem ShopResult, jeśli handler rozpoznał blok i znalazł ofertę.
     */
    Optional<ShopResult> tryResolve(BlockEntity be, Level level, BlockPos pos);

    /**
     * Ile jeszcze transakcji da się w tym sklepie wykonać — podstawa znacznika
     * "aktywny / pusty" na Global Market.
     *
     * <p>Wywoływane wyłącznie po stronie serwera i tylko dla bloków w ZAŁADOWANYCH
     * chunkach, więc implementacja może swobodnie czytać sąsiednie bloki.
     *
     * <p>Zwróć {@link OptionalInt#empty()}, jeśli nie potrafisz tego ustalić —
     * rynek pokaże wtedy "nieznany" zamiast zgadywać. Nigdy nie zwracaj {@code 0}
     * "na wszelki wypadek": zero oznacza potwierdzony pusty sklep i odstraszy graczy.
     *
     * @return liczba możliwych transakcji, {@link pl.makoto.createmarketplace.data.StockInfo#INFINITE}
     *         dla sklepów bez magazynu, albo pusty {@code OptionalInt} gdy nie wiadomo
     */
    default OptionalInt getStock(BlockEntity be, Level level, BlockPos pos) {
        return OptionalInt.empty();
    }
}
