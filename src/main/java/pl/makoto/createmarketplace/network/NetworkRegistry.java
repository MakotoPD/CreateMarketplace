package pl.makoto.createmarketplace.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import pl.makoto.createmarketplace.CreateMarketplace;

@EventBusSubscriber(modid = CreateMarketplace.MODID, bus = Bus.MOD)
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
    }
}
