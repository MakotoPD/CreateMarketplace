package pl.makoto.createmarketplace.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import pl.makoto.createmarketplace.CreateMarketplace;

/** serwer → klient: aktualny stan trybu administratora gracza */
public record AdminModePayload(boolean active) implements CustomPacketPayload {
    public static final Type<AdminModePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateMarketplace.MODID, "admin_mode"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AdminModePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, AdminModePayload::active, AdminModePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
