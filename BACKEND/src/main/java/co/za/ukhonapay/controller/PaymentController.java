package co.za.ukhonapay.controller;

import co.za.ukhonapay.dto.AssociationTransferRequest;
import co.za.ukhonapay.dto.AssociationTransferResponse;
import co.za.ukhonapay.dto.IncomingPaymentRequest;
import co.za.ukhonapay.dto.IncomingPaymentResponse;
import co.za.ukhonapay.dto.PaymentRequest;
import co.za.ukhonapay.dto.PaymentResponse;
import co.za.ukhonapay.security.CurrentUser;
import co.za.ukhonapay.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/pay")
    public ResponseEntity<PaymentResponse> pay(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.pay(CurrentUser.id(), request));
    }

    @PostMapping("/association")
    public ResponseEntity<AssociationTransferResponse> payAssociation(@Valid @RequestBody AssociationTransferRequest request) {
        return ResponseEntity.ok(paymentService.transferToAssociation(CurrentUser.id(), request));
    }

    // Public - a commuter paying via their own banking app never has a UKHONA
    // PAY account/JWT. See SecurityConfig for the matching permitAll rule.
    @PostMapping("/receive")
    public ResponseEntity<IncomingPaymentResponse> receive(@Valid @RequestBody IncomingPaymentRequest request) {
        return ResponseEntity.ok(paymentService.receiveExternalPayment(request));
    }
}
