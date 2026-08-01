package pl.makoto.createmarketplace.network;

import net.minecraft.network.FriendlyByteBuf;


import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Serwer -> klient: otwarcie ekranu rejestracji sklepu, z podpowiedziami istniejacych nazw. */
public record OpenRegistrationGuiPayload(BlockPos pos, ItemStack item, ItemStack currency,
                                         List<String> existingShops) {

    public OpenRegistrationGuiPayload(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readItem(), buf.readItem(), readShops(buf));
    }

    private static List<String> readShops(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> shops = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            shops.add(buf.readUtf());
        }
        return shops;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeItem(item);
        buf.writeItem(currency);
        buf.writeVarInt(existingShops.size());
        for (String s : existingShops) {
            buf.writeUtf(s);
        }
    }
}
