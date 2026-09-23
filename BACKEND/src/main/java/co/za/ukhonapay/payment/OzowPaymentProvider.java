package co.za.ukhonapay.payment;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
@Component
public class OzowPaymentProvider implements PaymentProvider {
 private final RestClient client; private final String clientId,clientSecret,siteCode,scope;
 public OzowPaymentProvider(
  @Value("$"+"{ukhonapay.payments.ozow.base-url:https://one.ozow.com/v1}") String baseUrl,
  @Value("$"+"{ukhonapay.payments.ozow.client-id:}") String clientId,
  @Value("$"+"{ukhonapay.payments.ozow.client-secret:}") String clientSecret,
  @Value("$"+"{ukhonapay.payments.ozow.site-code:}") String siteCode,
  @Value("$"+"{ukhonapay.payments.ozow.scope:payments}") String scope){
  this.client=RestClient.builder().baseUrl(baseUrl).build();this.clientId=clientId;this.clientSecret=clientSecret;this.siteCode=siteCode;this.scope=scope;
 }
 public String name(){return "OZOW";}
 public ProviderPaymentResponse createPayment(String reference,BigDecimal amount,String currency,String returnUrl,String idempotencyKey){
  requireConfigured(); String token=accessToken();
  Map<String,Object> payload=Map.of("siteCode",siteCode,"amount",Map.of("currency",currency,"value",amount),"merchantReference",reference,"beneficiaryReference",reference,"expireAt",Instant.now().plus(30,ChronoUnit.MINUTES).toString(),"returnUrl",returnUrl);
  JsonNode response=client.post().uri("/payments").headers(h->h.setBearerAuth(token)).header("Idempotency-Key",idempotencyKey).contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(JsonNode.class);
  if(response==null||response.path("id").isMissingNode()||response.path("redirectUrl").isMissingNode())throw new IllegalStateException("Ozow returned an incomplete payment response");
  return new ProviderPaymentResponse(response.path("id").asText(),response.path("redirectUrl").asText(),response.path("status").asText("Created"));
 }
 @Override
 public java.util.List<ProviderPaymentTransaction> getTransactions(String paymentReference, java.time.LocalDate fromDate, java.time.LocalDate toDate){
  requireConfigured();
  if(paymentReference==null||paymentReference.isBlank()) throw new IllegalArgumentException("Ozow payment reference is required");
  String token=accessToken();
  com.fasterxml.jackson.databind.JsonNode response=client.get().uri(uriBuilder->uriBuilder.path("/payments/{id}/transactions")
   .queryParam("limit",50).queryParam("offset",0).queryParam("fromDate",fromDate).queryParam("toDate",toDate)
   .build(paymentReference)).headers(h->h.setBearerAuth(token)).retrieve().body(com.fasterxml.jackson.databind.JsonNode.class);
  java.util.List<ProviderPaymentTransaction> result=new java.util.ArrayList<>();
  if(response==null||!response.path("results").isArray()) return result;
  for(com.fasterxml.jackson.databind.JsonNode item:response.path("results")){
   com.fasterxml.jackson.databind.JsonNode amount=item.path("amount");
   result.add(new ProviderPaymentTransaction(item.path("id").asText(""),item.path("merchantReference").asText(""),
    amount.path("value").decimalValue(),amount.path("currency").asText(""),item.path("status").asText(""),item.path("reason").asText("")));
  }
  return result;
 }

 @Override
 public ProviderPaymentRequestStatus getPaymentStatus(String paymentReference){
  requireConfigured();
  String token=accessToken();
  com.fasterxml.jackson.databind.JsonNode response=client.get().uri("/payments/{id}",paymentReference)
   .headers(h->h.setBearerAuth(token)).retrieve().body(com.fasterxml.jackson.databind.JsonNode.class);
  if(response==null) throw new IllegalStateException("Ozow returned an empty payment status response");
  return new ProviderPaymentRequestStatus(response.path("id").asText(paymentReference),response.path("status").asText(""),response.path("reason").asText(""));
 }

 private String accessToken(){
  String form="client_id="+enc(clientId)+"&client_secret="+enc(clientSecret)+"&scope="+enc(scope)+"&grant_type=client_credentials";
  JsonNode response=client.post().uri("/token").contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(JsonNode.class);
  if(response==null||response.path("access_token").isMissingNode())throw new IllegalStateException("Ozow authentication returned no access token");
  return response.path("access_token").asText();
 }
 private void requireConfigured(){if(clientId.isBlank()||clientSecret.isBlank()||siteCode.isBlank())throw new IllegalStateException("Ozow is not configured. Set OZOW_CLIENT_ID, OZOW_CLIENT_SECRET and OZOW_SITE_CODE.");}
 private String enc(String v){return URLEncoder.encode(v,StandardCharsets.UTF_8);}
}