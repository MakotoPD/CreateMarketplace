package pl.makoto.createmarketplace.network;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import pl.makoto.createmarketplace.client.GlobalMarketScreen;
import pl.makoto.createmarketplace.client.MyShopsScreen;
import pl.makoto.createmarketplace.client.ShopRegistrationScreen;
import pl.makoto.createmarketplace.data.MarketOffer;
import pl.makoto.createmarketplace.data.StockInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientPayloadHandler {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Cache for offers received while no market screen is open
    private static List<MarketOffer> cachedOffers = Collections.emptyList();
    private static Map<net.minecraft.core.BlockPos, StockInfo> cachedStock = Map.of();

    public static List<MarketOffer> getCachedOffers() {
        return cachedOffers;
    }

    /** Stan magazynowy oferty pod daną pozycją; {@link StockInfo#NONE} gdy nieznany. */
    public static StockInfo getStock(net.minecraft.core.BlockPos pos) {
        return cachedStock.getOrDefault(pos, StockInfo.NONE);
    }

    public static void handleMarketUpdate(final MarketUpdatePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            // zawsze trzymamy najnowsze oferty (do otwarcia/powrotu na ekran)
            cachedOffers = payload.offers();
            Map<net.minecraft.core.BlockPos, StockInfo> stock = new HashMap<>();
            for (int i = 0; i < payload.offers().size() && i < payload.stock().size(); i++) {
                stock.put(payload.offers().get(i).pos(), payload.stock().get(i));
            }
            cachedStock = stock;
            if (mc.screen instanceof GlobalMarketScreen marketScreen) {
                marketScreen.updateOffers(payload.offers());
            } else if (mc.screen instanceof MyShopsScreen myShops) {
                myShops.updateOffers(payload.offers());
            }
            LOGGER.debug("Client received market update. Offers: {}", payload.offers().size());
        });
    }

    public static void handleAdminMode(final AdminModePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            pl.makoto.createmarketplace.client.ClientAdminState.set(payload.active());
            // odśwież otwarte "Moje sklepy" — zmienia się filtr (własne ↔ serwerowe)
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof MyShopsScreen myShops) {
                myShops.updateOffers(cachedOffers);
            }
        });
    }

    public static void handleOpenRegistrationGui(final OpenRegistrationGuiPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(
                    new ShopRegistrationScreen(payload.pos(), payload.item(), payload.currency(), payload.existingShops())
            );
        });
    }

    public static void handleOpenServerVendorTrade(final OpenServerVendorTradePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft.getInstance().setScreen(new pl.makoto.createmarketplace.client.ServerVendorTradeScreen(
                    payload.pos(), payload.tradeItem(),
                    payload.buyPrice(), payload.sellPrice(),
                    payload.buyEnabled(), payload.sellEnabled()));
        });
    }

    public static void handleServerVendorTradeResult(final ServerVendorTradeResultPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof pl.makoto.createmarketplace.client.ServerVendorTradeScreen ts) {
                ts.onResult(payload.ok(), payload.i18nKey(), payload.units());
            } else if (mc.player != null) {
                mc.player.displayClientMessage(net.minecraft.network.chat.Component.translatable(payload.i18nKey(), payload.units()), true);
            }
        });
    }
}
