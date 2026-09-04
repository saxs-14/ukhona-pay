package co.za.ukhonapay.controller;

import co.za.ukhonapay.dto.TransactionResponse;
import co.za.ukhonapay.security.CurrentUser;
import co.za.ukhonapay.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/me")
    public ResponseEntity<List<TransactionResponse>> myTransactions() {
        return ResponseEntity.ok(transactionService.historyForUser(CurrentUser.id()));
    }
}
