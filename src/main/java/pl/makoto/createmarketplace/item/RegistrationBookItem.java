package pl.makoto.createmarketplace.item;

import net.minecraft.world.item.Item;

public class RegistrationBookItem extends Item {
    public RegistrationBookItem(Item.Properties properties) {
        super(properties);
    }
    // Czysta klasa przedmiotu. Cała logika jest w CommonEvents i ServerPayloadHandler.
}
