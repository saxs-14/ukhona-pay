package co.za.ukhonapay.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        String reference,
        Long senderId,
        String senderName,
        Long receiverId,
        String receiverName,
        String vendorCategory,
        BigDecimal amount,
        BigDecimal cashbackAmount,
        String status,
        String description,
        LocalDateTime createdAt,
        String direction // "SENT" or "RECEIVED" relative to requesting user
) {
}
