package pl.makoto.createmarketplace.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import pl.makoto.createmarketplace.CreateMarketplace;

import java.util.Optional;

public record DeleteShopPayload(Optional<BlockPos> posToRemove, Optional<String> shopNameToRemove) implements CustomPacketPayload {
    public static final Type<DeleteShopPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(CreateMarketplace.MODID, "delete_shop"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteShopPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBoolean(payload.posToRemove().isPresent());
                if (payload.posToRemove().isPresent()) {
                    BlockPos.STREAM_CODEC.encode(buf, payload.posToRemove().get());
                }
                
                buf.writeBoolean(payload.shopNameToRemove().isPresent());
                if (payload.shopNameToRemove().isPresent()) {
                    buf.writeUtf(payload.shopNameToRemove().get());
                }
            },
            buf -> {
                boolean hasPos = buf.readBoolean();
                Optional<BlockPos> pos = hasPos ? Optional.of(BlockPos.STREAM_CODEC.decode(buf)) : Optional.empty();
                
                boolean hasName = buf.readBoolean();
                Optional<String> name = hasName ? Optional.of(buf.readUtf()) : Optional.empty();
                
                return new DeleteShopPayload(pos, name);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
