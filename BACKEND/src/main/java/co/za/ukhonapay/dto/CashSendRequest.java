package co.za.ukhonapay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CashSendRequest(
        @NotBlank(message = "Recipient phone number is required")
        String recipientPhone,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "10.00", message = "Minimum CashSend amount is R10.00")
        BigDecimal amount,

        @NotBlank(message = "4-digit CashSend withdrawal PIN is required")
        @Size(min = 4, max = 4, message = "Withdrawal PIN must be 4 digits")
        String cashSendPin,

        @NotBlank(message = "Account security PIN is required")
        @Size(min = 4, max = 4, message = "Account PIN must be 4 digits")
        String accountPin
) {
}
