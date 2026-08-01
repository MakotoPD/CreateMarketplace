package pl.makoto.createmarketplace.data;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public record MarketOffer(UUID ownerId, String ownerName, String shopName, BlockPos pos,
                          ResourceLocation dimension, ItemStack item, ItemStack currency, long timestamp) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(ownerId);
        buf.writeUtf(ownerName);
        buf.writeUtf(shopName);
        buf.writeBlockPos(pos);
        buf.writeResourceLocation(dimension);
        // writeItem nie przyjmuje null-a, a oferta bez przedmiotu jest dozwolona
        buf.writeItem(item == null ? ItemStack.EMPTY : item);
        buf.writeItem(currency == null ? ItemStack.EMPTY : currency);
        buf.writeLong(timestamp);
    }

    public static MarketOffer decode(FriendlyByteBuf buf) {
        return new MarketOffer(
                buf.readUUID(),
                buf.readUtf(),
                buf.readUtf(),
                buf.readBlockPos(),
                buf.readResourceLocation(),
                buf.readItem(),
                buf.readItem(),
                buf.readLong());
    }
}
