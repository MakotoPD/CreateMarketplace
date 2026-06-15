package pl.makoto.createmarketplace.client;

/** Klientowy cień serwerowego trybu administratora (synchronizowany pakietem). */
public final class ClientAdminState {
    private ClientAdminState() {}

    private static volatile boolean active = false;

    public static boolean isActive() {
        return active;
    }

    public static void set(boolean value) {
        active = value;
    }
}
