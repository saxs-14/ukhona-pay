package co.za.ukhonapay.security;
import org.junit.jupiter.api.Test;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;
class SvixWebhookVerifierTest {
 @Test void acceptsValidSignature() throws Exception{
  String secret=Base64.getEncoder().encodeToString("secret".getBytes(StandardCharsets.UTF_8));
  String id="evt_123",ts=String.valueOf(System.currentTimeMillis()/1000),body="{\"type\":\"transaction.complete\"}";
  Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec("secret".getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
  String sig=Base64.getEncoder().encodeToString(mac.doFinal((id+"."+ts+"."+body).getBytes(StandardCharsets.UTF_8)));
  assertTrue(SvixWebhookVerifier.verify("whsec_"+secret,id,ts,"v1,"+sig,body));
 }
 @Test void rejectsMissingHeaders(){assertFalse(SvixWebhookVerifier.verify("whsec_x",null,"1","v1,x","{}"));}
}