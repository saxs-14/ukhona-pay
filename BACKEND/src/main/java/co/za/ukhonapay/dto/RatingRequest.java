package co.za.ukhonapay.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RatingRequest(
        @NotNull Long vendorId,
        Long transactionId,
        @Min(1) @Max(5) short stars,
        String review
) {
}
