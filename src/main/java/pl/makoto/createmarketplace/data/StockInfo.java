package pl.makoto.createmarketplace.data;

/**
 * Stan magazynowy jednej oferty — ile jeszcze transakcji da się w niej wykonać.
 *
 * Celowo NIE jest częścią {@link MarketOffer}: oferta jest zapisywana w świecie,
 * a stan magazynu dezaktualizuje się przy pierwszym restarcie serwera. Trzymamy
 * go wyłącznie w pamięci i przesyłamy obok listy ofert.
 *
 * <p>{@code units} niesie trzy znaczenia: {@link #UNKNOWN} — nie dało się sprawdzić
 * (chunk wyładowany, nieznany typ sklepu), {@link #INFINITE} — sklep bez magazynu
 * (Server Vendor, kreatywny vendor Numismatics), {@code >= 0} — liczba możliwych
 * transakcji, gdzie {@code 0} znaczy pusty.
 *
 * <p>{@code checkedAt} to znacznik czasu w milisekundach lokalnych dla strony,
 * która obiekt trzyma. Serwer wpisuje własny czas; przy wysyłce zamieniamy go na
 * wiek w sekundach, a klient odtwarza z niego znacznik na SWOIM zegarze — dzięki
 * temu różnica zegarów klient/serwer nigdy nie zafałszuje wieku odczytu.
 */
public record StockInfo(int units, long checkedAt) {

    public static final int UNKNOWN = -1;
    public static final int INFINITE = -2;

    /** Odczyt starszy niż tyle sekund przestaje być traktowany jako potwierdzony. */
    public static final int FRESH_SECONDS = 60;

    public static final StockInfo NONE = new StockInfo(UNKNOWN, 0L);

    public static StockInfo of(int units) {
        return new StockInfo(units, System.currentTimeMillis());
    }

    public boolean known()    { return units != UNKNOWN; }
    public boolean infinite() { return units == INFINITE; }
    public boolean empty()    { return units == 0; }

    /** Wiek odczytu w sekundach; rośnie sam, gdy ekran zostaje otwarty. */
    public int ageSeconds() {
        if (!known()) return 0;
        return (int) Math.max(0L, (System.currentTimeMillis() - checkedAt) / 1000L);
    }

    /** Świeży = sprawdzony na tyle niedawno, że można go pokazać jako pewny. */
    public boolean fresh() {
        return known() && ageSeconds() <= FRESH_SECONDS;
    }
}
