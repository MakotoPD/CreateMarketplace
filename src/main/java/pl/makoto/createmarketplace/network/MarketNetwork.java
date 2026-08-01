package pl.makoto.createmarketplace.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import pl.makoto.createmarketplace.CreateMarketplace;

/**
 * Kanał sieciowy moda.
 *
 * <p>Na 1.20.1 nie ma {@code CustomPacketPayload} ani {@code StreamCodec} — pakiety
 * idą przez {@link SimpleChannel} z ręcznym encode/decode. Klasa nazywa się
 * {@code MarketNetwork}, a nie {@code NetworkRegistry}, żeby nie kolidowała
 * z {@link net.minecraftforge.network.NetworkRegistry}.
 *
 * <p>Obsługa pakietów klienckich idzie przez {@link DistExecutor}, a nie przez zwykłe
 * wywołanie — inaczej dedykowany serwer ładowałby klasy klienckie i wywalałby się
 * przy starcie.
 */
public final class MarketNetwork {

    private MarketNetwork() {}

    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(CreateMarketplace.MODID, "main"))
            .networkProtocolVersion(() -> VERSION)
            .clientAcceptedVersions(VERSION::equals)
            .serverAcceptedVersions(VERSION::equals)
            .simpleChannel();

    private static int nextId = 0;

    public static void register() {
        // --- klient -> serwer ---
        CHANNEL.messageBuilder(PublishShopPayload.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(PublishShopPayload::encode)
                .decoder(PublishShopPayload::new)
                .consumerMainThread((msg, ctx) -> {
                    ServerPayloadHandler.handlePublishShop(msg, ctx.get());
                    ctx.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(RequestMarketRefreshPayload.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestMarketRefreshPayload::encode)
                .decoder(RequestMarketRefreshPayload::new)
                .consumerMainThread((msg, ctx) -> {
                    ServerPayloadHandler.handleRefreshRequest(msg, ctx.get());
                    ctx.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(DeleteShopPayload.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeleteShopPayload::encode)
                .decoder(DeleteShopPayload::new)
                .consumerMainThread((msg, ctx) -> {
                    ServerPayloadHandler.handleDeleteShop(msg, ctx.get());
                    ctx.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(SaveServerVendorPayload.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SaveServerVendorPayload::encode)
                .decoder(SaveServerVendorPayload::new)
                .consumerMainThread((msg, ctx) -> {
                    ServerPayloadHandler.handleSaveServerVendor(msg, ctx.get());
                    ctx.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(ServerVendorTradePayload.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ServerVendorTradePayload::encode)
                .decoder(ServerVendorTradePayload::new)
                .consumerMainThread((msg, ctx) -> {
                    ServerPayloadHandler.handleServerVendorTrade(msg, ctx.get());
                    ctx.get().setPacketHandled(true);
                })
                .add();

        // --- serwer -> klient ---
        CHANNEL.messageBuilder(MarketUpdatePayload.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(MarketUpdatePayload::encode)
                .decoder(MarketUpdatePayload::new)
                .consumerMainThread((msg, ctx) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> ClientPayloadHandler.handleMarketUpdate(msg));
                    ctx.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(OpenRegistrationGuiPayload.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenRegistrationGuiPayload::encode)
                .decoder(OpenRegistrationGuiPayload::new)
                .consumerMainThread((msg, ctx) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> ClientPayloadHandler.handleOpenRegistrationGui(msg));
                    ctx.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(AdminModePayload.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AdminModePayload::encode)
                .decoder(AdminModePayload::new)
                .consumerMainThread((msg, ctx) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> ClientPayloadHandler.handleAdminMode(msg));
                    ctx.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(OpenServerVendorTradePayload.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenServerVendorTradePayload::encode)
                .decoder(OpenServerVendorTradePayload::new)
                .consumerMainThread((msg, ctx) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> ClientPayloadHandler.handleOpenServerVendorTrade(msg));
                    ctx.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(ServerVendorTradeResultPayload.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ServerVendorTradeResultPayload::encode)
                .decoder(ServerVendorTradeResultPayload::new)
                .consumerMainThread((msg, ctx) -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                            () -> () -> ClientPayloadHandler.handleServerVendorTradeResult(msg));
                    ctx.get().setPacketHandled(true);
                })
                .add();
    }

    // --- skróty wysyłkowe (odpowiedniki PacketDistributor.sendToX z NeoForge) ---

    public static void toServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }

    public static void toPlayer(ServerPlayer player, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void toAll(Object msg) {
        CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
    }
}
