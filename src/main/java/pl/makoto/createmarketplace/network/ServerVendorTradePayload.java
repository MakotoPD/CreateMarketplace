package pl.makoto.createmarketplace.network;

import net.minecraft.network.FriendlyByteBuf;


import net.minecraft.core.BlockPos;

/** Klient -> serwer: prosba o transakcje (BUY/SELL x quantity) w Server Vendor. */
public record ServerVendorTradePayload(BlockPos pos, boolean buying, int quantity) {

    public ServerVendorTradePayload(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readBoolean(), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(buying);
        buf.writeVarInt(quantity);
    }
}
