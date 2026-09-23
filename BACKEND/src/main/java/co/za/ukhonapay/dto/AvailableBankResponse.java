package co.za.ukhonapay.dto;

public record AvailableBankResponse(
        String bankGroupId,
        String bankGroupName,
        String universalBranchCode
) {}