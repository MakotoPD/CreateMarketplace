package pl.makoto.createmarketplace;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import pl.makoto.createmarketplace.registry.ItemRegistry;

import java.util.function.Supplier;

@Mod(CreateMarketplace.MODID)
public class CreateMarketplace {
    public static final String MODID = "create_marketplace";
    
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, MODID);
    
    public static final Supplier<CreativeModeTab> MARKETPLACE_TAB = CREATIVE_MODE_TABS.register("marketplace_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.create_marketplace"))
            .icon(() -> new net.minecraft.world.item.ItemStack(ItemRegistry.REGISTRATION_CARD.get()))
            .displayItems((parameters, output) -> {
                output.accept(ItemRegistry.REGISTRATION_CARD.get());
            })
            .build());

    public CreateMarketplace(IEventBus modEventBus) {
        ItemRegistry.ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
