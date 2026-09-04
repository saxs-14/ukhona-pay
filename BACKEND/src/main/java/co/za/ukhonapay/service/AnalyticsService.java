package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.AnalyticsResponse;
import co.za.ukhonapay.model.Transaction;
import co.za.ukhonapay.model.Vendor;
import co.za.ukhonapay.repository.TransactionRepository;
import co.za.ukhonapay.repository.VendorRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        return new AnalyticsResponse(all.size(), totalVolume, totalCashback, activeVendors, topVendors, categoryBreakdown);
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
}
