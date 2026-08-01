package pl.makoto.createmarketplace.network;

import net.minecraft.network.FriendlyByteBuf;


import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/** Serwer -> klient: otwarcie ekranu handlu Server Vendor. */
public record OpenServerVendorTradePayload(BlockPos pos, ItemStack tradeItem,
                 ItemStack buyPrice, ItemStack sellPrice,
                 boolean buyEnabled, boolean sellEnabled) {

    public OpenServerVendorTradePayload(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readItem(), buf.readItem(), buf.readItem(),
             buf.readBoolean(), buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeItem(tradeItem);
        buf.writeItem(buyPrice);
        buf.writeItem(sellPrice);
        buf.writeBoolean(buyEnabled);
        buf.writeBoolean(sellEnabled);
    }
}
