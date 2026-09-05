package co.za.ukhonapay.dto;

public record BankAccountResponse(
        String accountHolderName,
        String bankName,
        String maskedAccountNumber,
        String branchCode
) {
}
