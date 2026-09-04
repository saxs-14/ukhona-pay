package co.za.ukhonapay.validation;

import java.time.LocalDate;
import java.time.YearMonth;

// Validates the structure of a South African 13-digit ID number: YYMMDD SSSS C A Z
// (date of birth, gender sequence, citizenship, historical race marker, Luhn check
// digit). Deliberately does not enforce an age minimum - that's a business rule for
// the caller, not a format rule.
public final class SouthAfricanIdValidator {

    private SouthAfricanIdValidator() {}

    public static boolean isValid(String id) {
        if (id == null || !id.matches("^[0-9]{13}$")) {
            return false;
        }
        if (!hasValidBirthDate(id)) {
            return false;
        }
        char citizenship = id.charAt(10);
        if (citizenship != '0' && citizenship != '1') {
            return false;
        }
        return hasValidCheckDigit(id);
    }

    private static boolean hasValidBirthDate(String id) {
        int yy = Integer.parseInt(id.substring(0, 2));
        int mm = Integer.parseInt(id.substring(2, 4));
        int dd = Integer.parseInt(id.substring(4, 6));
        if (mm < 1 || mm > 12) {
            return false;
        }
        // SA ID numbers don't encode the century - assume 1900s unless that would
        // put the birth year in the future, in which case it must be 2000s.
        int currentYearTwoDigit = LocalDate.now().getYear() % 100;
        int fullYear = (yy <= currentYearTwoDigit) ? 2000 + yy : 1900 + yy;
        int daysInMonth = YearMonth.of(fullYear, mm).lengthOfMonth();
        return dd >= 1 && dd <= daysInMonth;
    }

    private static boolean hasValidCheckDigit(String id) {
        int oddSum = 0;
        for (int i = 0; i < 12; i += 2) {
            oddSum += id.charAt(i) - '0';
        }

        StringBuilder evenDigits = new StringBuilder();
        for (int i = 1; i < 12; i += 2) {
            evenDigits.append(id.charAt(i));
        }
        long doubled = Long.parseLong(evenDigits.toString()) * 2;
        int evenSum = 0;
        for (char c : String.valueOf(doubled).toCharArray()) {
            evenSum += c - '0';
        }

        int total = oddSum + evenSum;
        int expectedCheckDigit = (10 - (total % 10)) % 10;
        int actualCheckDigit = id.charAt(12) - '0';
        return expectedCheckDigit == actualCheckDigit;
    }
}
