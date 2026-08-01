package pl.makoto.createmarketplace.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;
import pl.makoto.createmarketplace.CreateMarketplace;
import pl.makoto.createmarketplace.registry.BlockEntityRegistry;
import pl.makoto.createmarketplace.registry.MenuRegistry;

@Mod.EventBusSubscriber(modid = CreateMarketplace.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
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
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Na 1.20.1 nie ma RegisterMenuScreensEvent - ekrany menu rejestruje sie w client setupie.
        event.enqueueWork(() ->
                MenuScreens.register(MenuRegistry.SERVER_VENDOR_ADMIN.get(), ServerVendorAdminScreen::new));
    }

    @SubscribeEvent
    public static void onRegisterBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityRegistry.SERVER_VENDOR.get(), ServerVendorBlockRenderer::new);
    }
}
