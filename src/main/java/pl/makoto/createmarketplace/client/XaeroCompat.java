package pl.makoto.createmarketplace.client;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import pl.makoto.createmarketplace.MarketConfig;

import java.util.List;

public class XaeroCompat {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Cached reflection objects — initialized once
    private static boolean initialized = false;
    private static boolean available = false;
    private static Class<?> waypointClass;
    private static java.lang.reflect.Constructor<?> waypointConstructor;
    private static java.lang.reflect.Method getCurrentSessionMethod;
    private static java.lang.reflect.Method getWaypointsManagerMethod;
    private static java.lang.reflect.Method getWaypointsMethod;
    private static java.lang.reflect.Method getListMethod;
    private static java.lang.reflect.Method saveWaypointsMethod;

    private static synchronized void initReflection() {
        if (initialized) return;
        initialized = true;
        try {
            waypointClass = Class.forName("xaero.common.minimap.waypoints.Waypoint");
            waypointConstructor = waypointClass.getConstructor(
                int.class, int.class, int.class, String.class, String.class, int.class, int.class, boolean.class);

            Class<?> sessionClass = Class.forName("xaero.common.XaeroMinimapSession");
            getCurrentSessionMethod = sessionClass.getMethod("getCurrentSession");

            available = true;
        } catch (Exception e) {
            LOGGER.warn("Xaero's Minimap reflection init failed — waypoints disabled", e);
            available = false;
        }
    }

    public static void addWaypoint(String name, int x, int y, int z) {
        if (!ModList.get().isLoaded("xaerominimap")) return;
        initReflection();
        if (!available) return;

        try {
            String symbol = resolveSymbol(name);
            Object waypoint = waypointConstructor.newInstance(x, y, z, name, symbol, 1, 0, false);

            Object session = getCurrentSessionMethod.invoke(null);
            if (session == null) return;

            if (getWaypointsManagerMethod == null) {
                getWaypointsManagerMethod = session.getClass().getMethod("getWaypointsManager");
            }
            Object waypointManager = getWaypointsManagerMethod.invoke(session);

            if (getWaypointsMethod == null) {
                getWaypointsMethod = waypointManager.getClass().getMethod("getWaypoints");
            }
            Object waypointSet = getWaypointsMethod.invoke(waypointManager);

            if (getListMethod == null) {
                getListMethod = waypointSet.getClass().getMethod("getList");
            }
            @SuppressWarnings("unchecked")
            List<Object> list = (List<Object>) getListMethod.invoke(waypointSet);
            list.add(waypoint);

            if (saveWaypointsMethod == null) {
                saveWaypointsMethod = waypointManager.getClass().getMethod("saveWaypoints");
            }
            saveWaypointsMethod.invoke(waypointManager);
        } catch (Exception e) {
            LOGGER.error("Failed to add Xaero waypoint", e);
        }
    }

    private static String resolveSymbol(String name) {
        MarketConfig.WaypointSymbolMode mode = MarketConfig.WAYPOINT_SYMBOL_MODE.get();
        if (mode == MarketConfig.WaypointSymbolMode.FIRST_LETTER && name != null && !name.isEmpty()) {
            return name.substring(0, 1).toUpperCase();
        } else if (mode == MarketConfig.WaypointSymbolMode.CUSTOM) {
            return MarketConfig.CUSTOM_WAYPOINT_SYMBOL.get();
        }
        return "🛒";
    }
}
