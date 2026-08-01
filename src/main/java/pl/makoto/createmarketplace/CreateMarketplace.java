package pl.makoto.createmarketplace;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import pl.makoto.createmarketplace.api.MarketApi;
import pl.makoto.createmarketplace.api.impl.NumismaticsShopHandler;
import pl.makoto.createmarketplace.api.impl.ServerVendorShopHandler;
import pl.makoto.createmarketplace.network.MarketNetwork;
import pl.makoto.createmarketplace.registry.BlockEntityRegistry;
import pl.makoto.createmarketplace.registry.BlockRegistry;
import pl.makoto.createmarketplace.registry.ItemRegistry;
import pl.makoto.createmarketplace.registry.MenuRegistry;

@Mod(CreateMarketplace.MODID)
public class CreateMarketplace {
    public static final String MODID = "create_marketplace";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<CreativeModeTab> MARKETPLACE_TAB = CREATIVE_MODE_TABS.register(
            "marketplace_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.create_marketplace"))
                    .icon(() -> new ItemStack(ItemRegistry.REGISTRATION_BOOK.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ItemRegistry.REGISTRATION_BOOK.get());
                        output.accept(ItemRegistry.DEBUG_PAPER.get());
                        output.accept(ItemRegistry.SERVER_VENDOR_ITEM.get());
                    })
                    .build());

    public CreateMarketplace() {
        LOGGER.info(">>> Create: Marketplace is initializing...");

        // Na 1.20.1 szynę modową bierze się z FMLJavaModLoadingContext, a nie
        // z parametru konstruktora jak w NeoForge 1.21.
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BlockRegistry.BLOCKS.register(modEventBus);
        BlockEntityRegistry.BLOCK_ENTITIES.register(modEventBus);
        MenuRegistry.MENU_TYPES.register(modEventBus);
        ItemRegistry.ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MarketConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, MarketConfig.CLIENT_SPEC);

        MarketApi.registerHandler(new NumismaticsShopHandler());
        MarketApi.registerHandler(new ServerVendorShopHandler());

        modEventBus.addListener(this::commonSetup);

        LOGGER.info(">>> Create: Marketplace core components registered.");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(MarketNetwork::register);
        LOGGER.info(">>> Create: Marketplace common setup completed.");
    }
}
