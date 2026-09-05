package co.za.ukhonapay.dto;

import java.time.LocalDateTime;

public record RatingResponse(
        String reviewerName,
        short stars,
        String review,
        LocalDateTime createdAt
) {
}
