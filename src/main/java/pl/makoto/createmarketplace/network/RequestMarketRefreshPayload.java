package pl.makoto.createmarketplace.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import pl.makoto.createmarketplace.CreateMarketplace;

public record RequestMarketRefreshPayload() implements CustomPacketPayload {
    public static final Type<RequestMarketRefreshPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateMarketplace.MODID, "request_market_refresh"));

    public static final StreamCodec<FriendlyByteBuf, RequestMarketRefreshPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {},
            buf -> new RequestMarketRefreshPayload()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
