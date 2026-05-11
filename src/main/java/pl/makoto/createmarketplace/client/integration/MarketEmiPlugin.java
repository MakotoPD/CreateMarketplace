package pl.makoto.createmarketplace.client.integration;

import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public class MarketEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        // Dynamiczny obszar wykluczenia dla naszego przycisku w lewym dolnym rogu.
        // Dzięki temu EMI nie będzie renderować swoich elementów nad naszym przyciskiem.
        registry.addExclusionArea(AbstractContainerScreen.class, (screen, consumer) -> {
            int x = 2 + MarketEmiCompat.getEmiOffset();
            int y = screen.height - 22;
            consumer.accept(new Bounds(x, y, 20, 20));
        });
    }
}
