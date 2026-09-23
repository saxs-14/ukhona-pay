package co.za.ukhonapay.controller;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.za.ukhonapay.model.PaymentWebhookEvent;
import co.za.ukhonapay.repository.PaymentWebhookEventRepository;
import co.za.ukhonapay.security.SvixWebhookVerifier;
import co.za.ukhonapay.service.ProviderPaymentSettlementService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@RestController @RequestMapping("/api/payments/webhooks/ozow")
public class OzowWebhookController{
 private final ObjectMapper mapper; private final PaymentWebhookEventRepository events; private final ProviderPaymentSettlementService settlement; private final String secret;
 public OzowWebhookController(ObjectMapper mapper,PaymentWebhookEventRepository events,ProviderPaymentSettlementService settlement,
   @Value("$"+"{ukhonapay.payments.webhook-secret:}") String secret){this.mapper=mapper;this.events=events;this.settlement=settlement;this.secret=secret;}
 @PostMapping public ResponseEntity<Void> receive(@RequestHeader(value="svix-id",required=false) String id,
   @RequestHeader(value="svix-timestamp",required=false) String timestamp,
   @RequestHeader(value="svix-signature",required=false) String signature,
   @RequestBody String rawBody){
  if(!SvixWebhookVerifier.verify(secret,id,timestamp,signature,rawBody))return ResponseEntity.status(401).build();
  try{
   JsonNode root=mapper.readTree(rawBody); String type=root.path("type").asText(""); JsonNode data=root.path("data");
   if(!"transaction.complete".equals(type))return ResponseEntity.ok().build();
   if(id==null||events.findByProviderAndProviderEventId("OZOW",id).isPresent())return ResponseEntity.ok().build();
   PaymentWebhookEvent event=new PaymentWebhookEvent();event.setProvider("OZOW");event.setProviderEventId(id);event.setEventType(type);event.setSignatureVerified(true);event.setPayload(rawBody);event.setProcessingStatus("RECEIVED");events.saveAndFlush(event);
   String reference=data.path("TransactionReference").asText("");
   if(reference.isBlank())reference=data.path("merchantReference").asText("");
   String providerReference=data.path("TransactionId").asText(data.path("id").asText(""));
   String status=data.path("Status").asText(data.path("status").asText(""));
   if("Successful".equalsIgnoreCase(status)){
    BigDecimal amount=new BigDecimal(data.path("Amount").asText("0"));
    settlement.settleSuccessful(reference,providerReference,amount,"ZAR");
   }else if("Error".equalsIgnoreCase(status)){
    settlement.markFailed(reference,data.path("Reason").asText(data.path("reason").asText("Provider reported payment failure")));
   }
   event.setProcessingStatus("PROCESSED");event.setProcessedAt(LocalDateTime.now());events.save(event);
   return ResponseEntity.ok().build();
  }catch(Exception ex){
   return ResponseEntity.status(500).build();
  }
 }
}