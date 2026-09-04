package co.za.ukhonapay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaxiRankRequest(
        @NotBlank @Size(min = 3, max = 150, message = "must be 3-150 characters") String name,
        String locationName
) {
}
