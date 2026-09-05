package co.za.ukhonapay.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AssociationTransferResponse(
        String reference,
        Long associationId,
        String associationName,
        BigDecimal amount,
        BigDecimal newWalletBalance,
        LocalDateTime timestamp
) {
}
