package co.za.ukhonapay.dto;

import java.math.BigDecimal;

public record AssociationWalletResponse(
        Long associationId,
        String associationName,
        BigDecimal balance,
        String currency
) {
}
