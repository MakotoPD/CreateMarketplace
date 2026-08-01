package pl.makoto.createmarketplace.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import pl.makoto.createmarketplace.CreateMarketplace;
import pl.makoto.createmarketplace.menu.ServerVendorAdminMenu;

public class MenuRegistry {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, CreateMarketplace.MODID);

    public static final RegistryObject<MenuType<ServerVendorAdminMenu>> SERVER_VENDOR_ADMIN =
            MENU_TYPES.register("server_vendor_admin",
                    () -> IForgeMenuType.create(ServerVendorAdminMenu::new));
}
