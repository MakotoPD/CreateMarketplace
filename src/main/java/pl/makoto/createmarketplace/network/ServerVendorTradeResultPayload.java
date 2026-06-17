package pl.makoto.createmarketplace.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import pl.makoto.createmarketplace.CreateMarketplace;

/** Serwer → klient: rezultat transakcji Server Vendor (OK/błąd + klucz i18n). */
public record ServerVendorTradeResultPayload(boolean ok, String i18nKey, int units) implements CustomPacketPayload {

    public static final Type<ServerVendorTradeResultPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateMarketplace.MODID, "server_vendor_trade_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerVendorTradeResultPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.ok());
                buf.writeUtf(p.i18nKey());
                buf.writeVarInt(p.units());
            },
            buf -> new ServerVendorTradeResultPayload(
                    buf.readBoolean(),
                    buf.readUtf(),
                    buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
