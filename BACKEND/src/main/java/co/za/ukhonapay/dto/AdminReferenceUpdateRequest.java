package co.za.ukhonapay.dto;

import jakarta.validation.constraints.NotBlank;

// Shared by taxi-association and taxi-rank admin updates - locationName is
// ignored for associations (they don't have one).
public record AdminReferenceUpdateRequest(
        @NotBlank String name,
        String locationName
) {
}
