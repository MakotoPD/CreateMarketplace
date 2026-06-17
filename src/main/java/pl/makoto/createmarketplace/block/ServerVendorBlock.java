package pl.makoto.createmarketplace.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import pl.makoto.createmarketplace.AdminMode;
import pl.makoto.createmarketplace.data.MarketDatabase;
import pl.makoto.createmarketplace.network.OpenServerVendorTradePayload;

/**
 * Blok Server Vendor — administracyjny "vendor serwerowy".
 *
 * Interakcje:
 *  - admin (admin mode ON) + crouch → otwiera ekran konfiguracji (slot przedmiotu,
 *    slot waluty, ceny, włącz/wyłącz strony handlu),
 *  - dowolny gracz (poza powyższym) → ekran handlu kupna/sprzedaży,
 *  - registration_book obsługiwany w {@code CommonEvents} (rejestracja oferty
 *    na Global Market jako "Serwer").
 */
public class ServerVendorBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final MapCodec<ServerVendorBlock> CODEC = simpleCodec(ServerVendorBlock::new);

    public ServerVendorBlock(Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ServerVendorBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ServerVendorBlockEntity sv) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        boolean admin = AdminMode.isAdmin(player.getUUID());

        if (player.isShiftKeyDown()) {
            if (!admin) {
                player.sendSystemMessage(Component.translatable("message.create_marketplace.server_vendor.admin_required")
                        .withStyle(ChatFormatting.YELLOW));
                return InteractionResult.CONSUME;
            }
            // otwiera kontener admin menu — snapshot do bufora dla konstrukcji klienckiej
            sp.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inv, p) -> new pl.makoto.createmarketplace.menu.ServerVendorAdminMenu(id, inv, sv),
                    sv.getDisplayName()
            ), buf -> writeAdminSnapshot(buf, sv, pos));
            return InteractionResult.CONSUME;
        }

        // Brak crouch → ekran handlu (snapshot wysyłany packetem do klienta)
        if (sv.getTradeItem().isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.create_marketplace.server_vendor.empty")
                    .withStyle(ChatFormatting.YELLOW));
            return InteractionResult.CONSUME;
        }
        PacketDistributor.sendToPlayer(sp, new OpenServerVendorTradePayload(
                pos, sv.getTradeItem(), sv.getBuyPrice(), sv.getSellPrice(),
                sv.isBuyEnabled(), sv.isSellEnabled()));
        return InteractionResult.CONSUME;
    }

    private static void writeAdminSnapshot(RegistryFriendlyByteBuf buf, ServerVendorBlockEntity sv, BlockPos pos) {
        buf.writeBlockPos(pos);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, sv.getTradeItem());
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, sv.getBuyPrice());
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, sv.getSellPrice());
        buf.writeBoolean(sv.isBuyEnabled());
        buf.writeBoolean(sv.isSellEnabled());
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && level.getServer() != null) {
            try {
                MarketDatabase db = MarketDatabase.get(level.getServer());
                db.getOffers().stream()
                        .filter(o -> o.pos().equals(pos))
                        .toList()
                        .forEach(o -> db.removeOffer(o.pos()));
                PacketDistributor.sendToAllPlayers(
                        new pl.makoto.createmarketplace.network.MarketUpdatePayload(db.getOffers()));
            } catch (Exception ignored) {}
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
