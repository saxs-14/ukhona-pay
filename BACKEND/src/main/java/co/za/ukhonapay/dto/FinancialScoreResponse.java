package co.za.ukhonapay.dto;

import java.math.BigDecimal;

/**
 * Credit-readiness summary derived entirely from a trader's existing
 * transaction history - the "financial identity" layer: turning recorded
 * cash-equivalent income into evidence a lender could act on. The exact
 * scoring weights are a product/business rule to validate, not an
 * established credit-bureau standard.
 */
public record FinancialScoreResponse(
        int financialScore,
        int windowDays,
        int daysRecorded,
        double consistencyPercentage,
        BigDecimal totalIncome,
        long transactionCount,
        boolean creditEligible,
        BigDecimal recommendedMin,
        BigDecimal recommendedMax,
        String reason
) {
}
