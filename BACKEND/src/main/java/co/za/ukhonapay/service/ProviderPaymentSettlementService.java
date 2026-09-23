package co.za.ukhonapay.service;
import co.za.ukhonapay.model.PaymentIntent;
import co.za.ukhonapay.model.Transaction;
import co.za.ukhonapay.model.Vendor;
import co.za.ukhonapay.model.Wallet;
import co.za.ukhonapay.model.enums.TransactionStatus;
import co.za.ukhonapay.repository.PaymentIntentRepository;
import co.za.ukhonapay.repository.TransactionRepository;
import co.za.ukhonapay.repository.VendorRepository;
import co.za.ukhonapay.repository.WalletRepository;
import co.za.ukhonapay.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Service
public class ProviderPaymentSettlementService {
 private final PaymentIntentRepository intents; private final VendorRepository vendors; private final WalletRepository wallets; private final TransactionRepository transactions; private final WalletService walletService;
 public ProviderPaymentSettlementService(PaymentIntentRepository intents,VendorRepository vendors,WalletRepository wallets,TransactionRepository transactions,WalletService walletService){
  this.intents=intents;this.vendors=vendors;this.wallets=wallets;this.transactions=transactions;this.walletService=walletService;
 }
 @Transactional
 public void settleSuccessful(String internalReference,String providerReference,BigDecimal amount,String currency){
  PaymentIntent intent=intents.findByInternalReferenceForUpdate(internalReference).orElseThrow(()->new ResourceNotFoundException("Payment intent not found"));
  if(!"ZAR".equals(currency)||intent.getAmount().compareTo(amount)!=0)throw new IllegalArgumentException("Provider payment amount or currency does not match the payment intent");
  if(intent.getProviderReference()!=null&&!intent.getProviderReference().equals(providerReference))throw new IllegalArgumentException("Provider reference does not match the payment intent");
  if("COMPLETED".equals(intent.getStatus()))return;
  if(!"PENDING".equals(intent.getStatus()))throw new IllegalStateException("Payment intent is not payable in status "+intent.getStatus());
  Vendor vendor=vendors.findById(intent.getVendorId()).orElseThrow(()->new ResourceNotFoundException("Vendor not found"));
  Wallet vendorWallet=wallets.findWithLockByUserId(vendor.getUserId()).orElseThrow(()->new ResourceNotFoundException("Vendor wallet not found"));
  Wallet platformWallet=walletService.getLockedPlatformFeeWallet();
  BigDecimal fee=WalletService.PLATFORM_FEE;
  BigDecimal net=amount.subtract(fee);
  if(net.signum()<=0)throw new IllegalArgumentException("Payment amount is too small for the configured platform fee");
  WalletService.creditWithAutoAllocation(vendorWallet,net); platformWallet.setBalance(platformWallet.getBalance().add(fee));
  wallets.save(vendorWallet);wallets.save(platformWallet);
  Transaction tx=Transaction.builder().reference("TXN-"+java.util.UUID.randomUUID().toString().replace("-","").substring(0,16).toUpperCase())
   .receiverId(vendor.getUserId()).vendorId(vendor.getId()).amount(amount).platformFee(fee).cashbackAmount(BigDecimal.ZERO).cashbackRate(BigDecimal.ZERO)
   .status(TransactionStatus.COMPLETED).description("Provider payment "+internalReference).build();
  transactions.save(tx);
  intent.setProviderReference(providerReference);intent.setStatus("COMPLETED");intent.setCompletedAt(LocalDateTime.now());intents.save(intent);
 }
 @Transactional
 public void markFailed(String internalReference,String reason){
  PaymentIntent intent=intents.findByInternalReferenceForUpdate(internalReference).orElseThrow(()->new ResourceNotFoundException("Payment intent not found"));
  if("COMPLETED".equals(intent.getStatus()))return;
  intent.setStatus("FAILED");intent.setFailureReason(reason);intents.save(intent);
 }
}