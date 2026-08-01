package pl.makoto.createmarketplace.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MarketDatabase extends SavedData {
    private final List<MarketOffer> offers = new ArrayList<>();

    /**
     * Stan magazynowy per pozycja sklepu. Świadomie NIE jest zapisywany razem z
     * ofertami — po restarcie serwera każdy odczyt byłby i tak nieaktualny, a
     * pokazanie graczowi "ma towar" na podstawie danych sprzed tygodnia to
     * dokładnie ten błąd, którego ta funkcja ma unikać. Mapa żyje tyle, co świat.
     */
    private final Map<BlockPos, StockInfo> stock = new HashMap<>();

    public MarketDatabase() {}

    public static MarketDatabase load(CompoundTag tag) {
        MarketDatabase db = new MarketDatabase();
        ListTag listTag = tag.getList("Offers", Tag.TAG_COMPOUND);
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag offerTag = listTag.getCompound(i);
            UUID ownerId = offerTag.getUUID("OwnerId");
            String ownerName = offerTag.contains("OwnerName") ? offerTag.getString("OwnerName") : "Nieznany";
            String shopName = offerTag.getString("ShopName");
            
            // BlockPos
            BlockPos pos = BlockPos.of(offerTag.getLong("Pos"));

            // wymiar (stare oferty bez pola → zakładamy overworld)
            ResourceLocation dimension = offerTag.contains("Dimension")
                    ? ResourceLocation.tryParse(offerTag.getString("Dimension")) : null;
            if (dimension == null) dimension = Level.OVERWORLD.location();

            // ItemStack deserialization using HolderLookup.Provider
            ItemStack item = ItemStack.of(offerTag.getCompound("Item"));
            ItemStack currency = ItemStack.of(offerTag.getCompound("Currency"));

            long timestamp = offerTag.getLong("Timestamp");

            db.offers.add(new MarketOffer(ownerId, ownerName, shopName, pos, dimension, item, currency, timestamp));
        }
        return db;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag listTag = new ListTag();
        for (MarketOffer offer : offers) {
            CompoundTag offerTag = new CompoundTag();
            offerTag.putUUID("OwnerId", offer.ownerId());
            offerTag.putString("OwnerName", offer.ownerName());
            offerTag.putString("ShopName", offer.shopName());
            offerTag.putLong("Pos", offer.pos().asLong());
            offerTag.putString("Dimension", offer.dimension().toString());
            offerTag.put("Item", offer.item().save(new CompoundTag()));
            offerTag.put("Currency", offer.currency().save(new CompoundTag()));
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
        stock.remove(pos);
        if (offers.removeIf(offer -> offer.pos().equals(pos))) {
            setDirty();
        }
    }

    public List<MarketOffer> getOffers() {
        return Collections.unmodifiableList(offers);
    }

    /**
     * Zapisuje odczyt stanu magazynowego.
     *
     * @return true, jeśli zmieniła się sama liczba (a nie tylko znacznik czasu) —
     *         dzięki temu skan cykliczny rozsyła pakiet tylko przy realnej zmianie
     */
    public boolean setStock(BlockPos pos, StockInfo info) {
        StockInfo previous = stock.put(pos, info);
        return previous == null || previous.units() != info.units();
    }

    public StockInfo getStock(BlockPos pos) {
        return stock.getOrDefault(pos, StockInfo.NONE);
    }

    public static MarketDatabase get(MinecraftServer server) {
        DimensionDataStorage storage = server.overworld().getDataStorage();
        // 1.20.1: computeIfAbsent(loader, konstruktor, nazwa) - nie ma SavedData.Factory
        return storage.computeIfAbsent(MarketDatabase::load, MarketDatabase::new, "create_marketplace_db");
    }
}
