package pl.makoto.createmarketplace.client.integration;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import pl.makoto.createmarketplace.CreateMarketplace;

import java.util.List;

@JeiPlugin
public class MarketJeiPlugin implements IModPlugin {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreateMarketplace.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // Rejestrujemy obszar przycisku jako "extra area", aby JEI go nie zasłaniało
        registration.addGuiContainerHandler(InventoryScreen.class, new IGuiContainerHandler<InventoryScreen>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(InventoryScreen containerScreen) {
                // Musi odpowiadać pozycji w ClientGameEvents
                return List.of(new Rect2i(containerScreen.getGuiLeft() - 22, containerScreen.getGuiTop() + 10, 20, 20));
            }
        });
    }
}
