package pl.makoto.createmarketplace.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraft.server.MinecraftServer;
import pl.makoto.createmarketplace.data.MarketDatabase;
import pl.makoto.createmarketplace.data.MarketOffer;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Główna klasa API dla Create: Marketplace.
 * Pozwala na rejestrację handlerów wspierających różne typy sklepów oraz dostęp do bazy ofert.
 */
public class MarketApi {
    private static final List<IShopHandler> HANDLERS = new CopyOnWriteArrayList<>();

    /**
     * Rejestruje nowy handler sklepu. 
     * Wywołaj to podczas FMLCommonSetupEvent lub w konstruktorze moda.
     * 
     * @param handler Implementacja IShopHandler rozpoznająca nowe bloki.
     */
    public static void registerHandler(IShopHandler handler) {
        if (!HANDLERS.contains(handler)) {
            HANDLERS.add(handler);
        }
    }

    /**
     * Pobiera listę wszystkich zarejestrowanych ofert rynkowych (tylko do odczytu).
     * 
     * @param server Instancja serwera Minecraft.
     * @return Niemodyfikowalna lista wszystkich ofert w bazie.
     */
    public static List<MarketOffer> getOffers(MinecraftServer server) {
        return MarketDatabase.get(server).getOffers();
    }

    /**
     * Próbuje znaleźć dane sklepu dla danego BlockEntity, przeszukując zarejestrowane handlery.
     * 
     * @param be Blok encji do sprawdzenia.
     * @param level Świat, w którym znajduje się blok.
     * @param pos Współrzędne bloku.
     * @return Dane sklepu (przedmiot i cena), jeśli jakikolwiek handler je rozpoznał.
     */
    public static Optional<ShopResult> resolveShop(BlockEntity be, Level level, BlockPos pos) {
        for (IShopHandler handler : HANDLERS) {
            Optional<ShopResult> result = handler.tryResolve(be, level, pos);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    /**
     * Pyta zarejestrowane handlery o stan magazynowy sklepu. Pierwszy, który
     * potrafi odpowiedzieć, wygrywa.
     *
     * @return liczba możliwych transakcji albo pusty {@code OptionalInt}, gdy
     *         żaden handler nie potrafił tego ustalić
     * @see IShopHandler#getStock(BlockEntity, Level, BlockPos)
     */
    public static OptionalInt resolveStock(BlockEntity be, Level level, BlockPos pos) {
        for (IShopHandler handler : HANDLERS) {
            OptionalInt stock = handler.getStock(be, level, pos);
            if (stock.isPresent()) {
                return stock;
            }
        }
        return OptionalInt.empty();
    }
}
