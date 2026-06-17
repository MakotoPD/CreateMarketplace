package pl.makoto.createmarketplace.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import pl.makoto.createmarketplace.CreateMarketplace;

/** Klient → serwer: prośba o transakcję (BUY/SELL × quantity) w Server Vendor. */
public record ServerVendorTradePayload(BlockPos pos, boolean buying, int quantity) implements CustomPacketPayload {

    public static final Type<ServerVendorTradePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateMarketplace.MODID, "server_vendor_trade"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ServerVendorTradePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                BlockPos.STREAM_CODEC.encode(buf, p.pos());
                buf.writeBoolean(p.buying());
                buf.writeVarInt(p.quantity());
            },
            buf -> new ServerVendorTradePayload(
                    BlockPos.STREAM_CODEC.decode(buf),
                    buf.readBoolean(),
                    buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
