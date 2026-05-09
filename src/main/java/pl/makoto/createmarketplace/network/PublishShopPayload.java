package pl.makoto.createmarketplace.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import pl.makoto.createmarketplace.CreateMarketplace;
import pl.makoto.createmarketplace.data.MarketOffer;

public record PublishShopPayload(MarketOffer offer) implements CustomPacketPayload {
    public static final Type<PublishShopPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateMarketplace.MODID, "publish_shop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PublishShopPayload> STREAM_CODEC = StreamCodec.composite(
            MarketOffer.STREAM_CODEC, PublishShopPayload::offer,
            PublishShopPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
