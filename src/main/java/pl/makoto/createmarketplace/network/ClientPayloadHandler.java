package pl.makoto.createmarketplace.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {

    public static void handleMarketUpdate(final MarketUpdatePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.screen instanceof pl.makoto.createmarketplace.client.GlobalMarketScreen marketScreen) {
                marketScreen.updateOffers(payload.offers());
            } else {
                mc.setScreen(new pl.makoto.createmarketplace.client.GlobalMarketScreen(payload.offers()));
            }
            System.out.println("Klient zaktualizował widok rynku. Ofert: " + payload.offers().size());
        });
    }

    public static void handleOpenRegistrationGui(final OpenRegistrationGuiPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            net.minecraft.client.Minecraft.getInstance().setScreen(
                    new pl.makoto.createmarketplace.client.ShopRegistrationScreen(payload.pos(), payload.item(), payload.currency(), payload.existingShops())
            );
        });
    }
}
