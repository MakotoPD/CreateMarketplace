package pl.makoto.createmarketplace.network;

import net.minecraft.network.FriendlyByteBuf;


import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/** Klient -> serwer: zapis konfiguracji Server Vendor (tylko admin). */
public record SaveServerVendorPayload(BlockPos pos, ItemStack tradeItem,
                 ItemStack buyPrice, ItemStack sellPrice,
                 boolean buyEnabled, boolean sellEnabled) {

    public SaveServerVendorPayload(FriendlyByteBuf buf) {
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
