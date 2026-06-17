package pl.makoto.createmarketplace.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import pl.makoto.createmarketplace.CreateMarketplace;

@EventBusSubscriber(modid = CreateMarketplace.MODID)
public class NetworkRegistry {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(CreateMarketplace.MODID);

        // Pakiety C2S (Od klienta do serwera)
        registrar.playToServer(
                PublishShopPayload.TYPE,
                PublishShopPayload.STREAM_CODEC,
                ServerPayloadHandler::handlePublishShop
        );

        registrar.playToServer(
                RequestMarketRefreshPayload.TYPE,
                RequestMarketRefreshPayload.STREAM_CODEC,
                ServerPayloadHandler::handleRefreshRequest
        );

        registrar.playToServer(
                DeleteShopPayload.TYPE,
                DeleteShopPayload.STREAM_CODEC,
                ServerPayloadHandler::handleDeleteShop
        );

        registrar.playToServer(
                SaveServerVendorPayload.TYPE,
                SaveServerVendorPayload.STREAM_CODEC,
                ServerPayloadHandler::handleSaveServerVendor
        );

        registrar.playToServer(
                ServerVendorTradePayload.TYPE,
                ServerVendorTradePayload.STREAM_CODEC,
                ServerPayloadHandler::handleServerVendorTrade
        );

        // Pakiety S2C (Od serwera do klienta) - rejestrowane na obu stronach dla kompatybilności kanałów,
        // ale logika handlera jest wywoływana tylko na kliencie.
        registrar.playToClient(
                MarketUpdatePayload.TYPE,
                MarketUpdatePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
                        ClientPayloadHandler.handleMarketUpdate(payload, context);
                    }
                }
        );

        registrar.playToClient(
                OpenRegistrationGuiPayload.TYPE,
                OpenRegistrationGuiPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
                        ClientPayloadHandler.handleOpenRegistrationGui(payload, context);
                    }
                }
        );

        registrar.playToClient(
                AdminModePayload.TYPE,
                AdminModePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
                        ClientPayloadHandler.handleAdminMode(payload, context);
                    }
                }
        );

        registrar.playToClient(
                OpenServerVendorTradePayload.TYPE,
                OpenServerVendorTradePayload.STREAM_CODEC,
                (payload, context) -> {
                    if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
                        ClientPayloadHandler.handleOpenServerVendorTrade(payload, context);
                    }
                }
        );

        registrar.playToClient(
                ServerVendorTradeResultPayload.TYPE,
                ServerVendorTradeResultPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
                        ClientPayloadHandler.handleServerVendorTradeResult(payload, context);
                    }
                }
        );
    }
}
