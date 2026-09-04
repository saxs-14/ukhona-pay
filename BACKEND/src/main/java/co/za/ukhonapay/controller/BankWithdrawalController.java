package co.za.ukhonapay.controller;

import co.za.ukhonapay.dto.BankWithdrawalRequest;
import co.za.ukhonapay.dto.BankWithdrawalResponse;
import co.za.ukhonapay.security.CurrentUser;
import co.za.ukhonapay.service.BankWithdrawalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bank-withdrawals")
public class BankWithdrawalController {

    private final BankWithdrawalService bankWithdrawalService;

    public BankWithdrawalController(BankWithdrawalService bankWithdrawalService) {
        this.bankWithdrawalService = bankWithdrawalService;
    }

    @PostMapping
    public ResponseEntity<BankWithdrawalResponse> withdraw(@Valid @RequestBody BankWithdrawalRequest request) {
        return ResponseEntity.ok(bankWithdrawalService.withdraw(CurrentUser.id(), request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<BankWithdrawalResponse>> myWithdrawals() {
        return ResponseEntity.ok(bankWithdrawalService.historyForUser(CurrentUser.id()));
    }
}
