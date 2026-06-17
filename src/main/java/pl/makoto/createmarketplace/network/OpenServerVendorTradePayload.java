package pl.makoto.createmarketplace.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import pl.makoto.createmarketplace.CreateMarketplace;

/** Serwer → klient: snapshot dla otwarcia ekranu handlu Server Vendor. */
public record OpenServerVendorTradePayload(BlockPos pos, ItemStack tradeItem,
                                            ItemStack buyPrice, ItemStack sellPrice,
                                            boolean buyEnabled, boolean sellEnabled) implements CustomPacketPayload {

    public static final Type<OpenServerVendorTradePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateMarketplace.MODID, "open_server_vendor_trade"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenServerVendorTradePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                BlockPos.STREAM_CODEC.encode(buf, p.pos());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.tradeItem());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.buyPrice());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.sellPrice());
                buf.writeBoolean(p.buyEnabled());
                buf.writeBoolean(p.sellEnabled());
            },
            buf -> new OpenServerVendorTradePayload(
                    BlockPos.STREAM_CODEC.decode(buf),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                    buf.readBoolean(),
                    buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
