package pl.makoto.createmarketplace;

import net.neoforged.neoforge.common.ModConfigSpec;

public class MarketConfig {
    public static final ModConfigSpec COMMON_SPEC;
    
    public static final ModConfigSpec.EnumValue<WaypointSymbolMode> WAYPOINT_SYMBOL_MODE;
    public static final ModConfigSpec.ConfigValue<String> CUSTOM_WAYPOINT_SYMBOL;
    public static final ModConfigSpec.BooleanValue USE_CARD_DURABILITY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Settings for Create: Marketplace").push("general");

        WAYPOINT_SYMBOL_MODE = builder
                .comment("Waypoint symbol display mode (Xaero's Minimap)",
                         "ICON - Uses a shopping cart icon (🛒)",
                         "FIRST_LETTER - Uses the first letter of the shop name",
                         "CUSTOM - Uses a custom symbol defined below")
                .defineEnum("waypointSymbolMode", WaypointSymbolMode.ICON);

        CUSTOM_WAYPOINT_SYMBOL = builder
                .comment("Custom waypoint symbol (used only if waypointSymbolMode is set to CUSTOM)")
                .define("customWaypointSymbol", "S");

        USE_CARD_DURABILITY = builder
                .comment("Should the Registration Card lose durability on use?",
                         "If false, the card has infinite uses.")
                .define("useCardDurability", true);

        builder.pop();
        COMMON_SPEC = builder.build();
    }

    public enum WaypointSymbolMode {
        ICON,
        FIRST_LETTER,
        CUSTOM
    }
}
