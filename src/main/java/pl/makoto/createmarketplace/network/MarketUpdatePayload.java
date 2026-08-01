package pl.makoto.createmarketplace.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import pl.makoto.createmarketplace.CreateMarketplace;
import pl.makoto.createmarketplace.data.MarketDatabase;
import pl.makoto.createmarketplace.data.MarketOffer;
import pl.makoto.createmarketplace.data.StockInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Pełna lista ofert wraz ze stanem magazynowym każdej z nich.
 *
 * <p>{@code stock} jest równoległe do {@code offers} — ten sam rozmiar i kolejność.
 * Wiek odczytu jedzie jako liczba sekund, a nie znacznik czasu: klient odtwarza go
 * na własnym zegarze, więc rozjechane zegary nie zafałszują "sprzed X minut".
 */
public record MarketUpdatePayload(List<MarketOffer> offers, List<StockInfo> stock) implements CustomPacketPayload {
    public static final Type<MarketUpdatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateMarketplace.MODID, "market_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MarketUpdatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.offers().size());
                for (MarketOffer offer : payload.offers()) {
                    MarketOffer.STREAM_CODEC.encode(buf, offer);
                }
                for (StockInfo info : payload.stock()) {
                    buf.writeInt(info.units());
                    buf.writeVarInt(info.ageSeconds());
                }
            },
            buf -> {
                int size = buf.readVarInt();
                List<MarketOffer> offers = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    offers.add(MarketOffer.STREAM_CODEC.decode(buf));
                }
                long now = System.currentTimeMillis();
                List<StockInfo> stock = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    int units = buf.readInt();
                    long checkedAt = now - buf.readVarInt() * 1000L;
                    stock.add(new StockInfo(units, checkedAt));
                }
                return new MarketUpdatePayload(offers, stock);
            }
    );

    /** Buduje pakiet, dobierając stany z bazy w kolejności ofert. */
    public static MarketUpdatePayload of(MarketDatabase db) {
        List<MarketOffer> offers = List.copyOf(db.getOffers());
        List<StockInfo> stock = new ArrayList<>(offers.size());
        for (MarketOffer offer : offers) {
            stock.add(db.getStock(offer.pos()));
        }
        return new MarketUpdatePayload(offers, stock);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
