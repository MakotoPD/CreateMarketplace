package pl.makoto.createmarketplace.network;

import net.minecraft.network.FriendlyByteBuf;


import pl.makoto.createmarketplace.data.MarketOffer;

/** Klient -> serwer: publikacja oferty na Global Market. */
public record PublishShopPayload(MarketOffer offer) {

    public PublishShopPayload(FriendlyByteBuf buf) {
        this(MarketOffer.decode(buf));
    }

    public void encode(FriendlyByteBuf buf) {
        offer.encode(buf);
    }
}
