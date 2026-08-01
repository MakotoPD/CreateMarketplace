package pl.makoto.createmarketplace.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import pl.makoto.createmarketplace.CreateMarketplace;
import pl.makoto.createmarketplace.item.DebugPaperItem;
import pl.makoto.createmarketplace.item.RegistrationBookItem;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CreateMarketplace.MODID);

    public static final RegistryObject<Item> REGISTRATION_BOOK = ITEMS.register("registration_book",
            () -> new RegistrationBookItem(new Item.Properties().durability(10)));

    public static final RegistryObject<Item> DEBUG_PAPER = ITEMS.register("debug_paper",
            () -> new DebugPaperItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<BlockItem> SERVER_VENDOR_ITEM = ITEMS.register("server_vendor",
            () -> new BlockItem(BlockRegistry.SERVER_VENDOR.get(), new Item.Properties()));
}
