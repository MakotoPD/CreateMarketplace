package pl.makoto.createmarketplace.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import pl.makoto.createmarketplace.CreateMarketplace;

import java.util.List;

public record OpenRegistrationGuiPayload(BlockPos pos, ItemStack item, ItemStack currency, List<String> existingShops) implements CustomPacketPayload {
    public static final Type<OpenRegistrationGuiPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateMarketplace.MODID, "open_registration_gui"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenRegistrationGuiPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                BlockPos.STREAM_CODEC.encode(buf, payload.pos());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, payload.item());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, payload.currency());
                buf.writeVarInt(payload.existingShops().size());
                for (String s : payload.existingShops()) {
                    buf.writeUtf(s);
                }
            },
            buf -> {
                BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
                ItemStack item = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                ItemStack currency = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                int size = buf.readVarInt();
                java.util.List<String> shops = new java.util.ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    shops.add(buf.readUtf());
                }
                return new OpenRegistrationGuiPayload(pos, item, currency, shops);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
