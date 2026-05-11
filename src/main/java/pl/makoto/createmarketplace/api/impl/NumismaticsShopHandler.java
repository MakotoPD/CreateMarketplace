package pl.makoto.createmarketplace.api.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import pl.makoto.createmarketplace.api.IShopHandler;
import pl.makoto.createmarketplace.api.ShopResult;
import pl.makoto.createmarketplace.util.ShopScanner;

import java.util.Optional;

/**
 * Handler wspierający bloki z moda Numismatics (Vendor i Table Cloth).
 */
public class NumismaticsShopHandler implements IShopHandler {

    @Override
    public Optional<ShopResult> tryResolve(BlockEntity be, Level level, BlockPos pos) {
        String fullClassName = be.getClass().getName();
        boolean isVendor = fullClassName.contains("VendorBlockEntity");
        boolean isTableCloth = fullClassName.contains("TableClothBlockEntity");

        if (!isVendor && !isTableCloth) {
            return Optional.empty();
        }

        try {
            ItemStack sellingItem = ItemStack.EMPTY;
            ItemStack currencyItem = ItemStack.EMPTY;
            CompoundTag nbt = be.saveWithFullMetadata(level.registryAccess());

            if (isVendor) {
                // Logika dla Vendor
                if (nbt.contains("Selling", 10)) {
                    sellingItem = ItemStack.parseOptional(level.registryAccess(), nbt.getCompound("Selling"));
                } else {
                    sellingItem = ShopScanner.findItemStackRecursive(be, 3);
                }

                if (nbt.contains("Prices", 10)) {
                    CompoundTag prices = nbt.getCompound("Prices");
                    String[] coinNames = {"sun", "crown", "cog", "sprocket", "bevel", "spur"};
                    for (String coinName : coinNames) {
                        if (prices.contains(coinName) && prices.getInt(coinName) > 0) {
                            currencyItem = ShopScanner.getCoinItemByName(coinName, prices.getInt(coinName));
                            break;
                        }
                    }
                }
            } else {
                // Logika dla Table Cloth
                sellingItem = ShopScanner.invokeMethodReturningItemStack(be, "getSellingItem")
                        .or(() -> ShopScanner.invokeMethodReturningItemStack(be, "getFilterItem"))
                        .or(() -> {
                            try {
                                java.lang.reflect.Method m = be.getClass().getMethod("getItemsForRender");
                                Object result = m.invoke(be);
                                if (result instanceof Iterable<?> iterable) {
                                    for (Object o : iterable) {
                                        if (o instanceof ItemStack rs && !rs.isEmpty())
                                            return java.util.Optional.of(rs.copy());
                                    }
                                }
                            } catch (Exception ignored) {}
                            return java.util.Optional.empty();
                        })
                        .orElse(ItemStack.EMPTY);

                if (sellingItem.isEmpty() && nbt.contains("RequestData")) {
                    CompoundTag requestData = nbt.getCompound("RequestData");
                    if (requestData.contains("encoded_request")) {
                        CompoundTag encodedRequest = requestData.getCompound("encoded_request");
                        if (encodedRequest.contains("ordered_stacks")) {
                            CompoundTag orderedStacks = encodedRequest.getCompound("ordered_stacks");
                            if (orderedStacks.contains("entries")) {
                                net.minecraft.nbt.ListTag entries = orderedStacks.getList("entries", 10);
                                if (!entries.isEmpty()) {
                                    CompoundTag entry = entries.getCompound(0);
                                    if (entry.contains("item_stack")) {
                                        sellingItem = ItemStack.parseOptional(level.registryAccess(),
                                                entry.getCompound("item_stack"));
                                    }
                                }
                            }
                        }
                    }
                }
                if (nbt.contains("Filter")) {
                    currencyItem = ItemStack.parseOptional(level.registryAccess(), nbt.getCompound("Filter"));
                    if (nbt.contains("FilterAmount")) {
                        currencyItem.setCount(nbt.getInt("FilterAmount"));
                    } else if (nbt.contains("Price")) {
                        currencyItem.setCount(nbt.getInt("Price"));
                    } else if (nbt.contains("price")) {
                        currencyItem.setCount(nbt.getInt("price"));
                    }
                }
            }

            if (sellingItem.isEmpty() && currencyItem.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(new ShopResult(sellingItem, currencyItem));

        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
