package pl.makoto.createmarketplace.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public record MarketOffer(UUID ownerId, String ownerName, String shopName, BlockPos pos, ItemStack item, ItemStack currency, long timestamp) {
    public static final StreamCodec<RegistryFriendlyByteBuf, MarketOffer> STREAM_CODEC = StreamCodec.of(
        (buf, offer) -> {
            UUIDUtil.STREAM_CODEC.encode(buf, offer.ownerId());
            ByteBufCodecs.STRING_UTF8.encode(buf, offer.ownerName());
            ByteBufCodecs.STRING_UTF8.encode(buf, offer.shopName());
            BlockPos.STREAM_CODEC.encode(buf, offer.pos());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, offer.item());
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, offer.currency());
            buf.writeLong(offer.timestamp());
        },
        buf -> new MarketOffer(
            UUIDUtil.STREAM_CODEC.decode(buf),
            ByteBufCodecs.STRING_UTF8.decode(buf),
            ByteBufCodecs.STRING_UTF8.decode(buf),
            BlockPos.STREAM_CODEC.decode(buf),
            ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
            ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
            buf.readLong()
        )
    );
}
