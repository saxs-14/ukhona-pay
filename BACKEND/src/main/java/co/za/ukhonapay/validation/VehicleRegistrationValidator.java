package co.za.ukhonapay.validation;

import java.util.regex.Pattern;

// Validates the shape of a South African vehicle registration/number plate.
// SA plates aren't one uniform format - current-issue provincial plates look like
// "DX 45 FG MP" (2-3 letters, 2-6 digits, up to 4 trailing letters incl. the
// province code: GP/MP/LP/NW/FS/NC/WC/EC/KZN...), while pre-1998 city-issued
// plates still legally on the road (Cape Town "CA 123-456", Durban "ND 123-456")
// are 2 letters followed by up to 6 digits with no trailing letters at all.
// Rather than hard-code an exhaustive, possibly-incomplete province-code list and
// risk rejecting a genuine plate, this matches the common structural shape shared
// by both eras: letters, then digits, then optionally more letters.
public final class VehicleRegistrationValidator {

    private static final Pattern SHAPE = Pattern.compile("^[A-Z]{2,3}[0-9]{2,6}[A-Z]{0,4}$");

    private VehicleRegistrationValidator() {}

    public static boolean isValid(String rawInput) {
        if (rawInput == null) {
            return false;
        }
        String normalized = normalize(rawInput);
        return SHAPE.matcher(normalized).matches();
    }

    public static String normalize(String rawInput) {
        return rawInput.toUpperCase().replaceAll("[\\s-]", "");
    }
}
