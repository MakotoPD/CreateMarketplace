package pl.makoto.createmarketplace.client.integration;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class MarketButton extends Button {

    public MarketButton(int x, int y, OnPress onPress) {
        super(x, y, 20, 20, Component.literal("🛒"), onPress, DEFAULT_NARRATION);
    }
}
