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
            .icon(() -> new net.minecraft.world.item.ItemStack(ItemRegistry.REGISTRATION_BOOK.get()))
            .displayItems((parameters, output) -> {
                output.accept(ItemRegistry.REGISTRATION_BOOK.get());
                output.accept(ItemRegistry.DEBUG_PAPER.get());
                output.accept(ItemRegistry.SERVER_VENDOR_ITEM.get());
            })
            .build());

    public CreateMarketplace(IEventBus modEventBus, net.neoforged.fml.ModContainer modContainer) {
        LOGGER.info(">>> Create: Marketplace is initializing...");

        try {
            pl.makoto.createmarketplace.registry.BlockRegistry.BLOCKS.register(modEventBus);
            pl.makoto.createmarketplace.registry.BlockEntityRegistry.BLOCK_ENTITIES.register(modEventBus);
            pl.makoto.createmarketplace.registry.MenuRegistry.MENU_TYPES.register(modEventBus);
            ItemRegistry.ITEMS.register(modEventBus);
            CREATIVE_MODE_TABS.register(modEventBus);

            modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, MarketConfig.COMMON_SPEC);
            modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, MarketConfig.CLIENT_SPEC);

            // Rejestracja domyślnych handlerów API
            pl.makoto.createmarketplace.api.MarketApi.registerHandler(new pl.makoto.createmarketplace.api.impl.NumismaticsShopHandler());
            pl.makoto.createmarketplace.api.MarketApi.registerHandler(new pl.makoto.createmarketplace.api.impl.ServerVendorShopHandler());
            
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
