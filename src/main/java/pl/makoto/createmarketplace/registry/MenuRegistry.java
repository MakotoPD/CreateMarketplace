package pl.makoto.createmarketplace.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import pl.makoto.createmarketplace.CreateMarketplace;
import pl.makoto.createmarketplace.menu.ServerVendorAdminMenu;

public class MenuRegistry {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, CreateMarketplace.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<ServerVendorAdminMenu>> SERVER_VENDOR_ADMIN =
            MENU_TYPES.register("server_vendor_admin",
                    () -> IMenuTypeExtension.create(ServerVendorAdminMenu::new));
}
