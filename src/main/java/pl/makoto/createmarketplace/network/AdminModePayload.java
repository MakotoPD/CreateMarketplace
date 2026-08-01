package pl.makoto.createmarketplace.network;

import net.minecraft.network.FriendlyByteBuf;


/** Serwer -> klient: stan trybu administratora gracza. */
public record AdminModePayload(boolean active) {

    public AdminModePayload(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(active);
    }
}
