package co.za.ukhonapay.controller;

import co.za.ukhonapay.dto.AtmLocationResponse;
import co.za.ukhonapay.dto.WithdrawalRequest;
import co.za.ukhonapay.dto.WithdrawalResponse;
import co.za.ukhonapay.security.CurrentUser;
import co.za.ukhonapay.service.WithdrawalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import co.za.ukhonapay.dto.VendorBankWithdrawalRequest;
import co.za.ukhonapay.dto.VendorBankWithdrawalResponse;

@RestController
@RequestMapping("/api/withdrawals")
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    public WithdrawalController(WithdrawalService withdrawalService) {
        this.withdrawalService = withdrawalService;
    }

    @GetMapping("/atms")
    public ResponseEntity<List<AtmLocationResponse>> nearbyAtms() {
        return ResponseEntity.ok(withdrawalService.nearbyAtms());
    }

    @PostMapping
    public ResponseEntity<WithdrawalResponse> request(@Valid @RequestBody WithdrawalRequest request) {
        return ResponseEntity.ok(withdrawalService.requestWithdrawal(CurrentUser.id(), request));
    }

    @PostMapping("/vendor/bank")
    public ResponseEntity<VendorBankWithdrawalResponse> withdrawToBank(@Valid @RequestBody VendorBankWithdrawalRequest request) {
        return ResponseEntity.ok(withdrawalService.withdrawToBank(CurrentUser.id(), request));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<WithdrawalResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(withdrawalService.completeWithdrawal(CurrentUser.id(), id));
    }

    @GetMapping("/me")
    public ResponseEntity<List<WithdrawalResponse>> myWithdrawals() {
        return ResponseEntity.ok(withdrawalService.historyForUser(CurrentUser.id()));
    }
}
