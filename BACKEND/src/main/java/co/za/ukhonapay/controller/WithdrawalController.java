package co.za.ukhonapay.controller;

import co.za.ukhonapay.dto.VendorBankWithdrawalRequest;
import co.za.ukhonapay.dto.VendorBankWithdrawalResponse;
import co.za.ukhonapay.security.CurrentUser;
import co.za.ukhonapay.service.WithdrawalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/withdrawals")
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    public WithdrawalController(WithdrawalService withdrawalService) {
        this.withdrawalService = withdrawalService;
    }

    @PostMapping("/vendor/bank")
    public ResponseEntity<VendorBankWithdrawalResponse> withdrawToBank(@Valid @RequestBody VendorBankWithdrawalRequest request) {
        return ResponseEntity.ok(withdrawalService.withdrawToBank(CurrentUser.id(), request));
    }
}
