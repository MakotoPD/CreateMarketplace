package pl.makoto.createmarketplace.network;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import pl.makoto.createmarketplace.client.GlobalMarketScreen;
import pl.makoto.createmarketplace.client.ShopRegistrationScreen;
import pl.makoto.createmarketplace.data.MarketOffer;

import java.util.Collections;
import java.util.List;

public class ClientPayloadHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Cache for offers received while no market screen is open
    private static List<MarketOffer> cachedOffers = Collections.emptyList();

    public static List<MarketOffer> getCachedOffers() {
        return cachedOffers;
    }

    public static void handleMarketUpdate(final MarketUpdatePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof GlobalMarketScreen marketScreen) {
                marketScreen.updateOffers(payload.offers());
            } else {
                // Cache for later — do NOT open or change the current screen
                cachedOffers = payload.offers();
            }
            LOGGER.debug("Client received market update. Offers: {}", payload.offers().size());
        });
    }

    public static void handleOpenRegistrationGui(final OpenRegistrationGuiPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(
                    new ShopRegistrationScreen(payload.pos(), payload.item(), payload.currency(), payload.existingShops())
            );
        });
    }
}
