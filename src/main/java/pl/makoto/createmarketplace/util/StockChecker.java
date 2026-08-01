package pl.makoto.createmarketplace.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import pl.makoto.createmarketplace.MarketConfig;
import pl.makoto.createmarketplace.api.MarketApi;
import pl.makoto.createmarketplace.data.MarketDatabase;
import pl.makoto.createmarketplace.data.MarketOffer;
import pl.makoto.createmarketplace.data.StockInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Odświeża stan magazynowy ofert.
 *
 * <p><b>Skanujemy wyłącznie oferty w załadowanych chunkach.</b> {@code getBlockEntity}
 * na {@code ServerLevel} doładowuje chunk synchronicznie, więc pełny skan rozrzuconych
 * po mapie sklepów zablokowałby serwer na tyle ticków, ile jest ofert. Oferta w
 * wyładowanym chunku zachowuje ostatni znany odczyt wraz z jego wiekiem — GUI pokaże
 * "sprzed X", nigdy fałszywego "pusty".
 */
public final class StockChecker {

    private StockChecker() {}

    /**
     * Przechodzi po wszystkich ofertach i odświeża te, które da się sprawdzić.
     *
     * @return true, jeśli którykolwiek stan się zmienił (warto rozesłać aktualizację)
     */
    public static boolean scan(MinecraftServer server) {
        MarketDatabase db = MarketDatabase.get(server);
        List<MarketOffer> dead = new ArrayList<>();
        boolean changed = false;

        for (MarketOffer offer : db.getOffers()) {
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, offer.dimension()));
            // brak wymiaru (usunięty datapack) albo chunk poza pamięcią → zostaw ostatni odczyt
            if (level == null || !level.isLoaded(offer.pos())) continue;

            BlockEntity be = level.getBlockEntity(offer.pos());
            if (be == null) {
                // Chunk jest załadowany, a bloku nie ma — sklep zniknął (TNT, creative,
                // WorldEdit). playerWillDestroy łapie tylko ręczne rozbicie.
                dead.add(offer);
                changed |= db.setStock(offer.pos(), StockInfo.of(0));
                continue;
            }

            OptionalInt stock = MarketApi.resolveStock(be, level, offer.pos());
            changed |= db.setStock(offer.pos(),
                    stock.isPresent() ? StockInfo.of(stock.getAsInt()) : StockInfo.NONE);
        }

        if (!dead.isEmpty() && MarketConfig.REMOVE_DEAD_OFFERS.get()) {
            for (MarketOffer offer : dead) {
                db.removeOffer(offer.pos());
            }
            changed = true;
        }
        return changed;
    }

    /**
     * Odświeża stan pojedynczej oferty — używane przy publikacji, żeby nowy sklep
     * od razu miał znacznik zamiast czekać na najbliższy skan cykliczny.
     */
    public static void refreshOne(Level level, MarketOffer offer) {
        if (!(level instanceof ServerLevel serverLevel) || !serverLevel.isLoaded(offer.pos())) return;
        BlockEntity be = serverLevel.getBlockEntity(offer.pos());
        if (be == null || serverLevel.getServer() == null) return;
        OptionalInt stock = MarketApi.resolveStock(be, serverLevel, offer.pos());
        MarketDatabase.get(serverLevel.getServer()).setStock(offer.pos(),
                stock.isPresent() ? StockInfo.of(stock.getAsInt()) : StockInfo.NONE);
    }
}
