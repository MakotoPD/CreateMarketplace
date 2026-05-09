package pl.makoto.createmarketplace.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import pl.makoto.createmarketplace.data.MarketDatabase;
import pl.makoto.createmarketplace.data.MarketOffer;

import java.util.List;

public class ServerPayloadHandler {

    public static void handlePublishShop(final PublishShopPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                // Sprawdzenie, czy gracz posiada kartę (jeśli tak, zabieramy 1 sztukę)
                boolean hasCard = false;
                if (!player.isCreative()) {
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
                        if (stack.is(pl.makoto.createmarketplace.registry.ItemRegistry.REGISTRATION_CARD.get())) {
                            int damage = stack.getDamageValue();
                            if (damage + 1 >= stack.getMaxDamage()) {
                                stack.shrink(1);
                            } else {
                                stack.setDamageValue(damage + 1);
                            }
                            hasCard = true;
                            break;
                        }
                    }
                } else {
                    hasCard = true;
                }

                if (hasCard) {
                    MarketDatabase db = MarketDatabase.get(player.server);
                    db.addOffer(payload.offer());
                    
                    System.out.println("Serwer otrzymał i zapisał ofertę na sklep: " + payload.offer().shopName());
                    player.sendSystemMessage(Component.literal("Serwer pomyślnie zarejestrował Twój sklep: " + payload.offer().shopName()).withStyle(net.minecraft.ChatFormatting.GREEN));
                    
                    // Rozsyłamy odświeżoną listę do wszystkich podłączonych graczy
                    PacketDistributor.sendToAllPlayers(new MarketUpdatePayload(db.getOffers()));
                } else {
                    player.sendSystemMessage(Component.literal("Nie posiadasz Karty Rejestracyjnej w ekwipunku!").withStyle(net.minecraft.ChatFormatting.RED));
                }
            }
        });
    }

    public static void handleRefreshRequest(final RequestMarketRefreshPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                MarketDatabase db = MarketDatabase.get(player.server);
                List<MarketOffer> offers = db.getOffers();
                
                // Wysyłamy do gracza, który zażądał odświeżenia
                PacketDistributor.sendToPlayer(player, new MarketUpdatePayload(offers));
                
                System.out.println("Serwer wysłał " + offers.size() + " ofert do gracza " + player.getName().getString());
            }
        });
    }

    public static void handleDeleteShop(final DeleteShopPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                MarketDatabase db = MarketDatabase.get(player.server);
                
                boolean changed = false;
                if (payload.posToRemove().isPresent()) {
                    BlockPos pos = payload.posToRemove().get();
                    // Sprawdzamy czy użytkownik jest właścicielem
                    db.getOffers().stream()
                        .filter(o -> o.pos().equals(pos) && o.ownerId().equals(player.getUUID()))
                        .findFirst()
                        .ifPresent(offer -> db.removeOffer(pos));
                    changed = true;
                } else if (payload.shopNameToRemove().isPresent()) {
                    String shopName = payload.shopNameToRemove().get();
                    // Usuwamy wszystkie oferty gracza o danej nazwie
                    db.getOffers().stream()
                        .filter(o -> o.shopName().equals(shopName) && o.ownerId().equals(player.getUUID()))
                        .map(MarketOffer::pos)
                        .toList() // kopiujemy do listy, żeby uniknąć ConcurrentModificationException
                        .forEach(db::removeOffer);
                    changed = true;
                }
                
                if (changed) {
                    player.sendSystemMessage(Component.literal("Pomyślnie usunięto sklep(y).").withStyle(net.minecraft.ChatFormatting.GREEN));
                    PacketDistributor.sendToAllPlayers(new MarketUpdatePayload(db.getOffers()));
                }
            }
        });
    }
}
