package pl.makoto.createmarketplace.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import pl.makoto.createmarketplace.CreateMarketplace;

@EventBusSubscriber(modid = CreateMarketplace.MODID, bus = Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    public static final KeyMapping OPEN_MARKET_KEY = new KeyMapping(
            "key.createmarketplace.open_market",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "key.categories.createmarketplace"
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MARKET_KEY);
    }
}
