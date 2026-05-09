package pl.makoto.createmarketplace.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import pl.makoto.createmarketplace.CreateMarketplace;
import pl.makoto.createmarketplace.data.MarketOffer;

import java.util.ArrayList;
import java.util.List;

public record MarketUpdatePayload(List<MarketOffer> offers) implements CustomPacketPayload {
    public static final Type<MarketUpdatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateMarketplace.MODID, "market_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MarketUpdatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.offers().size());
                for (MarketOffer offer : payload.offers()) {
                    MarketOffer.STREAM_CODEC.encode(buf, offer);
                }
            },
            buf -> {
                int size = buf.readVarInt();
                List<MarketOffer> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    list.add(MarketOffer.STREAM_CODEC.decode(buf));
                }
                return new MarketUpdatePayload(list);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
