package pl.makoto.createmarketplace.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import pl.makoto.createmarketplace.data.MarketOffer;

/**
 * Bazowa klasa dla eventów związanych z ofertami rynkowymi.
 */
public abstract class MarketOfferEvent extends Event {
    private final MarketOffer offer;
    private final ServerPlayer player;

    protected MarketOfferEvent(MarketOffer offer, ServerPlayer player) {
        this.offer = offer;
        this.player = player;
    }

    public MarketOffer getOffer() {
        return offer;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    /**
     * Wywoływany tuż przed zarejestrowaniem nowej oferty. Pozwala na anulowanie rejestracji.
     */
    public static class Register extends MarketOfferEvent implements ICancellableEvent {
        public Register(MarketOffer offer, ServerPlayer player) {
            super(offer, player);
        }
    }

    /**
     * Wywoływany po pomyślnym zarejestrowaniu oferty w bazie danych.
     */
    public static class Registered extends MarketOfferEvent {
        public Registered(MarketOffer offer, ServerPlayer player) {
            super(offer, player);
        }
    }

    /**
     * Wywoływany po usunięciu oferty z bazy danych.
     */
    public static class Removed extends MarketOfferEvent {
        public Removed(MarketOffer offer, ServerPlayer player) {
            super(offer, player);
        }
    }
}
