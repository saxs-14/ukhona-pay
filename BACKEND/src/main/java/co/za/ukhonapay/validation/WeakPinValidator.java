package co.za.ukhonapay.validation;

import java.util.Set;

// Rejects the small set of 4-digit PINs that account for a disproportionate share
// of real-world PIN guesses (all-same-digit, sequential runs, and common
// human-picked patterns like a year or "2580" - a straight vertical ATM keypad
// line). Not a full strength check - just cheap, well-known weak picks.
public final class WeakPinValidator {

    private static final Set<String> WEAK_PINS = Set.of(
            "0000", "1111", "2222", "3333", "4444", "5555", "6666", "7777", "8888", "9999",
            "1234", "2345", "3456", "4567", "5678", "6789", "9876", "8765", "7654", "6543", "5432", "4321",
            "0123", "1230", "2580", "1212", "1122", "1004", "2001", "2000");

    private WeakPinValidator() {}

    public static boolean isWeak(String pin) {
        return pin != null && WEAK_PINS.contains(pin);
    }
}
