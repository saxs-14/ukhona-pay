package co.za.ukhonapay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTaxiRankRequest(
        @NotBlank @Size(min = 3, max = 150, message = "must be 3-150 characters") String name,
        String locationName,
        @NotNull(message = "Choose which taxi association this rank falls under") Long associationId
) {
}
