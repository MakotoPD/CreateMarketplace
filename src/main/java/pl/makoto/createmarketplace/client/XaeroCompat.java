package pl.makoto.createmarketplace.client;

import net.neoforged.fml.ModList;
import pl.makoto.createmarketplace.MarketConfig;

public class XaeroCompat {
    public static void addWaypoint(String name, int x, int y, int z) {
        // Sprawdzamy, czy Xaero's Minimap jest załadowana
        if (!ModList.get().isLoaded("xaerominimap")) {
            System.err.println("Xaero's Minimap nie jest zainstalowana. Omijanie dodawania waypointu.");
            return;
        }

        try {
            // Używamy refleksji aby nie wymagać modyfikacji na etapie kompilacji (hard-dependency).
            // To symuluje kod:
            // Waypoint w = new Waypoint(x, y, z, name, symbol, color, globalType, temporary);
            // XaeroMinimapSession.getCurrentSession().getWaypointsManager().getWaypoints().getList().add(w);
            
            Class<?> waypointClass = Class.forName("xaero.common.minimap.waypoints.Waypoint");
            
            String symbol = "🛒";
            MarketConfig.WaypointSymbolMode mode = MarketConfig.WAYPOINT_SYMBOL_MODE.get();
            if (mode == MarketConfig.WaypointSymbolMode.FIRST_LETTER && name != null && !name.isEmpty()) {
                symbol = name.substring(0, 1).toUpperCase();
            } else if (mode == MarketConfig.WaypointSymbolMode.CUSTOM) {
                symbol = MarketConfig.CUSTOM_WAYPOINT_SYMBOL.get();
            }
            
            Object waypoint = waypointClass.getConstructor(
                    int.class, int.class, int.class, String.class, String.class, int.class, int.class, boolean.class
            ).newInstance(x, y, z, name, symbol, 1, 0, false);

            Class<?> sessionClass = Class.forName("xaero.common.XaeroMinimapSession");
            Object session = sessionClass.getMethod("getCurrentSession").invoke(null);
            
            if (session == null) {
                return; // Gracz nie jest w grze lub sesja nie wystartowała
            }
            
            Object waypointManager = session.getClass().getMethod("getWaypointsManager").invoke(session);
            Object waypointSet = waypointManager.getClass().getMethod("getWaypoints").invoke(waypointManager); 

            java.util.List<Object> list = (java.util.List<Object>) waypointSet.getClass().getMethod("getList").invoke(waypointSet);
            list.add(waypoint);

            // Wymuszenie zapisu
            waypointManager.getClass().getMethod("saveWaypoints").invoke(waypointManager);
            
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Błąd podczas dodawania Waypointu przez API Xaero's Minimap.");
        }
    }
}
