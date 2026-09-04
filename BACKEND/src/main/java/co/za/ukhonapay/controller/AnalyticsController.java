package co.za.ukhonapay.controller;

import co.za.ukhonapay.dto.AnalyticsResponse;
import co.za.ukhonapay.dto.FinancialScoreResponse;
import co.za.ukhonapay.security.CurrentUser;
import co.za.ukhonapay.service.AnalyticsService;
import co.za.ukhonapay.service.VendorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final VendorService vendorService;

    public AnalyticsController(AnalyticsService analyticsService, VendorService vendorService) {
        this.analyticsService = analyticsService;
        this.vendorService = vendorService;
    }

    @GetMapping("/platform")
    public ResponseEntity<AnalyticsResponse> platform() {
        return ResponseEntity.ok(analyticsService.platformAnalytics());
    }

    @GetMapping("/vendor/me")
    public ResponseEntity<Map<String, Object>> myVendorAnalytics() {
        Long vendorId = vendorService.getByUserId(CurrentUser.id()).vendorId();
        return ResponseEntity.ok(analyticsService.vendorAnalytics(vendorId));
    }

    @GetMapping("/financial-score/me")
    public ResponseEntity<FinancialScoreResponse> myFinancialScore() {
        Long vendorId = vendorService.getByUserId(CurrentUser.id()).vendorId();
        return ResponseEntity.ok(analyticsService.financialScore(vendorId));
    }
}
