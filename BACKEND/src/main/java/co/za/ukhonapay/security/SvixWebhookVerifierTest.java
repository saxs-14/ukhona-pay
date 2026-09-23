package co.za.ukhonapay.security;
import org.junit.jupiter.api.Test;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;
class SvixWebhookVerifierTest{
 @Test void verifiesKnownSignature(){
  String secret="whsec_plJ3nmyCDGBKInavdOK15jsl"; String body="{\"event_type\":\"ping\",\"data\":{\"success\":true}}"; String id="msg_loFOjxBNrRLzqYUf"; String ts=""+Instant.now().getEpochSecond();
  try{
   byte[] key=Base64.getDecoder().decode(secret.substring(6)); Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));
   String expected=Base64.getEncoder().encodeToString(mac.doFinal((id+"."+ts+"."+body).getBytes(StandardCharsets.UTF_8)));
   assertTrue(SvixWebhookVerifier.verify(secret,id,ts,"v1,"+expected,body));
   assertFalse(SvixWebhookVerifier.verify(secret,id,ts,"v1,"+expected,body+"x"));
  }catch(Exception e){fail(e);}
 }
 @Test void rejectsOldTimestamp(){
  assertFalse(SvixWebhookVerifier.verify("whsec_plJ3nmyCDGBKInavdOK15jsl","msg","1","v1,anything","{}"));
 }
}