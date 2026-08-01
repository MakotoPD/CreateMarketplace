package pl.makoto.createmarketplace.network;

import net.minecraft.network.FriendlyByteBuf;


/** Serwer -> klient: wynik transakcji w Server Vendor. */
public record ServerVendorTradeResultPayload(boolean ok, String i18nKey, int units) {

    public ServerVendorTradeResultPayload(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readUtf(), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(ok);
        buf.writeUtf(i18nKey);
        buf.writeVarInt(units);
    }
}
