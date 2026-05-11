package pl.makoto.createmarketplace.client.integration;

import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.screen.EmiScreenManager;
import net.neoforged.fml.ModList;

/**
 * Klasa pomocnicza do bezpiecznej integracji z EMI.
 * Wszystkie odwołania do klas EMI powinny znajdować się tutaj lub być wywoływane dopiero po sprawdzeniu obecności moda.
 */
public class MarketEmiCompat {

    /**
     * Oblicza przesunięcie w osi X na podstawie widocznych przycisków EMI.
     * @return Przesunięcie w pikselach (np. 0, 22 lub 44).
     */
    public static int getEmiOffset() {
        if (ModList.get().isLoaded("emi")) {
            return getOffsetInternal();
        }
        return 0;
    }

    private static int getOffsetInternal() {
        try {
            int offset = 0;
            // Sprawdzamy, czy EMI jest aktywne (nie wyłączone w configu lub podczas ładowania)
            boolean active = !EmiScreenManager.isDisabled();

            // Sprawdzamy widoczność przycisku konfiguracji
            if (EmiConfig.emiConfigButtonVisibility.resolve(active)) {
                offset += 22;
            }
            // Sprawdzamy widoczność przycisku drzewa przepisów
            if (EmiConfig.recipeTreeButtonVisibility.resolve(active)) {
                offset += 22;
            }
            return offset;
        } catch (Throwable t) {
            // W razie jakichkolwiek problemów z kompatybilnością (np. zmiana pól w EMI), zwracamy 0
            return 0;
        }
    }
}
