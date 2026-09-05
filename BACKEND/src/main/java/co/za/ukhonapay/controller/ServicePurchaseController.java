package co.za.ukhonapay.controller;

import co.za.ukhonapay.dto.ServicePurchaseRequest;
import co.za.ukhonapay.dto.ServicePurchaseResponse;
import co.za.ukhonapay.model.ServicePurchase;
import co.za.ukhonapay.security.CurrentUser;
import co.za.ukhonapay.service.ServicePurchaseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
public class ServicePurchaseController {

    private final ServicePurchaseService servicePurchaseService;

    public ServicePurchaseController(ServicePurchaseService servicePurchaseService) {
        this.servicePurchaseService = servicePurchaseService;
    }

    @PostMapping("/purchase")
    public ResponseEntity<ServicePurchaseResponse> purchase(@Valid @RequestBody ServicePurchaseRequest req) {
        return ResponseEntity.ok(servicePurchaseService.purchase(CurrentUser.id(), req));
    }

    @GetMapping("/history")
    public ResponseEntity<List<ServicePurchase>> history() {
        return ResponseEntity.ok(servicePurchaseService.historyForUser(CurrentUser.id()));
    }
}
