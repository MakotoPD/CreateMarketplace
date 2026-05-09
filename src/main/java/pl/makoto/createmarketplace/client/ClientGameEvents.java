package pl.makoto.createmarketplace.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import pl.makoto.createmarketplace.CreateMarketplace;

@EventBusSubscriber(modid = CreateMarketplace.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
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
}
