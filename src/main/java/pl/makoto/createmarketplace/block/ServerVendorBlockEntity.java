package pl.makoto.createmarketplace.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (!tradeItem.isEmpty()) tag.put("TradeItem", tradeItem.save(provider, new CompoundTag()));
        if (!buyPrice.isEmpty())  tag.put("BuyPrice",  buyPrice.save(provider, new CompoundTag()));
        if (!sellPrice.isEmpty()) tag.put("SellPrice", sellPrice.save(provider, new CompoundTag()));
        tag.putBoolean("BuyEnabled", buyEnabled);
        tag.putBoolean("SellEnabled", sellEnabled);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        tradeItem = tag.contains("TradeItem") ? ItemStack.parseOptional(provider, tag.getCompound("TradeItem")) : ItemStack.EMPTY;
        buyPrice  = tag.contains("BuyPrice")  ? ItemStack.parseOptional(provider, tag.getCompound("BuyPrice"))  : ItemStack.EMPTY;
        sellPrice = tag.contains("SellPrice") ? ItemStack.parseOptional(provider, tag.getCompound("SellPrice")) : ItemStack.EMPTY;
        buyEnabled  = !tag.contains("BuyEnabled")  || tag.getBoolean("BuyEnabled");
        sellEnabled = !tag.contains("SellEnabled") || tag.getBoolean("SellEnabled");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = super.getUpdateTag(provider);
        saveAdditional(tag, provider);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider provider) {
        if (pkt.getTag() != null) loadAdditional(pkt.getTag(), provider);
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
