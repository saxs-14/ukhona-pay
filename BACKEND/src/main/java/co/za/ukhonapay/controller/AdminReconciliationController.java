package co.za.ukhonapay.controller;

import co.za.ukhonapay.service.WalletReconciliationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reconciliation")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReconciliationController {
    private final WalletReconciliationService reconciliation;

    public AdminReconciliationController(WalletReconciliationService reconciliation) {
        this.reconciliation = reconciliation;
    }

    @GetMapping("/vendors")
    public List<WalletReconciliationService.Result> reconcileVendors() {
        return reconciliation.reconcileVendorAccounts();
    }
}
