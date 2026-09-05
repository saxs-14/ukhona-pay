package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.AnalyticsResponse;
import co.za.ukhonapay.dto.FinancialScoreResponse;
import co.za.ukhonapay.model.Transaction;
import co.za.ukhonapay.model.Vendor;
import co.za.ukhonapay.repository.TransactionRepository;
import co.za.ukhonapay.repository.VendorRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Platform-wide analytics (demo dashboard) plus the per-vendor "Data Analytics
 * for Small Businesses" insight tile: derived entirely from transactions already
 * captured by the payment flow, no extra data entry required from the vendor.
 */
@Service
public class AnalyticsService {

    private final TransactionRepository transactionRepository;
    private final VendorRepository vendorRepository;

    public AnalyticsService(TransactionRepository transactionRepository, VendorRepository vendorRepository) {
        this.transactionRepository = transactionRepository;
        this.vendorRepository = vendorRepository;
    }

    public AnalyticsResponse platformAnalytics() {
        List<Transaction> all = transactionRepository.findAll();

        BigDecimal totalVolume = all.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCashback = all.stream().map(Transaction::getCashbackAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPlatformFees = all.stream().map(Transaction::getPlatformFee).reduce(BigDecimal.ZERO, BigDecimal::add);
        long activeVendors = vendorRepository.count();

        Map<Long, Long> countByVendor = all.stream()
                .filter(t -> t.getVendorId() != null)
                .collect(Collectors.groupingBy(Transaction::getVendorId, Collectors.counting()));

        List<AnalyticsResponse.TopVendor> topVendors = countByVendor.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    String name = vendorRepository.findById(e.getKey()).map(Vendor::getBusinessName).orElse("Unknown");
                    return new AnalyticsResponse.TopVendor(name, e.getValue());
                })
                .toList();

        Map<String, Long> categoryBreakdown = all.stream()
                .filter(t -> t.getVendorId() != null)
                .map(t -> vendorRepository.findById(t.getVendorId()).map(v -> v.getCategory().name()).orElse("OTHER"))
                .collect(Collectors.groupingBy(c -> c, LinkedHashMap::new, Collectors.counting()));

        return new AnalyticsResponse(all.size(), totalVolume, totalCashback, totalPlatformFees, activeVendors, topVendors, categoryBreakdown);
    }

    public Map<String, Object> vendorAnalytics(Long vendorId) {
        List<Transaction> received = transactionRepository.findByVendorIdOrderByCreatedAtDesc(vendorId);

        BigDecimal totalEarned = received.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Integer, BigDecimal> byHour = received.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCreatedAt().getHour(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));

        int peakHour = byHour.entrySet().stream()
                .max(Comparator.comparing(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(-1);

        List<Map<String, Object>> earningsByHour = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("hour", hour);
            point.put("total", byHour.getOrDefault(hour, BigDecimal.ZERO));
            earningsByHour.add(point);
        }

        Map<LocalDate, BigDecimal> byDay = received.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCreatedAt().toLocalDate(),
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));

        List<Map<String, Object>> earningsByDay = byDay.entrySet().stream()
                .map(e -> {
                    Map<String, Object> point = new LinkedHashMap<>();
                    point.put("date", e.getKey().toString());
                    point.put("total", e.getValue());
                    return point;
                })
                .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("transactionCount", received.size());
        result.put("totalEarned", totalEarned);
        result.put("averageTransaction", received.isEmpty() ? BigDecimal.ZERO
                : totalEarned.divide(BigDecimal.valueOf(received.size()), 2, java.math.RoundingMode.HALF_UP));
        result.put("peakHourOfDay", peakHour);
        result.put("earningsByHour", earningsByHour);
        result.put("earningsByDay", earningsByDay);
        result.put("last10Transactions", received.stream().limit(10).toList());
        return result;
    }

    /**
     * Financial identity / credit-readiness score, derived entirely from the
     * trader's own transaction history over a trailing 90-day window - no
     * separate application, no extra data entry. Weighting: 30% record
     * duration (days recorded out of the 90-day target), 30% consistency
     * (how many of the days since their first transaction they actually
     * showed up), 40% verified income (normalised against a R10,000/90-day
     * reference). These weights are a product rule to validate against real
     * lending data, not an established credit-scoring standard.
     */
    public FinancialScoreResponse financialScore(Long vendorId) {
        final int windowDays = 90;
        LocalDateTime windowStart = LocalDateTime.now().minusDays(windowDays);

        List<Transaction> inWindow = transactionRepository.findByVendorIdOrderByCreatedAtDesc(vendorId).stream()
                .filter(t -> t.getCreatedAt().isAfter(windowStart))
                .toList();

        BigDecimal totalIncome = inWindow.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long transactionCount = inWindow.size();

        Set<LocalDate> distinctDays = inWindow.stream()
                .map(t -> t.getCreatedAt().toLocalDate())
                .collect(Collectors.toSet());
        int daysRecorded = distinctDays.size();

        long daysSinceFirst = inWindow.stream()
                .map(Transaction::getCreatedAt)
                .min(LocalDateTime::compareTo)
                .map(first -> ChronoUnit.DAYS.between(first.toLocalDate(), LocalDate.now()) + 1)
                .orElse(0L);
        double consistencyPercentage = daysSinceFirst <= 0 ? 0
                : Math.min(100.0, (daysRecorded * 100.0) / daysSinceFirst);

        double recordDurationScore = Math.min(100.0, (daysRecorded * 100.0) / windowDays);
        double incomeReference = 10000.0;
        double incomeScore = Math.min(100.0, (totalIncome.doubleValue() / incomeReference) * 100.0);

        int financialScore = (int) Math.round(
                0.30 * recordDurationScore + 0.30 * consistencyPercentage + 0.40 * incomeScore);

        boolean creditEligible = daysRecorded >= 30 && financialScore >= 40;

        BigDecimal recommendedMin = BigDecimal.ZERO;
        BigDecimal recommendedMax = BigDecimal.ZERO;
        String reason;
        if (creditEligible) {
            double dailyAverage = daysRecorded > 0 ? totalIncome.doubleValue() / daysRecorded : 0;
            double monthlyEstimate = dailyAverage * 30;
            recommendedMin = roundToNearest(monthlyEstimate * 1.0, 500);
            recommendedMax = roundToNearest(monthlyEstimate * 1.8, 500);
            reason = daysRecorded + " days of recorded income at " + String.format("%.0f", consistencyPercentage)
                    + "% consistency over the last " + windowDays + " days";
        } else {
            reason = "Needs at least 30 recorded days (currently " + daysRecorded
                    + ") and a stronger income/consistency pattern to reach credit eligibility";
        }

        return new FinancialScoreResponse(
                financialScore, windowDays, daysRecorded, round1(consistencyPercentage),
                totalIncome, transactionCount, creditEligible, recommendedMin, recommendedMax, reason);
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static BigDecimal roundToNearest(double value, int nearest) {
        long rounded = Math.round(value / nearest) * (long) nearest;
        return BigDecimal.valueOf(rounded);
    }
}
