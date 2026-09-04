package co.za.ukhonapay.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AnalyticsResponse(
        long totalTransactions,
        BigDecimal totalVolume,
        BigDecimal totalCashback,
        long activeVendors,
        List<TopVendor> topVendors,
        Map<String, Long> categoryBreakdown
) {
    public record TopVendor(String businessName, long transactionCount) {
    }
}
