package pl.makoto.createmarketplace.client;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import pl.makoto.createmarketplace.CreateMarketplace;

@Mod.EventBusSubscriber(modid = CreateMarketplace.MODID, value = Dist.CLIENT)
public class ClientGameEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            while (ClientEvents.OPEN_MARKET_KEY.consumeClick()) {
                if (mc.screen == null) {
                    pl.makoto.createmarketplace.network.MarketNetwork.toServer(new pl.makoto.createmarketplace.network.RequestMarketRefreshPayload());
                    mc.setScreen(new GlobalMarketScreen(pl.makoto.createmarketplace.network.ClientPayloadHandler.getCachedOffers()));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        // admin mode resetuje się serwerowo przy wylogowaniu — czyścimy też cień klienta
        ClientAdminState.set(false);
    }

    @SubscribeEvent
    public static void onScreenInit(net.minecraftforge.client.event.ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen) {
            LOGGER.debug("Screen init: {}", screen.getClass().getSimpleName());

            // Dynamiczne pozycjonowanie na podstawie obecności EMI i konfiguracji
            int x, y;

            if (net.minecraftforge.fml.ModList.get().isLoaded("emi")) {
                // Gdy EMI jest obecne, trzymamy się jego przycisków (zachowanie natywne dla EMI)
                x = 2 + pl.makoto.createmarketplace.client.integration.MarketEmiCompat.getEmiOffset();
                y = screen.height - 22;
            } else {
                // Gdy nie ma EMI, używamy nowej konfiguracji użytkownika
                pl.makoto.createmarketplace.MarketConfig.ButtonPosition pos = pl.makoto.createmarketplace.MarketConfig.BUTTON_POSITION.get();
                switch (pos) {
                    case INVENTORY_SIDE -> {
                        x = screen.getGuiLeft() - 22;
                        y = screen.getGuiTop() + 10;
                    }
                    case CUSTOM -> {
                        x = pl.makoto.createmarketplace.MarketConfig.CUSTOM_BUTTON_X.get();
                        y = pl.makoto.createmarketplace.MarketConfig.CUSTOM_BUTTON_Y.get();
                    }
                    default -> { // CORNER (domyślnie lewy dolny róg)
                        x = 2;
                        y = screen.height - 22;
                    }
                }
            }

            pl.makoto.createmarketplace.client.integration.MarketButton marketButton = new pl.makoto.createmarketplace.client.integration.MarketButton(x, y, b -> {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.player != null) {
                    pl.makoto.createmarketplace.network.MarketNetwork.toServer(new pl.makoto.createmarketplace.network.RequestMarketRefreshPayload());
                    mc.setScreen(new pl.makoto.createmarketplace.client.GlobalMarketScreen(pl.makoto.createmarketplace.network.ClientPayloadHandler.getCachedOffers()));
                }
            });

            marketButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(net.minecraft.network.chat.Component.translatable("gui.create_marketplace.global_market.title")));
            
            // W NeoForge dodajemy do obu list, aby mieć pewność, że przycisk jest widoczny i interaktywny
            event.addListener(marketButton);
        }
    }
}
