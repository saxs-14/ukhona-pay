package co.za.ukhonapay.service;
import co.za.ukhonapay.dto.ProviderPaymentIntentRequest;
import co.za.ukhonapay.dto.ProviderPaymentIntentResponse;
import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.model.PaymentIntent;
import co.za.ukhonapay.model.Vendor;
import co.za.ukhonapay.model.enums.VendorStatus;
import co.za.ukhonapay.payment.PaymentProvider;
import co.za.ukhonapay.payment.ProviderPaymentResponse;
import co.za.ukhonapay.repository.PaymentIntentRepository;
import co.za.ukhonapay.repository.VendorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.UUID;
@Service
public class ProviderPaymentService {
 private final PaymentIntentRepository intentRepository; private final VendorRepository vendorRepository; private final PaymentProvider provider; private final String configuredProvider; private final String returnUrl;
 public ProviderPaymentService(PaymentIntentRepository intentRepository,VendorRepository vendorRepository,PaymentProvider provider,
   @Value("$"+"{ukhonapay.payments.provider:}") String configuredProvider,
   @Value("$"+"{ukhonapay.frontend.base-url:http://localhost:5173}") String frontendBaseUrl){
  this.intentRepository=intentRepository;this.vendorRepository=vendorRepository;this.provider=provider;this.configuredProvider=configuredProvider;this.returnUrl=frontendBaseUrl+"/payments/return";
 }
 @Transactional
 public ProviderPaymentIntentResponse create(Long payerUserId,ProviderPaymentIntentRequest req){
  Vendor vendor=vendorRepository.findByQrCode(req.vendorQrCode()).orElseThrow(()->new ResourceNotFoundException("No vendor found for this QR code"));
  if(vendor.getStatus()!=VendorStatus.APPROVED)throw new IllegalArgumentException("This vendor is not approved");
  String idem=req.idempotencyKey()==null||req.idempotencyKey().isBlank()?UUID.randomUUID().toString():req.idempotencyKey().trim();
  var existing=intentRepository.findByIdempotencyKey(idem);
  if(existing.isPresent())return response(existing.get(),null);
  if(!"OZOW".equalsIgnoreCase(configuredProvider)||!"OZOW".equalsIgnoreCase(provider.name()))
   throw new IllegalStateException("No production payment provider is configured. Set PAYMENT_PROVIDER=OZOW and configure Ozow credentials.");
  PaymentIntent intent=new PaymentIntent();
  intent.setInternalReference("UKH-"+UUID.randomUUID().toString().replace("-","").substring(0,28).toUpperCase());
  intent.setProvider(provider.name());intent.setIdempotencyKey(idem);intent.setVendorId(vendor.getId());intent.setPayerUserId(payerUserId);intent.setAmount(req.amount());intent.setCurrency("ZAR");intent.setStatus("PENDING");
  intent=intentRepository.saveAndFlush(intent);
  ProviderPaymentResponse created=provider.createPayment(intent.getInternalReference(),intent.getAmount(),intent.getCurrency(),returnUrl,idem);
  intent.setProviderReference(created.providerReference());intent.setStatus("PENDING");intentRepository.save(intent);
  return response(intent,created.redirectUrl());
 }
 private ProviderPaymentIntentResponse response(PaymentIntent i,String redirectUrl){
  return new ProviderPaymentIntentResponse(i.getInternalReference(),i.getProvider(),i.getProviderReference(),redirectUrl,i.getStatus(),i.getAmount(),i.getCurrency());
 }
}