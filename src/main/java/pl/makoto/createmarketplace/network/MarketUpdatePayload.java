package pl.makoto.createmarketplace.network;

import net.minecraft.network.FriendlyByteBuf;


import pl.makoto.createmarketplace.data.MarketDatabase;
import pl.makoto.createmarketplace.data.MarketOffer;
import pl.makoto.createmarketplace.data.StockInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Serwer -> klient: pelna lista ofert wraz ze stanem magazynowym kazdej z nich.
 *
 * <p>{@code stock} jest rownolegle do {@code offers} - ten sam rozmiar i kolejnosc.
 * Wiek odczytu jedzie jako liczba sekund, a nie znacznik czasu: klient odtwarza go
 * na wlasnym zegarze, wiec rozjechane zegary nie zafalszuja "sprzed X minut".
 */
public record MarketUpdatePayload(List<MarketOffer> offers, List<StockInfo> stock) {

    public MarketUpdatePayload(FriendlyByteBuf buf) {
        this(readOffers(buf), new ArrayList<>());
        long now = System.currentTimeMillis();
        for (int i = 0; i < offers.size(); i++) {
            int units = buf.readInt();
            stock.add(new StockInfo(units, now - buf.readVarInt() * 1000L));
        }
    }

    private static List<MarketOffer> readOffers(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<MarketOffer> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(MarketOffer.decode(buf));
        }
        return list;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(offers.size());
        for (MarketOffer offer : offers) {
            offer.encode(buf);
        }
        for (StockInfo info : stock) {
            buf.writeInt(info.units());
            buf.writeVarInt(info.ageSeconds());
        }
    }

    /** Buduje pakiet, dobierajac stany z bazy w kolejnosci ofert. */
    public static MarketUpdatePayload of(MarketDatabase db) {
        List<MarketOffer> offers = List.copyOf(db.getOffers());
        List<StockInfo> stock = new ArrayList<>(offers.size());
        for (MarketOffer offer : offers) {
            stock.add(db.getStock(offer.pos()));
        }
        return new MarketUpdatePayload(offers, stock);
    }
}
