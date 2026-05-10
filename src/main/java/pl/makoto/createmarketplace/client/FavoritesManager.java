package pl.makoto.createmarketplace.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

public class FavoritesManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File(Minecraft.getInstance().gameDirectory, "config/create_marketplace_favorites.json");
    
    private static Set<Long> favoritePositions = new HashSet<>();

    static {
        load();
    }

    public static void load() {
        if (!FILE.exists()) {
            favoritePositions = new HashSet<>();
            return;
        }
        try (FileReader reader = new FileReader(FILE)) {
            Type setType = new TypeToken<HashSet<Long>>() {}.getType();
            favoritePositions = GSON.fromJson(reader, setType);
            if (favoritePositions == null) favoritePositions = new HashSet<>();
        } catch (Exception e) {
            e.printStackTrace();
            favoritePositions = new HashSet<>();
        }
    }

    public static void save() {
        try {
            if (!FILE.getParentFile().exists()) {
                FILE.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(FILE)) {
                GSON.toJson(favoritePositions, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isFavorite(BlockPos pos) {
        return favoritePositions.contains(pos.asLong());
    }

    public static void toggleFavorite(BlockPos pos) {
        long longPos = pos.asLong();
        if (favoritePositions.contains(longPos)) {
            favoritePositions.remove(longPos);
        } else {
            favoritePositions.add(longPos);
        }
        save();
    }
}
