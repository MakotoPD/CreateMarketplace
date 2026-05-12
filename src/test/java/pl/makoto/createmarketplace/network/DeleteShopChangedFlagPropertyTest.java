package pl.makoto.createmarketplace.network;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import net.minecraft.core.BlockPos;
import pl.makoto.createmarketplace.data.MarketOffer;
import pl.makoto.createmarketplace.network.DeleteShopLogic.DeleteResult;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for the delete shop changed flag logic.
 *
 * Property 6: Delete reports change only when offers were actually removed.
 *
 * For any MarketDatabase state and for any DeleteShopPayload, the changed flag SHALL be true
 * if and only if at least one offer owned by the requesting player was present and removed.
 * When changed is false, no success message or broadcast SHALL occur.
 *
 * Validates: Requirements 5.1, 5.2, 5.3
 */
class DeleteShopChangedFlagPropertyTest {

    /**
     * Property 6a: Position-based delete sets changed=true only when an offer
     * owned by the player exists at that position.
     *
     * Validates: Requirements 5.1, 5.2, 5.3
     */
    @Property
    void positionDeleteReportsChangeOnlyWhenPlayerOfferExistsAtPosition(
            @ForAll("offerLists") List<MarketOffer> offers,
            @ForAll("blockPositions") BlockPos targetPos,
            @ForAll("playerIds") UUID playerId
    ) {
        DeleteResult result = DeleteShopLogic.deleteByPosition(offers, targetPos, playerId);

        // Check if there actually was an offer owned by this player at this position
        boolean playerOfferExistsAtPos = offers.stream()
                .anyMatch(o -> o.pos().equals(targetPos) && o.ownerId().equals(playerId));

        assertEquals(playerOfferExistsAtPos, result.changed(),
                "changed flag must be true iff an offer owned by the player existed at the position");

        if (!result.changed()) {
            assertTrue(result.removed().isEmpty(),
                    "When changed is false, no offers should be in the removed list");
        } else {
            assertFalse(result.removed().isEmpty(),
                    "When changed is true, at least one offer should be in the removed list");
            // All removed offers must be owned by the player and at the target position
            for (MarketOffer removed : result.removed()) {
                assertEquals(playerId, removed.ownerId());
                assertEquals(targetPos, removed.pos());
            }
        }
    }

    /**
     * Property 6b: Name-based delete sets changed=true only when one or more offers
     * owned by the player with that shop name exist.
     *
     * Validates: Requirements 5.1, 5.2, 5.3
     */
    @Property
    void nameDeleteReportsChangeOnlyWhenPlayerOffersWithNameExist(
            @ForAll("offerLists") List<MarketOffer> offers,
            @ForAll("shopNames") String targetShopName,
            @ForAll("playerIds") UUID playerId
    ) {
        DeleteResult result = DeleteShopLogic.deleteByShopName(offers, targetShopName, playerId);

        // Check if there actually were offers owned by this player with this shop name
        long matchingCount = offers.stream()
                .filter(o -> o.shopName().equals(targetShopName) && o.ownerId().equals(playerId))
                .count();
        boolean playerOffersExist = matchingCount > 0;

        assertEquals(playerOffersExist, result.changed(),
                "changed flag must be true iff offers owned by the player with that shop name existed");

        if (!result.changed()) {
            assertTrue(result.removed().isEmpty(),
                    "When changed is false, no offers should be in the removed list");
        } else {
            assertEquals(matchingCount, result.removed().size(),
                    "All matching offers should be in the removed list");
            // All removed offers must be owned by the player and have the target shop name
            for (MarketOffer removed : result.removed()) {
                assertEquals(playerId, removed.ownerId());
                assertEquals(targetShopName, removed.shopName());
            }
        }
    }

    /**
     * Property 6c: Delete by position never removes offers belonging to other players,
     * even if they are at the same position.
     *
     * Validates: Requirements 5.1
     */
    @Property
    void positionDeleteNeverRemovesOtherPlayersOffers(
            @ForAll("offerLists") List<MarketOffer> offers,
            @ForAll("blockPositions") BlockPos targetPos,
            @ForAll("playerIds") UUID playerId
    ) {
        DeleteResult result = DeleteShopLogic.deleteByPosition(offers, targetPos, playerId);

        // No removed offer should belong to a different player
        for (MarketOffer removed : result.removed()) {
            assertEquals(playerId, removed.ownerId(),
                    "Position-based delete must never remove offers from other players");
        }
    }

    /**
     * Property 6d: Delete by name never removes offers belonging to other players,
     * even if they have the same shop name.
     *
     * Validates: Requirements 5.2
     */
    @Property
    void nameDeleteNeverRemovesOtherPlayersOffers(
            @ForAll("offerLists") List<MarketOffer> offers,
            @ForAll("shopNames") String targetShopName,
            @ForAll("playerIds") UUID playerId
    ) {
        DeleteResult result = DeleteShopLogic.deleteByShopName(offers, targetShopName, playerId);

        // No removed offer should belong to a different player
        for (MarketOffer removed : result.removed()) {
            assertEquals(playerId, removed.ownerId(),
                    "Name-based delete must never remove offers from other players");
        }
    }

    // --- Generators ---

    @Provide
    Arbitrary<UUID> playerIds() {
        return Arbitraries.create(UUID::randomUUID);
    }

    @Provide
    Arbitrary<BlockPos> blockPositions() {
        return Arbitraries.integers().between(-1000, 1000)
                .tuple3()
                .map(t -> new BlockPos(t.get1(), t.get2(), t.get3()));
    }

    @Provide
    Arbitrary<String> shopNames() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20);
    }

    @Provide
    Arbitrary<List<MarketOffer>> offerLists() {
        return marketOffers().list().ofMinSize(0).ofMaxSize(10);
    }

    private Arbitrary<MarketOffer> marketOffers() {
        Arbitrary<UUID> ownerIds = playerIds();
        Arbitrary<String> ownerNames = Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(16);
        Arbitrary<String> names = shopNames();
        Arbitrary<BlockPos> positions = blockPositions();
        Arbitrary<Long> timestamps = Arbitraries.longs().between(0, System.currentTimeMillis());

        return Combinators.combine(ownerIds, ownerNames, names, positions, timestamps)
                .as((ownerId, ownerName, shopName, pos, timestamp) ->
                        new MarketOffer(ownerId, ownerName, shopName, pos, null, null, timestamp));
    }
}
