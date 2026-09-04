package co.za.ukhonapay.controller;

import co.za.ukhonapay.dto.BankAccountRequest;
import co.za.ukhonapay.dto.BankAccountResponse;
import co.za.ukhonapay.security.CurrentUser;
import co.za.ukhonapay.service.BankAccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bank-accounts")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @GetMapping("/me")
    public ResponseEntity<BankAccountResponse> myBankAccount() {
        return ResponseEntity.ok(bankAccountService.getMine(CurrentUser.id()));
    }

    @PutMapping("/me")
    public ResponseEntity<BankAccountResponse> saveMyBankAccount(@Valid @RequestBody BankAccountRequest request) {
        return ResponseEntity.ok(bankAccountService.saveOrUpdate(CurrentUser.id(), request));
    }
}
