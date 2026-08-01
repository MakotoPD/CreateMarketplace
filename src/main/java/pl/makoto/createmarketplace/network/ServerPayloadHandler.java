package pl.makoto.createmarketplace.network;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import pl.makoto.createmarketplace.MarketConfig;
import pl.makoto.createmarketplace.api.MarketApi;
import pl.makoto.createmarketplace.api.event.MarketOfferEvent;
import pl.makoto.createmarketplace.data.MarketDatabase;
import pl.makoto.createmarketplace.data.MarketOffer;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.List;

public class ServerPayloadHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_SHOP_NAME_LENGTH = 32;

    /**
     * Górny limit ilości w jednej transakcji Server Vendor.
     *
     * Klient przysyła {@code quantity} jako surowy varint, więc bez tego limitu
     * iloczyn {@code count × wartość_monety × qty} przekracza zakres int, staje się
     * ujemny i przechodzi walidację środków — gracz dostaje towar za darmo.
     * Przy 4096: 64 × 4096 (sun) × 4096 = 1_073_741_824 < Integer.MAX_VALUE.
     * Limit jest i tak wielokrotnie wyższy niż pojemność ekwipunku (36 × 64).
     */
    private static final int MAX_TRADE_QUANTITY = 4096;

    /** Kwadrat zasięgu interakcji z blokiem (8 bloków) — jak vanilla {@code Container.stillValid}. */
    private static final double MAX_BLOCK_REACH_SQR = 64.0;

    public static void handlePublishShop(final PublishShopPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            MarketOffer clientOffer = payload.offer();
            boolean admin = pl.makoto.createmarketplace.AdminMode.isAdmin(player.getUUID());

            // Step 1: Check Registration Card (admin mode i kreatywny pomijają)
            if (!admin && !player.isCreative()) {
                if (!consumeOrDamageCard(player)) {
                    player.sendSystemMessage(Component.translatable("message.create_marketplace.no_card")
                        .withStyle(ChatFormatting.RED));
                    return;
                }
            }

            // Step 2: Validate shopName
            String shopName = sanitizeShopName(clientOffer.shopName());
            if (shopName == null) {
                player.sendSystemMessage(Component.translatable("message.create_marketplace.invalid_shop_name")
                    .withStyle(ChatFormatting.RED));
                return;
            }

            // Step 3: Validate position distance (≤64 blocks)
            BlockPos pos = clientOffer.pos();
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64 * 64) {
                player.sendSystemMessage(Component.translatable("message.create_marketplace.too_far")
                    .withStyle(ChatFormatting.RED));
                return;
            }

            // Step 4: Validate shop block
            BlockEntity be = player.serverLevel().getBlockEntity(pos);
            if (be == null) {
                player.sendSystemMessage(Component.translatable("message.create_marketplace.invalid_block")
                    .withStyle(ChatFormatting.RED));
                return;
            }
            if (MarketApi.resolveShop(be, player.serverLevel(), pos).isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.create_marketplace.not_a_shop")
                    .withStyle(ChatFormatting.RED));
                return;
            }

            // Step 5: Check offer limit (admin mode pomija — sklepy "Serwera")
            MarketDatabase db = MarketDatabase.get(player.server);
            if (!admin) {
                long playerOfferCount = db.getOffers().stream()
                    .filter(o -> o.ownerId().equals(player.getUUID()))
                    .count();
                if (playerOfferCount >= MarketConfig.MAX_OFFERS_PER_PLAYER.get()) {
                    player.sendSystemMessage(Component.translatable("message.create_marketplace.offer_limit_reached",
                        MarketConfig.MAX_OFFERS_PER_PLAYER.get()).withStyle(ChatFormatting.RED));
                    return;
                }
            }

            // Step 6: Override identity — zwykły gracz albo "Serwer" w admin mode
            java.util.UUID ownerId = admin ? pl.makoto.createmarketplace.AdminMode.SERVER_UUID : player.getUUID();
            String ownerName = admin ? pl.makoto.createmarketplace.AdminMode.SERVER_NAME : player.getGameProfile().getName();
            MarketOffer serverOffer = new MarketOffer(
                ownerId,
                ownerName,
                shopName,
                clientOffer.pos(),
                player.level().dimension().location(),
                clientOffer.item(),
                clientOffer.currency(),
                System.currentTimeMillis()
            );

            // Step 7: Fire event, persist, broadcast
            var event = new MarketOfferEvent.Register(serverOffer, player);
            if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
                player.sendSystemMessage(Component.translatable("message.create_marketplace.cancelled_by_mod")
                    .withStyle(ChatFormatting.RED));
                return;
            }

            db.addOffer(serverOffer);
            // od razu zmierz stan, żeby nowy sklep nie wisiał jako "nieznany" do najbliższego skanu
            pl.makoto.createmarketplace.util.StockChecker.refreshOne(player.serverLevel(), serverOffer);
            NeoForge.EVENT_BUS.post(new MarketOfferEvent.Registered(serverOffer, player));

            LOGGER.info("Server received and saved offer for shop: {} (owner: {})", shopName, ownerName);
            player.sendSystemMessage(Component.translatable(
                    admin ? "message.create_marketplace.registration_success_server" : "message.create_marketplace.registration_success",
                    shopName).withStyle(ChatFormatting.GREEN));
            PacketDistributor.sendToAllPlayers(MarketUpdatePayload.of(db));
        });
    }

    /**
     * Attempts to consume or damage a Registration Book from the player's inventory.
     * Returns true if a book was found (and consumed/damaged), false otherwise.
     */
    private static boolean consumeOrDamageCard(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(pl.makoto.createmarketplace.registry.ItemRegistry.REGISTRATION_BOOK.get())) {
                if (MarketConfig.USE_CARD_DURABILITY.get()) {
                    int damage = stack.getDamageValue();
                    if (damage + 1 >= stack.getMaxDamage()) {
                        stack.shrink(1);
                    } else {
                        stack.setDamageValue(damage + 1);
                    }
                }
                return true;
            }
        }
        return false;
    }

    public static void handleRefreshRequest(final RequestMarketRefreshPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                MarketDatabase db = MarketDatabase.get(player.server);

                // Doskan przy otwarciu GUI — łapie to, co akurat jest w załadowanych
                // chunkach. Reszta poleci z ostatnim znanym odczytem i jego wiekiem.
                pl.makoto.createmarketplace.util.StockChecker.scan(player.server);

                // Wysyłamy do gracza, który zażądał odświeżenia
                PacketDistributor.sendToPlayer(player, MarketUpdatePayload.of(db));

                LOGGER.debug("Server sent {} offers to player {}", db.getOffers().size(), player.getName().getString());
            }
        });
    }

    public static void handleDeleteShop(final DeleteShopPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                MarketDatabase db = MarketDatabase.get(player.server);

                boolean admin = pl.makoto.createmarketplace.AdminMode.isAdmin(player.getUUID());
                // usuwanie całego sklepu (po nazwie): admin → sklepy "Serwera", inaczej własne
                java.util.UUID ownerFilter = admin
                        ? pl.makoto.createmarketplace.AdminMode.SERVER_UUID : player.getUUID();

                DeleteShopLogic.DeleteResult result = null;
                if (payload.posToRemove().isPresent()) {
                    // usuwanie pojedynczej oferty: admin może KAŻDĄ, gracz tylko własną
                    result = DeleteShopLogic.deleteByPosition(
                            db.getOffers(), payload.posToRemove().get(), player.getUUID(), admin);
                } else if (payload.shopNameToRemove().isPresent()) {
                    result = DeleteShopLogic.deleteByShopName(
                            db.getOffers(), payload.shopNameToRemove().get(), ownerFilter);
                }

                if (result != null && result.changed()) {
                    for (MarketOffer offer : result.removed()) {
                        db.removeOffer(offer.pos());
                        NeoForge.EVENT_BUS.post(new MarketOfferEvent.Removed(offer, player));
                    }
                    player.sendSystemMessage(Component.translatable("message.create_marketplace.delete_success").withStyle(net.minecraft.ChatFormatting.GREEN));
                    PacketDistributor.sendToAllPlayers(MarketUpdatePayload.of(db));
                }
            }
        });
    }

    public static void handleSaveServerVendor(final SaveServerVendorPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!pl.makoto.createmarketplace.AdminMode.isAdmin(player.getUUID())) {
                player.sendSystemMessage(Component.translatable("message.create_marketplace.server_vendor.admin_required")
                        .withStyle(ChatFormatting.RED));
                return;
            }
            BlockPos pos = payload.pos();
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_BLOCK_REACH_SQR) {
                player.sendSystemMessage(Component.translatable("message.create_marketplace.server_vendor.too_far")
                        .withStyle(ChatFormatting.RED));
                return;
            }
            BlockEntity be = player.serverLevel().getBlockEntity(pos);
            if (!(be instanceof pl.makoto.createmarketplace.block.ServerVendorBlockEntity sv)) {
                player.sendSystemMessage(Component.translatable("message.create_marketplace.invalid_block")
                        .withStyle(ChatFormatting.RED));
                return;
            }
            sv.applySnapshot(payload.tradeItem(), payload.buyPrice(), payload.sellPrice(),
                    payload.buyEnabled(), payload.sellEnabled());
        });
    }

    public static void handleServerVendorTrade(final ServerVendorTradePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            BlockPos pos = payload.pos();
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_BLOCK_REACH_SQR) {
                sendResult(player, false, "message.create_marketplace.server_vendor.too_far", 0);
                return;
            }
            BlockEntity be = player.serverLevel().getBlockEntity(pos);
            if (!(be instanceof pl.makoto.createmarketplace.block.ServerVendorBlockEntity sv)) {
                sendResult(player, false, "message.create_marketplace.invalid_block", 0);
                return;
            }
            // qty pochodzi wprost od klienta — clamp jest jedyną barierą przed przepełnieniem int.
            int qty = Math.min(payload.quantity(), MAX_TRADE_QUANTITY);
            if (qty <= 0) return;
            net.minecraft.world.item.ItemStack template = sv.getTradeItem();
            if (template.isEmpty()) {
                sendResult(player, false, "message.create_marketplace.server_vendor.empty", 0);
                return;
            }
            boolean buying = payload.buying();
            if ((buying && !sv.isBuyEnabled()) || (!buying && !sv.isSellEnabled())) {
                sendResult(player, false, "message.create_marketplace.server_vendor.disabled", 0);
                return;
            }
            net.minecraft.world.item.ItemStack priceStack = buying ? sv.getBuyPrice() : sv.getSellPrice();
            if (priceStack.isEmpty()) {
                sendResult(player, false, "message.create_marketplace.server_vendor.empty", 0);
                return;
            }

            var inv = player.getInventory();
            int unitItemCount = Math.max(1, template.getCount());
            boolean priceIsCoin = pl.makoto.createmarketplace.util.Coinage.isNumismaticsCoin(priceStack);

            if (buying) {
                // gracz płaci priceStack × qty, otrzymuje template × qty
                if (priceIsCoin) {
                    int totalSpurs = priceStack.getCount()
                            * pl.makoto.createmarketplace.util.Coinage.coinValueOf(priceStack) * qty;
                    if (!pl.makoto.createmarketplace.util.Coinage.canDeductSpurs(inv, totalSpurs)) {
                        sendResult(player, false, "message.create_marketplace.server_vendor.no_funds", 0);
                        return;
                    }
                    pl.makoto.createmarketplace.util.Coinage.deductSpurs(inv, totalSpurs);
                } else {
                    int totalItems = priceStack.getCount() * qty;
                    if (!pl.makoto.createmarketplace.util.Coinage.canDeductCustom(inv, priceStack, totalItems)) {
                        sendResult(player, false, "message.create_marketplace.server_vendor.no_funds", 0);
                        return;
                    }
                    pl.makoto.createmarketplace.util.Coinage.deductCustom(inv, priceStack, totalItems);
                }
                int totalGive = unitItemCount * qty;
                int maxStack = template.getMaxStackSize();
                while (totalGive > 0) {
                    int take = Math.min(totalGive, maxStack);
                    net.minecraft.world.item.ItemStack s = template.copyWithCount(take);
                    if (!inv.add(s)) player.drop(s, false);
                    totalGive -= take;
                }
                sendResult(player, true, "message.create_marketplace.server_vendor.ok", qty);
            } else {
                // gracz oddaje template × qty, otrzymuje priceStack × qty (jako monety lub items)
                int needed = unitItemCount * qty;
                int have = 0;
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    net.minecraft.world.item.ItemStack s = inv.getItem(i);
                    if (!s.isEmpty() && net.minecraft.world.item.ItemStack.isSameItemSameComponents(s, template)) {
                        have += s.getCount();
                    }
                }
                if (have < needed) {
                    sendResult(player, false, "message.create_marketplace.server_vendor.no_items", 0);
                    return;
                }
                int remaining = needed;
                for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
                    net.minecraft.world.item.ItemStack s = inv.getItem(i);
                    if (s.isEmpty()) continue;
                    if (!net.minecraft.world.item.ItemStack.isSameItemSameComponents(s, template)) continue;
                    int take = Math.min(remaining, s.getCount());
                    s.shrink(take);
                    remaining -= take;
                }
                if (priceIsCoin) {
                    int totalSpurs = priceStack.getCount()
                            * pl.makoto.createmarketplace.util.Coinage.coinValueOf(priceStack) * qty;
                    pl.makoto.createmarketplace.util.Coinage.giveSpurs(player, totalSpurs);
                } else {
                    int totalItems = priceStack.getCount() * qty;
                    pl.makoto.createmarketplace.util.Coinage.giveCustom(player, priceStack, totalItems);
                }
                sendResult(player, true, "message.create_marketplace.server_vendor.ok", qty);
            }
        });
    }

    private static void sendResult(ServerPlayer player, boolean ok, String key, int units) {
        PacketDistributor.sendToPlayer(player, new ServerVendorTradeResultPayload(ok, key, units));
    }

    /**
     * Sanitizes a shop name. Returns null if the name is invalid (empty/whitespace-only or too long).
     * Strips § formatting codes before length check.
     */
    static String sanitizeShopName(String raw) {
        if (raw == null) return null;
        // Strip section sign formatting codes (§ followed by any character)
        String stripped = raw.replaceAll("§.", "");
        String trimmed = stripped.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.length() > MAX_SHOP_NAME_LENGTH) return null;
        return trimmed;
    }
}
