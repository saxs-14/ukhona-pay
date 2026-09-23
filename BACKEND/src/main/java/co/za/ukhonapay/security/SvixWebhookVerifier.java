package co.za.ukhonapay.security;
import javax.crypto.Mac; import javax.crypto.spec.SecretKeySpec; import java.nio.charset.StandardCharsets; import java.security.MessageDigest; import java.time.Instant; import java.util.Base64;
public final class SvixWebhookVerifier{
 private static final long TOLERANCE_SECONDS=300;
 private SvixWebhookVerifier(){}
 public static boolean verify(String secret,String id,String timestamp,String signature,String rawBody){
  try{
   if(secret==null||secret.isBlank()||id==null||timestamp==null||signature==null||rawBody==null)return false;
   long sent=Long.parseLong(timestamp); if(Math.abs(Instant.now().getEpochSecond()-sent)>TOLERANCE_SECONDS)return false;
   String encoded=secret.startsWith("whsec_")?secret.substring(6):secret; byte[] key=Base64.getDecoder().decode(encoded);
   Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));
   byte[] expected=Base64.getEncoder().encode(mac.doFinal((id+"."+sent+"."+rawBody).getBytes(StandardCharsets.UTF_8)));
   for(String candidate:signature.split(" ")){String[] p=candidate.split(",",2);if(p.length==2&&"v1".equals(p[0])&&MessageDigest.isEqual(p[1].getBytes(StandardCharsets.UTF_8),expected))return true;}
   return false;
  }catch(Exception e){return false;}
 }
}