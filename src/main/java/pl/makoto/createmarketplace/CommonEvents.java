package pl.makoto.createmarketplace;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;
import pl.makoto.createmarketplace.registry.ItemRegistry;

@EventBusSubscriber(modid = CreateMarketplace.MODID)
public class CommonEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onBlockRightClick(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        
        // 1. Działamy tylko jeśli gracz trzyma księgę rejestracyjną lub papier debugujący
        if (!stack.is(ItemRegistry.REGISTRATION_BOOK.get()) && !stack.is(ItemRegistry.DEBUG_PAPER.get())) {
            return;
        }

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockEntity be = level.getBlockEntity(pos);

        if (be == null) return;

        if (player.getItemInHand(event.getHand()).is(ItemRegistry.DEBUG_PAPER.get())) {
            // Permission check: require OP level 2 or Creative mode
            if (!player.isCreative() && !player.hasPermissions(2)) {
                return; // Allow normal block interaction
            }
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (!level.isClientSide()) {
                CompoundTag nbt = be.saveWithFullMetadata(level.registryAccess());
                player.sendSystemMessage(Component.literal("§e[Debug Paper] §fData for §6" + be.getClass().getSimpleName() + "§f at §7" + pos.toShortString() + ":"));
                player.sendSystemMessage(Component.literal(nbt.toString()));
            }
            return;
        }

        // Wykorzystujemy API do rozpoznania sklepu
        java.util.Optional<pl.makoto.createmarketplace.api.ShopResult> shopResult = pl.makoto.createmarketplace.api.MarketApi.resolveShop(be, level, pos);

        if (shopResult.isEmpty()) {
            return;
        }

        // 2. Jeśli kuca, pozwalamy na normalną interakcję bloku (menu Numismatics/inne)
        if (player.isCrouching()) {
            return;
        }

        // 3. Przejmujemy zdarzenie dla rejestracji
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);

        if (level.isClientSide()) {
            return;
        }

        // --- LOGIKA SERWEROWA ---
        try {
            ItemStack sellingItem = shopResult.get().sellingItem();
            ItemStack currencyItem = shopResult.get().currencyItem();

            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                java.util.List<String> existingShops = new java.util.ArrayList<>();
                try {
                    // w admin mode podpowiadamy istniejące sklepy "Serwera", inaczej własne gracza
                    java.util.UUID ownerFilter = AdminMode.isAdmin(player.getUUID())
                            ? AdminMode.SERVER_UUID : player.getUUID();
                    pl.makoto.createmarketplace.data.MarketDatabase db = pl.makoto.createmarketplace.data.MarketDatabase
                            .get(level.getServer());
                    existingShops = db.getOffers().stream()
                            .filter(o -> o.ownerId().equals(ownerFilter))
                            .map(pl.makoto.createmarketplace.data.MarketOffer::shopName)
                            .distinct()
                            .toList();
                } catch (Exception e) {
                    LOGGER.error("Failed to retrieve existing shops for player {}", player.getUUID(), e);
                }

                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                        serverPlayer,
                        new pl.makoto.createmarketplace.network.OpenRegistrationGuiPayload(pos, sellingItem,
                                currencyItem, existingShops));
            }
        } catch (Exception ignored) {}
    }

    // --- Admin mode: komenda + czyszczenie przy wylogowaniu ---

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("createmarketplace")
                .then(Commands.literal("adminmode")
                    .requires(src -> src.hasPermission(2))
                    .executes(ctx -> setAdminMode(ctx.getSource(), null))
                    .then(Commands.literal("on").executes(ctx -> setAdminMode(ctx.getSource(), true)))
                    .then(Commands.literal("off").executes(ctx -> setAdminMode(ctx.getSource(), false)))));
    }

    private static int setAdminMode(CommandSourceStack src, Boolean on) {
        ServerPlayer player = src.getPlayer();
        if (player == null) {
            src.sendFailure(Component.translatable("message.create_marketplace.player_only"));
            return 0;
        }
        boolean now;
        if (on == null) {
            now = AdminMode.toggle(player.getUUID());
        } else {
            AdminMode.set(player.getUUID(), on);
            now = on;
        }
        player.sendSystemMessage(Component.translatable(
                now ? "message.create_marketplace.adminmode_on" : "message.create_marketplace.adminmode_off")
                .withStyle(now ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        // synchronizuj stan do klienta (wskaźnik w GUI + filtr "Moje sklepy")
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                new pl.makoto.createmarketplace.network.AdminModePayload(now));
        return 1;
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AdminMode.clear(player.getUUID());
        }
    }
}
