package pl.makoto.createmarketplace.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class MarketDatabase extends SavedData {
    private final List<MarketOffer> offers = new ArrayList<>();

    public MarketDatabase() {}

    public static MarketDatabase load(CompoundTag tag, HolderLookup.Provider provider) {
        MarketDatabase db = new MarketDatabase();
        ListTag listTag = tag.getList("Offers", Tag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag offerTag = listTag.getCompound(i);
            UUID ownerId = offerTag.getUUID("OwnerId");
            String ownerName = offerTag.contains("OwnerName") ? offerTag.getString("OwnerName") : "Nieznany";
            String shopName = offerTag.getString("ShopName");
            
            // BlockPos
            BlockPos pos = BlockPos.of(offerTag.getLong("Pos"));
            
            // ItemStack deserialization using HolderLookup.Provider
            ItemStack item = ItemStack.parseOptional(provider, offerTag.getCompound("Item"));
            ItemStack currency = ItemStack.parseOptional(provider, offerTag.getCompound("Currency"));
            
            long timestamp = offerTag.getLong("Timestamp");
            
            db.offers.add(new MarketOffer(ownerId, ownerName, shopName, pos, item, currency, timestamp));
        }
        return db;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag listTag = new ListTag();
        for (MarketOffer offer : offers) {
            CompoundTag offerTag = new CompoundTag();
            offerTag.putUUID("OwnerId", offer.ownerId());
            offerTag.putString("OwnerName", offer.ownerName());
            offerTag.putString("ShopName", offer.shopName());
            offerTag.putLong("Pos", offer.pos().asLong());
            offerTag.put("Item", offer.item().saveOptional(provider));
            offerTag.put("Currency", offer.currency().saveOptional(provider));
            offerTag.putLong("Timestamp", offer.timestamp());
            listTag.add(offerTag);
        }
        tag.put("Offers", listTag);
        return tag;
    }

    public void addOffer(MarketOffer offer) {
        // Usuwamy istniejącą ofertę dla tego samego bloku (aktualizacja)
        offers.removeIf(existing -> existing.pos().equals(offer.pos()));
        offers.add(offer);
        setDirty();
    }

    public void removeOffer(BlockPos pos) {
        if (offers.removeIf(offer -> offer.pos().equals(pos))) {
            setDirty();
        }
    }

    public List<MarketOffer> getOffers() {
        return Collections.unmodifiableList(offers);
    }

    public static MarketDatabase get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        return storage.computeIfAbsent(
            new SavedData.Factory<>(MarketDatabase::new, MarketDatabase::load, null),
            "create_marketplace_db"
        );
    }
}
