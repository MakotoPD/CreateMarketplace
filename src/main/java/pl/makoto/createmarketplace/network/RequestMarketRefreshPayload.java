package pl.makoto.createmarketplace.network;

import net.minecraft.network.FriendlyByteBuf;


/** Klient -> serwer: prosba o aktualna liste ofert. Bez pol. */
public record RequestMarketRefreshPayload() {

    public RequestMarketRefreshPayload(FriendlyByteBuf buf) {
        this();
    }

    public void encode(FriendlyByteBuf buf) {
    }
}
