package pl.makoto.createmarketplace.network;

import net.minecraft.network.FriendlyByteBuf;


import net.minecraft.core.BlockPos;

import java.util.Optional;

/** Klient -> serwer: usuniecie pojedynczej oferty (po pozycji) albo calego sklepu (po nazwie). */
public record DeleteShopPayload(Optional<BlockPos> posToRemove, Optional<String> shopNameToRemove) {

    public DeleteShopPayload(FriendlyByteBuf buf) {
        this(buf.readBoolean() ? Optional.of(buf.readBlockPos()) : Optional.empty(),
             buf.readBoolean() ? Optional.of(buf.readUtf()) : Optional.empty());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(posToRemove.isPresent());
        posToRemove.ifPresent(buf::writeBlockPos);
        buf.writeBoolean(shopNameToRemove.isPresent());
        shopNameToRemove.ifPresent(buf::writeUtf);
    }
}
