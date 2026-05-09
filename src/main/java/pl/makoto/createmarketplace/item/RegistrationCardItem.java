package pl.makoto.createmarketplace.item;

import net.minecraft.world.item.Item;

public class RegistrationCardItem extends Item {
    public RegistrationCardItem(Item.Properties properties) {
        super(properties);
    }
    // Czysta klasa przedmiotu. Cała logika jest w CommonEvents i ShopScanner.
}
