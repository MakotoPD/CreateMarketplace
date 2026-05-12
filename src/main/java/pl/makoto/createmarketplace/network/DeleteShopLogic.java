package pl.makoto.createmarketplace.network;

import net.minecraft.core.BlockPos;
import pl.makoto.createmarketplace.data.MarketOffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Extracted delete logic from ServerPayloadHandler for testability.
 * Determines whether a delete operation actually removes offers and returns the result.
 */
public class DeleteShopLogic {

    /**
     * Result of a delete operation.
     *
     * @param changed   true if at least one offer was removed
     * @param removed   the list of offers that were removed
     */
    public record DeleteResult(boolean changed, List<MarketOffer> removed) {}

    /**
     * Attempts to delete an offer by position. Only removes offers owned by the specified player.
     *
     * @param offers   the current list of offers (not modified)
     * @param pos      the position to delete
     * @param playerId the UUID of the requesting player
     * @return a DeleteResult indicating whether a change occurred and which offers were removed
     */
    static DeleteResult deleteByPosition(List<MarketOffer> offers, BlockPos pos, UUID playerId) {
        Optional<MarketOffer> existing = offers.stream()
                .filter(o -> o.pos().equals(pos) && o.ownerId().equals(playerId))
                .findFirst();
        if (existing.isPresent()) {
            return new DeleteResult(true, List.of(existing.get()));
        }
        return new DeleteResult(false, List.of());
    }

    /**
     * Attempts to delete offers by shop name. Only removes offers owned by the specified player.
     *
     * @param offers   the current list of offers (not modified)
     * @param shopName the shop name to delete
     * @param playerId the UUID of the requesting player
     * @return a DeleteResult indicating whether a change occurred and which offers were removed
     */
    static DeleteResult deleteByShopName(List<MarketOffer> offers, String shopName, UUID playerId) {
        List<MarketOffer> toRemove = offers.stream()
                .filter(o -> o.shopName().equals(shopName) && o.ownerId().equals(playerId))
                .toList();
        return new DeleteResult(!toRemove.isEmpty(), toRemove);
    }
}
