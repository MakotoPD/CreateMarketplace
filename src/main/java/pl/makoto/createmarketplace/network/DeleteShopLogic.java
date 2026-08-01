package pl.makoto.createmarketplace.network;

import net.minecraft.core.BlockPos;
import pl.makoto.createmarketplace.data.MarketOffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Wybór ofert do usunięcia — używane przez {@link ServerPayloadHandler#handleDeleteShop}.
 * Sam nie modyfikuje bazy: zwraca listę trafień, o resztę dba handler.
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
     * Pojedyncza oferta pod daną pozycją. Gracz może usunąć tylko własną,
     * admin — dowolną.
     *
     * @param offers   bieżąca lista ofert (nie jest modyfikowana)
     * @param pos      pozycja do usunięcia
     * @param playerId UUID gracza wysyłającego żądanie
     * @param admin    czy gracz jest w trybie administratora
     */
    public static DeleteResult deleteByPosition(List<MarketOffer> offers, BlockPos pos, UUID playerId, boolean admin) {
        Optional<MarketOffer> existing = offers.stream()
                .filter(o -> o.pos().equals(pos) && (admin || o.ownerId().equals(playerId)))
                .findFirst();
        if (existing.isPresent()) {
            return new DeleteResult(true, List.of(existing.get()));
        }
        return new DeleteResult(false, List.of());
    }

    /**
     * Wszystkie oferty danego sklepu należące do {@code ownerId}. Handler podaje tu
     * UUID gracza albo {@code AdminMode.SERVER_UUID} — sklepy "Serwera" w trybie admina.
     *
     * @param offers   bieżąca lista ofert (nie jest modyfikowana)
     * @param shopName nazwa sklepu do usunięcia
     * @param ownerId  właściciel, którego oferty wolno usunąć
     */
    public static DeleteResult deleteByShopName(List<MarketOffer> offers, String shopName, UUID ownerId) {
        List<MarketOffer> toRemove = offers.stream()
                .filter(o -> o.shopName().equals(shopName) && o.ownerId().equals(ownerId))
                .toList();
        return new DeleteResult(!toRemove.isEmpty(), toRemove);
    }
}
