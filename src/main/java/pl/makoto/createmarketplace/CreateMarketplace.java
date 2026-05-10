package pl.makoto.createmarketplace;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import pl.makoto.createmarketplace.registry.ItemRegistry;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.function.Supplier;

@Mod(CreateMarketplace.MODID)
public class CreateMarketplace {
    public static final String MODID = "create_marketplace";
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, MODID);
    
    public static final Supplier<CreativeModeTab> MARKETPLACE_TAB = CREATIVE_MODE_TABS.register("marketplace_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.create_marketplace"))
            .icon(() -> new net.minecraft.world.item.ItemStack(ItemRegistry.REGISTRATION_CARD.get()))
            .displayItems((parameters, output) -> {
                output.accept(ItemRegistry.REGISTRATION_CARD.get());
                output.accept(ItemRegistry.DEBUG_PAPER.get());
            })
            .build());

    public CreateMarketplace(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        LOGGER.info(">>> Create: Marketplace is initializing...");
        
        try {
            ItemRegistry.ITEMS.register(modEventBus);
            CREATIVE_MODE_TABS.register(modEventBus);
            
            modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, MarketConfig.COMMON_SPEC);
            
            modEventBus.addListener(this::commonSetup);
            
            LOGGER.info(">>> Create: Marketplace core components registered.");
        } catch (Exception e) {
            LOGGER.error("!!! Create: Marketplace failed to initialize core components!", e);
            throw e; // Rzucamy dalej, żeby FML wiedział o błędzie
        }
    }

    private void commonSetup(final net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
        LOGGER.info(">>> Create: Marketplace common setup completed.");
    }
}
