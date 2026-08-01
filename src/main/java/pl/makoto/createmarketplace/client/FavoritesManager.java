package pl.makoto.createmarketplace.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class FavoritesManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File(Minecraft.getInstance().gameDirectory, "config/create_marketplace_favorites.json");
    private static final Logger LOGGER = LogUtils.getLogger();

    private static Set<Long> favoritePositions = new HashSet<>();
    private static final ScheduledExecutorService SAVE_EXECUTOR =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CreateMarketplace-FavoritesSave");
            t.setDaemon(true);
            return t;
        });
    private static final long DEBOUNCE_DELAY_SECONDS = 5;
    private static final DebounceSaveScheduler SAVER =
        new DebounceSaveScheduler(SAVE_EXECUTOR, FavoritesManager::save, DEBOUNCE_DELAY_SECONDS);

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
            LOGGER.error("Failed to load favorites", e);
            favoritePositions = new HashSet<>();
        }
    }

    public static void toggleFavorite(BlockPos pos) {
        long longPos = pos.asLong();
        if (favoritePositions.contains(longPos)) {
            favoritePositions.remove(longPos);
        } else {
            favoritePositions.add(longPos);
        }
        SAVER.scheduleSave();
    }

    /** Wymusza natychmiastowy zapis oczekujących zmian (np. przy zamykaniu gry). */
    public static void flush() {
        SAVER.flush();
    }

    private static void save() {
        try {
            if (!FILE.getParentFile().exists()) {
                FILE.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(FILE)) {
                GSON.toJson(favoritePositions, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save favorites", e);
        }
    }

    public static boolean isFavorite(BlockPos pos) {
        return favoritePositions.contains(pos.asLong());
    }
}
