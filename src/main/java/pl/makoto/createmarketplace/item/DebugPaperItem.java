package pl.makoto.createmarketplace.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class DebugPaperItem extends Item {
    public DebugPaperItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
