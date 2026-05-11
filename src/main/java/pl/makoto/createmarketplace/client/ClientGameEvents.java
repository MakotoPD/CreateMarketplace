package pl.makoto.createmarketplace.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import pl.makoto.createmarketplace.CreateMarketplace;

@EventBusSubscriber(modid = CreateMarketplace.MODID, value = Dist.CLIENT)
public class ClientGameEvents {
    @SubscribeEvent
    public static void onClientTick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            while (ClientEvents.OPEN_MARKET_KEY.consumeClick()) {
                if (mc.screen == null) {
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(new pl.makoto.createmarketplace.network.RequestMarketRefreshPayload());
                    mc.setScreen(new GlobalMarketScreen(java.util.Collections.emptyList()));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onScreenInit(net.neoforged.neoforge.client.event.ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen) {
            // Logowanie do konsoli
            System.out.println("[CreateMarketplace] Screen init: " + screen.getClass().getSimpleName());

            // Dynamiczne pozycjonowanie na podstawie obecności EMI i konfiguracji
            int x, y;

            if (net.neoforged.fml.ModList.get().isLoaded("emi")) {
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
                    net.neoforged.neoforge.network.PacketDistributor.sendToServer(new pl.makoto.createmarketplace.network.RequestMarketRefreshPayload());
                    mc.setScreen(new pl.makoto.createmarketplace.client.GlobalMarketScreen(java.util.Collections.emptyList()));
                }
            });

            marketButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(net.minecraft.network.chat.Component.translatable("gui.create_marketplace.global_market.title")));
            
            // W NeoForge dodajemy do obu list, aby mieć pewność, że przycisk jest widoczny i interaktywny
            event.addListener(marketButton);
        }
    }
}
