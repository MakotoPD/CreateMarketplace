package pl.makoto.createmarketplace.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

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
}
