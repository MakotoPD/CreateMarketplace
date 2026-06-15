package pl.makoto.createmarketplace.client;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import pl.makoto.createmarketplace.MarketConfig;
import pl.makoto.createmarketplace.data.MarketOffer;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * Integracja z Xaero's Minimap (przez refleksję — mod jest opcjonalny).
 * Automatycznie dodaje waypoint do zestawu waypointów WYMIARU SKLEPU (nie
 * gracza), więc np. sklep w Netherze tworzy waypoint w Netherze, a nie w
 * bieżącym wymiarze. Zapis przez MinimapWorldManagerIO.saveWorld (to samo,
 * czego używa natywne okno Xaero przy zatwierdzeniu) — działa na Xaero 25.x,
 * gdzie zniknęło dawne WaypointsManager.saveWaypoints().
 */
public final class XaeroCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String MOD_MINIMAP = "xaerominimap";
    private static final String CLS_SESSION = "xaero.common.XaeroMinimapSession";
    private static final String CLS_WAYPOINT = "xaero.common.minimap.waypoints.Waypoint";
    private static final String CLS_MINIMAP_WORLD = "xaero.hud.minimap.world.MinimapWorld";
    private static final String DEFAULT_SET = "gui.xaero_default";

    private XaeroCompat() {}

    public static boolean isPresent() {
        return ModList.get().isLoaded(MOD_MINIMAP);
    }

    /** Dodaje waypoint sklepu (w jego wymiarze) i zapisuje. Zwraca false gdy brak Xaero/błąd. */
    public static boolean addShopWaypoint(MarketOffer offer) {
        if (!isPresent()) return false;
        try {
            Object session = Class.forName(CLS_SESSION).getMethod("getCurrentSession").invoke(null);
            if (session == null) return false;
            Object manager = session.getClass().getMethod("getWaypointsManager").invoke(session);
            if (manager == null) return false;

            // świat waypointów wymiaru sklepu (fallback: bieżący świat gracza)
            Object world = resolveDimensionWorld(manager, offer.dimension());
            if (world == null) world = manager.getClass().getMethod("getCurrentWorld").invoke(manager);
            if (world == null) return false;

            Object set = currentSet(world);
            if (set == null) return false;
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) set.getClass().getMethod("getList").invoke(set);

            int x = offer.pos().getX(), y = offer.pos().getY(), z = offer.pos().getZ();
            // pomiń duplikat (te same współrzędne + nazwa)
            for (Object w : list) {
                if (sameWaypoint(w, x, y, z, offer.shopName())) {
                    saveWorld(manager, world); // upewnij się, że istniejący jest zapisany
                    return true;
                }
            }

            Object waypoint = newWaypoint(x, y, z, offer.shopName(), resolveSymbol(offer.shopName()));
            if (waypoint == null) return false;
            list.add(waypoint);

            saveWorld(manager, world);
            return true;
        } catch (Exception e) {
            LOGGER.warn("[CreateMarketplace] Nie udało się dodać waypointu Xaero: {}", e.getMessage());
            return false;
        }
    }

    /** Świat waypointów danego wymiaru (ten sam kontener/serwer, inny world id). */
    private static Object resolveDimensionWorld(Object manager, ResourceLocation dimension) {
        try {
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimension);
            String container = (String) manager.getClass().getMethod("getCurrentContainerID").invoke(manager);
            boolean mp = (boolean) manager.getClass().getMethod("isMultiplayer", String.class).invoke(manager, container);
            String worldId = (String) manager.getClass()
                    .getMethod("getNewAutoWorldID", ResourceKey.class, boolean.class)
                    .invoke(manager, dimKey, mp);
            Object world = manager.getClass().getMethod("getWorld", String.class, String.class)
                    .invoke(manager, container, worldId);
            if (world == null) {
                world = manager.getClass().getMethod("addWorld", String.class, String.class)
                        .invoke(manager, container, worldId);
            }
            return world;
        } catch (Exception e) {
            return null;
        }
    }

    /** Bieżący zestaw świata; gdy brak — tworzy domyślny. */
    private static Object currentSet(Object world) {
        try {
            Object set = world.getClass().getMethod("getCurrentSet").invoke(world);
            if (set != null) return set;
            world.getClass().getMethod("addSet", String.class).invoke(world, DEFAULT_SET);
            world.getClass().getMethod("setCurrent", String.class).invoke(world, DEFAULT_SET);
            return world.getClass().getMethod("getCurrentSet").invoke(world);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object newWaypoint(int x, int y, int z, String name, String symbol) throws Exception {
        Class<?> cls = Class.forName(CLS_WAYPOINT);
        for (Constructor<?> c : cls.getConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length == 8 && p[0] == int.class && p[3] == String.class
                    && p[5] == int.class && p[6] == int.class && p[7] == boolean.class) {
                return c.newInstance(x, y, z, name, symbol, 1, 0, false);
            }
        }
        return null;
    }

    private static boolean sameWaypoint(Object w, int x, int y, int z, String name) {
        try {
            return (int) w.getClass().getMethod("getX").invoke(w) == x
                    && (int) w.getClass().getMethod("getY").invoke(w) == y
                    && (int) w.getClass().getMethod("getZ").invoke(w) == z
                    && name.equals(w.getClass().getMethod("getName").invoke(w));
        } catch (Exception e) {
            return false;
        }
    }

    private static void saveWorld(Object manager, Object world) {
        try {
            Object io = manager.getClass().getMethod("getWorldManagerIO").invoke(manager);
            io.getClass().getMethod("saveWorld", Class.forName(CLS_MINIMAP_WORLD)).invoke(io, world);
        } catch (Exception e) {
            // zapis nieudany — waypoint i tak jest w pamięci, Xaero zapisze przy autosave
            LOGGER.debug("[CreateMarketplace] saveWorld nieudane: {}", e.getMessage());
        }
    }

    private static String resolveSymbol(String name) {
        MarketConfig.WaypointSymbolMode mode = MarketConfig.WAYPOINT_SYMBOL_MODE.get();
        if (mode == MarketConfig.WaypointSymbolMode.FIRST_LETTER && name != null && !name.isEmpty()) {
            return name.substring(0, 1).toUpperCase();
        } else if (mode == MarketConfig.WaypointSymbolMode.CUSTOM) {
            return MarketConfig.CUSTOM_WAYPOINT_SYMBOL.get();
        }
        return "🛒"; // tryb ICON (domyślny) — ikona koszyka, zgodnie z configiem
    }
}
