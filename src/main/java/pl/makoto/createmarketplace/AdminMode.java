package pl.makoto.createmarketplace;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tryb administratora (serwerowy, w pamięci). Gdy gracz jest w trybie admina,
 * rejestrowane przez niego sklepy należą do "Serwera", a nie do gracza.
 * Stan jest przejściowy — czyszczony przy wylogowaniu (admin musi włączyć go
 * od nowa po wejściu).
 */
public final class AdminMode {
    private AdminMode() {}

    /** właściciel "Serwer" — stały, zerowy UUID */
    public static final UUID SERVER_UUID = new UUID(0L, 0L);
    public static final String SERVER_NAME = "Serwer";

    private static final Set<UUID> ADMINS = ConcurrentHashMap.newKeySet();

    public static boolean isAdmin(UUID id) {
        return ADMINS.contains(id);
    }

    /** przełącza i zwraca nowy stan */
    public static boolean toggle(UUID id) {
        if (ADMINS.add(id)) return true;
        ADMINS.remove(id);
        return false;
    }

    public static void set(UUID id, boolean on) {
        if (on) ADMINS.add(id);
        else ADMINS.remove(id);
    }

    public static void clear(UUID id) {
        ADMINS.remove(id);
    }
}
