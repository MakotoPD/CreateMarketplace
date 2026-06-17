package pl.makoto.createmarketplace.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.lwjgl.glfw.GLFW;
import pl.makoto.createmarketplace.CreateMarketplace;
import pl.makoto.createmarketplace.registry.BlockEntityRegistry;
import pl.makoto.createmarketplace.registry.MenuRegistry;

@EventBusSubscriber(modid = CreateMarketplace.MODID, value = Dist.CLIENT)
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

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(MenuRegistry.SERVER_VENDOR_ADMIN.get(), ServerVendorAdminScreen::new);
    }

    @SubscribeEvent
    public static void onRegisterBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityRegistry.SERVER_VENDOR.get(), ServerVendorBlockRenderer::new);
    }
}
