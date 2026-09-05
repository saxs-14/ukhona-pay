package co.za.ukhonapay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record VendorBankWithdrawalRequest(
        @NotBlank(message = "Bank name is required") String bankName,
        @NotBlank(message = "Account number is required") String accountNumber,
        @NotBlank(message = "Account holder name is required") String accountHolderName,
        @NotNull @DecimalMin(value = "1.00", message = "Minimum withdrawal amount is R1.00") BigDecimal amount,
        @NotBlank(message = "PIN is required") String pin
) {
}
