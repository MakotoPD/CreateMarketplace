package pl.makoto.createmarketplace;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import pl.makoto.createmarketplace.registry.ItemRegistry;
import pl.makoto.createmarketplace.util.ShopScanner;

@EventBusSubscriber(modid = CreateMarketplace.MODID, bus = EventBusSubscriber.Bus.GAME)
public class CommonEvents {

    @SubscribeEvent
    public static void onBlockRightClick(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        
        // 1. Działamy TYLKO jeśli gracz trzyma naszą kartę
        if (!stack.is(ItemRegistry.REGISTRATION_CARD.get())) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockEntity be = level.getBlockEntity(pos);

        if (be == null) return;

        String fullClassName = be.getClass().getName();
        boolean isVendor = fullClassName.contains("VendorBlockEntity");
        boolean isTableCloth = fullClassName.contains("TableClothBlockEntity");

        if (!isVendor && !isTableCloth) {
            return;
        }

        // 2. Jeśli kuca, pozwalamy na normalną interakcję bloku (menu Numismatics)
        if (player.isCrouching()) {
            return;
        }

        // 3. Jeśli to sklep i trzymamy kartę (bez Shifta), przejmujemy zdarzenie dla rejestracji
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (level.isClientSide()) {
            return;
        }

        // --- LOGIKA SERWEROWA ---
        try {
            ItemStack sellingItem = ItemStack.EMPTY;
            ItemStack currencyItem = ItemStack.EMPTY;

            CompoundTag nbt = be.saveWithFullMetadata(level.registryAccess());

            if (isVendor) {
                // 1. Wyciągamy przedmiot bezpośrednio z tagu "Selling" (format 1.21.1)
                if (nbt.contains("Selling", 10)) {
                    sellingItem = ItemStack.parseOptional(level.registryAccess(), nbt.getCompound("Selling"));
                } else {
                    // Fallback do skanowania rekurencyjnego
                    sellingItem = ShopScanner.findItemStackRecursive(be, 3);
                }

                // 2. Wyciągamy cenę z nowego formatu (CompoundTag zamiast ListTag)
                if (nbt.contains("Prices", 10)) {
                    CompoundTag prices = nbt.getCompound("Prices");
                    // Szukamy najwyższej monety (hierarchia Numismatics 1.21.1)
                    String[] coinNames = {"sun", "crown", "cog", "sprocket", "bevel", "spur"};
                    for (String coinName : coinNames) {
                        if (prices.contains(coinName) && prices.getInt(coinName) > 0) {
                            currencyItem = ShopScanner.getCoinItemByName(coinName, prices.getInt(coinName));
                            break;
                        }
                    }
                }
            } else if (isTableCloth) {
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
                }
            }

            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                java.util.List<String> existingShops = new java.util.ArrayList<>();
                try {
                    pl.makoto.createmarketplace.data.MarketDatabase db = pl.makoto.createmarketplace.data.MarketDatabase
                            .get(level.getServer());
                    existingShops = db.getOffers().stream()
                            .filter(o -> o.ownerId().equals(player.getUUID()))
                            .map(pl.makoto.createmarketplace.data.MarketOffer::shopName)
                            .distinct()
                            .toList();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                        serverPlayer,
                        new pl.makoto.createmarketplace.network.OpenRegistrationGuiPayload(pos, sellingItem,
                                currencyItem, existingShops));
            }
        } catch (Exception ignored) {}
    }
}
