package co.za.ukhonapay.controller;

import co.za.ukhonapay.service.BankWithdrawalReconciliationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/payouts")
public class AdminPayoutReconciliationController {
    private final BankWithdrawalReconciliationService reconciliation;

    public AdminPayoutReconciliationController(BankWithdrawalReconciliationService reconciliation) {
        this.reconciliation = reconciliation;
    }

    @PostMapping("/reconcile")
    public Map<String, Object> reconcile() {
        return Map.of("processed", reconciliation.reconcilePending());
    }
}
