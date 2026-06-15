package pl.makoto.createmarketplace.registry;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import pl.makoto.createmarketplace.CreateMarketplace;
import pl.makoto.createmarketplace.item.RegistrationBookItem;
import pl.makoto.createmarketplace.item.DebugPaperItem;

public class ItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateMarketplace.MODID);

    public static final DeferredItem<Item> REGISTRATION_BOOK = ITEMS.registerItem("registration_book",
            properties -> new RegistrationBookItem(properties.durability(10)));

    public static final DeferredItem<Item> DEBUG_PAPER = ITEMS.registerItem("debug_paper", 
            properties -> new DebugPaperItem(properties.stacksTo(1)));
}
