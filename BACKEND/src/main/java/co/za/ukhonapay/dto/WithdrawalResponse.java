package co.za.ukhonapay.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WithdrawalResponse(
        Long id,
        BigDecimal amount,
        String withdrawalPin,
        String atmName,
        String status,
        LocalDateTime requestedAt,
        LocalDateTime expiresAt,
        LocalDateTime completedAt
) {
}
