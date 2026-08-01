package pl.makoto.createmarketplace;

import net.neoforged.neoforge.common.ModConfigSpec;

public class MarketConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;
    
    // Common
    public static final ModConfigSpec.EnumValue<WaypointSymbolMode> WAYPOINT_SYMBOL_MODE;
    public static final ModConfigSpec.ConfigValue<String> CUSTOM_WAYPOINT_SYMBOL;
    public static final ModConfigSpec.BooleanValue USE_CARD_DURABILITY;
    public static final ModConfigSpec.IntValue MAX_OFFERS_PER_PLAYER;
    public static final ModConfigSpec.IntValue STOCK_CHECK_INTERVAL;
    public static final ModConfigSpec.BooleanValue REMOVE_DEAD_OFFERS;

    // Client
    public static final ModConfigSpec.EnumValue<ButtonPosition> BUTTON_POSITION;
    public static final ModConfigSpec.IntValue CUSTOM_BUTTON_X;
    public static final ModConfigSpec.IntValue CUSTOM_BUTTON_Y;

    static {
        ModConfigSpec.Builder commonBuilder = new ModConfigSpec.Builder();

        commonBuilder.comment("Settings for Create: Marketplace").push("general");

        WAYPOINT_SYMBOL_MODE = commonBuilder
                .comment("Waypoint symbol display mode (Xaero's Minimap)",
                         "ICON - Uses a shopping cart icon (🛒)",
                         "FIRST_LETTER - Uses the first letter of the shop name",
                         "CUSTOM - Uses a custom symbol defined below")
                .defineEnum("waypointSymbolMode", WaypointSymbolMode.ICON);

        CUSTOM_WAYPOINT_SYMBOL = commonBuilder
                .comment("Custom waypoint symbol (used only if waypointSymbolMode is set to CUSTOM)")
                .define("customWaypointSymbol", "S");

        USE_CARD_DURABILITY = commonBuilder
                .comment("Should the Registration Card lose durability on use?",
                         "If false, the card has infinite uses.")
                .define("useCardDurability", true);

        MAX_OFFERS_PER_PLAYER = commonBuilder
                .comment("Maximum number of marketplace offers a single player can have.",
                         "Set to a high value to effectively disable the limit.")
                .defineInRange("maxOffersPerPlayer", 100, 1, 10000);

        STOCK_CHECK_INTERVAL = commonBuilder
                .comment("How often (in seconds) the server refreshes shop stock levels.",
                         "Only shops in currently loaded chunks are checked - shops elsewhere",
                         "keep their last known reading, which the GUI labels with its age.",
                         "Lower values mean fresher data at a slightly higher cost.")
                .defineInRange("stockCheckIntervalSeconds", 30, 5, 600);

        REMOVE_DEAD_OFFERS = commonBuilder
                .comment("Automatically delete listings whose shop block no longer exists.",
                         "Detected during the stock check, so it only ever triggers for blocks",
                         "in loaded chunks - a shop in an unloaded chunk is never touched.",
                         "When false, such listings stay on the market but are shown as empty.")
                .define("removeDeadOffers", false);

        commonBuilder.pop();
        COMMON_SPEC = commonBuilder.build();

        ModConfigSpec.Builder clientBuilder = new ModConfigSpec.Builder();
        clientBuilder.comment("Client-side settings for Create: Marketplace").push("appearance");

        BUTTON_POSITION = clientBuilder
                .comment("Where the Marketplace button should appear when EMI is not installed.",
                        "CORNER - Bottom-left corner (default)",
                        "INVENTORY_SIDE - Left side of the inventory",
                        "CUSTOM - Use custom coordinates")
                .defineEnum("buttonPosition", ButtonPosition.CORNER);

        CUSTOM_BUTTON_X = clientBuilder
                .comment("Custom X coordinate for the button (used only if buttonPosition is CUSTOM)")
                .defineInRange("customButtonX", 2, 0, 4000);

        CUSTOM_BUTTON_Y = clientBuilder
                .comment("Custom Y coordinate for the button (used only if buttonPosition is CUSTOM)")
                .defineInRange("customButtonY", 2, 0, 4000);

        clientBuilder.pop();
        CLIENT_SPEC = clientBuilder.build();
    }

    public enum WaypointSymbolMode {
        ICON,
        FIRST_LETTER,
        CUSTOM
    }

    public enum ButtonPosition {
        CORNER,
        INVENTORY_SIDE,
        CUSTOM
    }
}
