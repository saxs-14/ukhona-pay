package co.za.ukhonapay.controller;

import co.za.ukhonapay.service.BankWithdrawalReconciliationService;
import co.za.ukhonapay.service.ProviderPaymentReconciliationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/payments")
public class AdminPaymentReconciliationController {
    private final ProviderPaymentReconciliationService payinReconciliation;
    private final BankWithdrawalReconciliationService payoutReconciliation;

    public AdminPaymentReconciliationController(
            ProviderPaymentReconciliationService payinReconciliation,
            BankWithdrawalReconciliationService payoutReconciliation) {
        this.payinReconciliation = payinReconciliation;
        this.payoutReconciliation = payoutReconciliation;
    }

    @PostMapping("/reconcile")
    public Map<String, Object> reconcile() {
        return Map.of(
                "payinsProcessed", payinReconciliation.reconcilePending(),
                "payoutsProcessed", payoutReconciliation.reconcilePending());
    }
}
