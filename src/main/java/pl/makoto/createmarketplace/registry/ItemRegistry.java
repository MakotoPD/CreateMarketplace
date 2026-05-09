package pl.makoto.createmarketplace.registry;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import pl.makoto.createmarketplace.CreateMarketplace;
import pl.makoto.createmarketplace.item.RegistrationCardItem;

public class ItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateMarketplace.MODID);

    public static final DeferredItem<Item> REGISTRATION_CARD = ITEMS.registerItem("registration_card", 
            properties -> new RegistrationCardItem(properties.durability(10)));
}
