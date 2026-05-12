package pl.makoto.createmarketplace.network;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for ServerPayloadHandler.sanitizeShopName().
 * Validates Requirements 3.1, 3.2, 3.3.
 */
class SanitizeShopNamePropertyTest {

    private static final int MAX_SHOP_NAME_LENGTH = 32;

    /**
     * Property 3: Shop names exceeding max length are rejected.
     *
     * For any string with length greater than 32 characters (after stripping § formatting codes),
     * the shop name validation SHALL reject the input and return null.
     *
     * Validates: Requirements 3.1
     */
    @Property
    void shopNamesExceedingMaxLengthAreRejected(@ForAll("stringsExceedingMaxLength") String input) {
        String result = ServerPayloadHandler.sanitizeShopName(input);
        assertNull(result, "Names exceeding 32 characters after stripping should be rejected (return null)");
    }

    @Provide
    Arbitrary<String> stringsExceedingMaxLength() {
        // Generate strings that, after stripping § codes and trimming, exceed 32 characters
        return Arbitraries.strings()
                .ofMinLength(MAX_SHOP_NAME_LENGTH + 1)
                .ofMaxLength(100)
                .alpha()
                .map(s -> {
                    // Ensure the string is at least 33 printable non-whitespace chars
                    if (s.length() <= MAX_SHOP_NAME_LENGTH) {
                        s = s + "a".repeat(MAX_SHOP_NAME_LENGTH + 1 - s.length());
                    }
                    return s;
                });
    }

    /**
     * Property 4: Whitespace-only shop names are rejected.
     *
     * For any string composed entirely of whitespace characters (including the empty string),
     * the shop name validation SHALL reject the input and return null.
     *
     * Validates: Requirements 3.2
     */
    @Property
    void whitespaceOnlyShopNamesAreRejected(@ForAll("whitespaceOnlyStrings") String input) {
        String result = ServerPayloadHandler.sanitizeShopName(input);
        assertNull(result, "Whitespace-only names (including empty) should be rejected (return null)");
    }

    @Provide
    Arbitrary<String> whitespaceOnlyStrings() {
        // Generate strings composed entirely of whitespace characters
        return Arbitraries.of(' ', '\t', '\n', '\r', '\f')
                .list()
                .ofMinSize(0)
                .ofMaxSize(50)
                .map(chars -> {
                    StringBuilder sb = new StringBuilder();
                    for (char c : chars) {
                        sb.append(c);
                    }
                    return sb.toString();
                });
    }

    /**
     * Property 5: Section sign formatting codes are stripped.
     *
     * For any input string, the sanitized shop name SHALL not contain any section sign (§) characters.
     * Furthermore, for any string where removing all §X sequences (§ followed by one character) yields
     * a non-empty, ≤32-character result, the sanitized output SHALL equal that stripped and trimmed string.
     *
     * Validates: Requirements 3.3
     */
    @Property
    void sectionSignFormattingCodesAreStripped(@ForAll("stringsWithFormattingCodes") String input) {
        String result = ServerPayloadHandler.sanitizeShopName(input);

        // Compute expected: strip §X sequences, then trim
        String stripped = input.replaceAll("§.", "");
        String expected = stripped.trim();

        if (expected.isEmpty() || expected.length() > MAX_SHOP_NAME_LENGTH) {
            // Should be rejected
            assertNull(result, "After stripping formatting codes, empty or too-long names should be rejected");
        } else {
            // Should return the stripped and trimmed result without any § characters
            assertNotNull(result, "Valid names after stripping should not be null");
            assertFalse(result.contains("§"), "Sanitized name must not contain § characters");
            assertEquals(expected, result, "Sanitized name should equal the stripped and trimmed input");
        }
    }

    @Provide
    Arbitrary<String> stringsWithFormattingCodes() {
        // Generate strings that contain § formatting codes interspersed with normal text
        Arbitrary<String> normalText = Arbitraries.strings()
                .ofMinLength(1)
                .ofMaxLength(10)
                .alpha();

        Arbitrary<String> formattingCode = Arbitraries.of(
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'a', 'b', 'c', 'd', 'e', 'f',
                'k', 'l', 'm', 'n', 'o', 'r'
        ).map(c -> "§" + c);

        // Mix normal text segments with formatting codes
        return Combinators.combine(normalText, formattingCode, normalText)
                .as((before, code, after) -> before + code + after)
                .flatMap(base -> Arbitraries.of(
                        base,
                        "§a" + base,
                        base + "§r",
                        "§4§l" + base + "§r",
                        "  §c" + base + "§r  "
                ));
    }
}
