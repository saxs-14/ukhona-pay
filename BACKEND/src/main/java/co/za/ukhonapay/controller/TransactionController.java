package co.za.ukhonapay.controller;

import co.za.ukhonapay.dto.TransactionResponse;
import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.security.CurrentUser;
import co.za.ukhonapay.service.TransactionService;
import co.za.ukhonapay.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserService userService;

    public TransactionController(TransactionService transactionService, UserService userService) {
        this.transactionService = transactionService;
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<List<TransactionResponse>> myTransactions() {
        return ResponseEntity.ok(transactionService.historyForUser(CurrentUser.id()));
    }

    @GetMapping("/association/me")
    public ResponseEntity<List<TransactionResponse>> myAssociationTransactions() {
        Long associationId = userService.getAssociationIdForUser(CurrentUser.id());
        if (associationId == null) {
            throw new ResourceNotFoundException("No taxi association linked to your account");
        }
        return ResponseEntity.ok(transactionService.historyForAssociation(associationId));
    }
}
