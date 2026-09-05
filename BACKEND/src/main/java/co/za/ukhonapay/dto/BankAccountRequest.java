package co.za.ukhonapay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BankAccountRequest(
        @NotBlank String accountHolderName,
        @NotBlank String bankName,
        @NotBlank @Pattern(regexp = "^[0-9]{6,20}$", message = "account number must be 6-20 digits") String accountNumber,
        @NotBlank @Pattern(regexp = "^[0-9]{5,10}$", message = "branch code must be 5-10 digits") String branchCode
) {
}
