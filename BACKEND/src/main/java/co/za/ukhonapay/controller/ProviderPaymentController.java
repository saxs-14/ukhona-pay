package co.za.ukhonapay.controller;
import co.za.ukhonapay.dto.ProviderPaymentIntentRequest;
import co.za.ukhonapay.dto.ProviderPaymentIntentResponse;
import co.za.ukhonapay.security.CurrentUser;
import co.za.ukhonapay.service.ProviderPaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/payments/intents")
public class ProviderPaymentController{
 private final ProviderPaymentService service;
 public ProviderPaymentController(ProviderPaymentService service){this.service=service;}
 @PostMapping public ResponseEntity<ProviderPaymentIntentResponse> create(@Valid @RequestBody ProviderPaymentIntentRequest request){
  return ResponseEntity.ok(service.create(CurrentUser.id(),request));
 }
}