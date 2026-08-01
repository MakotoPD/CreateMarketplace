package pl.makoto.createmarketplace.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pl.makoto.createmarketplace.menu.ServerVendorAdminMenu;
import pl.makoto.createmarketplace.registry.BlockEntityRegistry;

/**
 * Stan Server Vendor. Trzy "szablony" przedmiotów:
 *  - {@code tradeItem} — co serwer handluje (count = sztuk na 1 transakcję),
 *  - {@code buyPrice} — co gracz musi zapłacić (monety Numismatics lub dowolny item),
 *  - {@code sellPrice} — co gracz otrzymuje przy sprzedaży.
 *
 * Liczba w stacku to wartość ceny: stack monet Numismatics jest mnożony przez wartość
 * monety w spursach (np. 5 cogs = 5×64 = 320 spurs), a stack dowolnego itemu liczy się
 * po prostu sztukami.
 */
public class ServerVendorBlockEntity extends BlockEntity implements MenuProvider {

    private ItemStack tradeItem = ItemStack.EMPTY;
    private ItemStack buyPrice = ItemStack.EMPTY;
    private ItemStack sellPrice = ItemStack.EMPTY;
    private boolean buyEnabled = true;
    private boolean sellEnabled = true;

    public ServerVendorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.SERVER_VENDOR.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!tradeItem.isEmpty()) tag.put("TradeItem", tradeItem.save(new CompoundTag()));
        if (!buyPrice.isEmpty())  tag.put("BuyPrice",  buyPrice.save(new CompoundTag()));
        if (!sellPrice.isEmpty()) tag.put("SellPrice", sellPrice.save(new CompoundTag()));
        tag.putBoolean("BuyEnabled", buyEnabled);
        tag.putBoolean("SellEnabled", sellEnabled);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        tradeItem = tag.contains("TradeItem") ? ItemStack.of(tag.getCompound("TradeItem")) : ItemStack.EMPTY;
        buyPrice  = tag.contains("BuyPrice")  ? ItemStack.of(tag.getCompound("BuyPrice"))  : ItemStack.EMPTY;
        sellPrice = tag.contains("SellPrice") ? ItemStack.of(tag.getCompound("SellPrice")) : ItemStack.EMPTY;
        buyEnabled  = !tag.contains("BuyEnabled")  || tag.getBoolean("BuyEnabled");
        sellEnabled = !tag.contains("SellEnabled") || tag.getBoolean("SellEnabled");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        if (pkt.getTag() != null) load(pkt.getTag());
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.create_marketplace.server_vendor.admin.title");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ServerVendorAdminMenu(id, inv, this);
    }

    public void applySnapshot(ItemStack item, ItemStack buy, ItemStack sell, boolean buyEn, boolean sellEn) {
        this.tradeItem = item == null ? ItemStack.EMPTY : item.copy();
        this.buyPrice = buy == null ? ItemStack.EMPTY : buy.copy();
        this.sellPrice = sell == null ? ItemStack.EMPTY : sell.copy();
        this.buyEnabled = buyEn;
        this.sellEnabled = sellEn;
        sync();
    }

    public ItemStack getTradeItem() { return tradeItem; }
    public ItemStack getBuyPrice()  { return buyPrice; }
    public ItemStack getSellPrice() { return sellPrice; }
    public boolean isBuyEnabled()   { return buyEnabled; }
    public boolean isSellEnabled()  { return sellEnabled; }
}
