package pl.makoto.createmarketplace.data;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kontrakt {@link StockInfo} — na nim opiera się cały znacznik "aktywny / pusty".
 *
 * Najważniejsza własność: odczyt, który NIE jest świeży, nigdy nie może uchodzić
 * za potwierdzony. To ta pomyłka wysyła gracza przez pół mapy po towar, którego
 * już nie ma.
 */
class StockInfoPropertyTest {

    private static StockInfo aged(int units, int ageSeconds) {
        return new StockInfo(units, System.currentTimeMillis() - ageSeconds * 1000L);
    }

    /** Nieznany stan nigdy nie jest świeży ani pusty — nie wolno go pokazać jako potwierdzony. */
    @Property
    void unknownIsNeverFreshAndNeverEmpty(@ForAll @IntRange(min = 0, max = 100000) int age) {
        StockInfo info = aged(StockInfo.UNKNOWN, age);

        assertFalse(info.known(), "UNKNOWN nie jest znanym odczytem");
        assertFalse(info.fresh(), "UNKNOWN nigdy nie moze byc swiezy");
        assertFalse(info.empty(), "UNKNOWN to nie to samo co pusty sklep");
        assertEquals(0, info.ageSeconds(), "wiek nieznanego odczytu nie ma sensu i musi byc zerowy");
    }

    /** Poza oknem świeżości odczyt traci status potwierdzonego, choć nadal jest znany. */
    @Property
    void readingsGoStaleExactlyAtTheFreshnessWindow(
            @ForAll @IntRange(min = 0, max = 4096) int units,
            @ForAll @IntRange(min = 0, max = 100000) int age
    ) {
        StockInfo info = aged(units, age);

        assertTrue(info.known(), "kazdy nieujemny stan jest znany");
        // tolerancja 1 s na przejscie zegara miedzy konstrukcja a odczytem
        if (age > StockInfo.FRESH_SECONDS + 1) {
            assertFalse(info.fresh(), "odczyt starszy niz okno swiezosci nie moze byc potwierdzony");
        } else if (age < StockInfo.FRESH_SECONDS) {
            assertTrue(info.fresh(), "odczyt w oknie swiezosci jest potwierdzony");
        }
    }

    /** INFINITE to sklep bez magazynu — znany, ale nigdy pusty, niezależnie od wieku. */
    @Property
    void infiniteIsKnownButNeverEmpty(@ForAll @IntRange(min = 0, max = 100000) int age) {
        StockInfo info = aged(StockInfo.INFINITE, age);

        assertTrue(info.known());
        assertTrue(info.infinite());
        assertFalse(info.empty(), "sklep bez magazynu nigdy nie jest pusty");
    }

    /** Zero to jedyna wartość oznaczająca potwierdzony brak towaru. */
    @Property
    void onlyZeroMeansEmpty(@ForAll @IntRange(min = -2, max = 4096) int units) {
        StockInfo info = aged(units, 0);
        assertEquals(units == 0, info.empty(),
                "empty() musi byc prawda dokladnie dla zera, nigdy dla sentineli");
    }

    /** Wiek nigdy nie jest ujemny, nawet gdy znacznik czasu wskazuje przyszłość. */
    @Property
    void ageIsNeverNegative(@ForAll @IntRange(min = 1, max = 100000) int secondsIntoFuture) {
        StockInfo info = new StockInfo(5, System.currentTimeMillis() + secondsIntoFuture * 1000L);
        assertTrue(info.ageSeconds() >= 0, "ujemny wiek zepsulby formatowanie dymka");
    }
}
