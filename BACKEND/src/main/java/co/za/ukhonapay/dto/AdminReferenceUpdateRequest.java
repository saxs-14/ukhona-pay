package co.za.ukhonapay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

// Shared by taxi-association and taxi-rank admin updates - locationName is
// ignored for associations (they don't have one), duesAmount is ignored for
// ranks (only an association charges membership dues).
public record AdminReferenceUpdateRequest(
        @NotBlank String name,
        String locationName,
        @DecimalMin(value = "0.00", message = "duesAmount cannot be negative") BigDecimal duesAmount
) {
}
