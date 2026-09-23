package co.za.ukhonapay.service;

import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.model.*;
import co.za.ukhonapay.model.enums.TransactionStatus;
import co.za.ukhonapay.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProviderPaymentSettlementService {
 private final PaymentIntentRepository intents; private final VendorRepository vendors; private final WalletRepository wallets;
 private final TransactionRepository transactions; private final WalletService walletService; private final LedgerService ledger;
 private final LedgerAccountRepository ledgerAccounts;

 public ProviderPaymentSettlementService(PaymentIntentRepository intents,VendorRepository vendors,WalletRepository wallets,
   TransactionRepository transactions,WalletService walletService,LedgerService ledger,LedgerAccountRepository ledgerAccounts){
  this.intents=intents;this.vendors=vendors;this.wallets=wallets;this.transactions=transactions;this.walletService=walletService;
  this.ledger=ledger;this.ledgerAccounts=ledgerAccounts;
 }

 @Transactional
 public void settleSuccessful(String internalReference,String providerReference,BigDecimal amount,String currency){
  PaymentIntent intent=intents.findByInternalReferenceForUpdate(internalReference)
    .orElseThrow(()->new ResourceNotFoundException("Payment intent not found"));
  if(!"ZAR".equals(currency)||amount==null||intent.getAmount().compareTo(amount)!=0)
    throw new IllegalArgumentException("Provider payment amount or currency does not match the payment intent");
  if(intent.getProviderReference()!=null&&!intent.getProviderReference().equals(providerReference))
    throw new IllegalArgumentException("Provider reference does not match the payment intent");
  if("COMPLETED".equals(intent.getStatus()))return;
  if(!"PENDING".equals(intent.getStatus()))
    throw new IllegalStateException("Payment intent is not payable in status "+intent.getStatus());

  Vendor vendor=vendors.findById(intent.getVendorId()).orElseThrow(()->new ResourceNotFoundException("Vendor not found"));
  Wallet vendorWallet=wallets.findWithLockByUserId(vendor.getUserId())
    .orElseThrow(()->new ResourceNotFoundException("Vendor wallet not found"));
  BigDecimal fee=WalletService.PLATFORM_FEE;
  BigDecimal net=amount.subtract(fee);
  if(net.signum()<=0)throw new IllegalArgumentException("Payment amount is too small for the configured platform fee");

  String vendorAccountCode="VENDOR_WALLET_ZAR_"+vendor.getId();
  ensureAccount(vendorAccountCode,"VENDOR_WALLET",vendor.getUserId(),null);
  Wallet platformWallet=walletService.getLockedPlatformFeeWallet();

  // The ledger records the economic event: external clearing is debited,
  // while the vendor receives the net amount and the platform receives its fee.
  ledger.post("LED-"+internalReference,"PROVIDER_PAYMENT",providerReference,
    "Provider settlement "+internalReference,List.of(
      new LedgerService.Entry("PAYMENT_CLEARING_ZAR","DEBIT",amount),
      new LedgerService.Entry(vendorAccountCode,"CREDIT",net),
      new LedgerService.Entry("PLATFORM_FEE_REVENUE_ZAR","CREDIT",fee)
    ));

  WalletService.creditWithAutoAllocation(vendorWallet,net);
  platformWallet.setBalance(platformWallet.getBalance().add(fee));
  wallets.save(vendorWallet);wallets.save(platformWallet);

  Transaction tx=Transaction.builder()
    .reference("TXN-"+java.util.UUID.randomUUID().toString().replace("-","").substring(0,16).toUpperCase())
    .receiverId(vendor.getUserId()).vendorId(vendor.getId()).amount(amount).platformFee(fee)
    .cashbackAmount(BigDecimal.ZERO).cashbackRate(BigDecimal.ZERO).status(TransactionStatus.COMPLETED)
    .description("Provider payment "+internalReference).build();
  transactions.save(tx);
  intent.setProviderReference(providerReference);intent.setStatus("COMPLETED");intent.setCompletedAt(LocalDateTime.now());intents.save(intent);
 }

 private void ensureAccount(String code,String type,Long userId,Long associationId){
  if(ledgerAccounts.findByAccountCode(code).isPresent())return;
  LedgerAccount a=new LedgerAccount();a.setAccountCode(code);a.setAccountType(type);a.setUserId(userId);a.setAssociationId(associationId);a.setCurrency("ZAR");a.setActive(true);
  ledgerAccounts.save(a);
 }

 @Transactional
 public void markFailed(String internalReference,String reason){
  PaymentIntent intent=intents.findByInternalReferenceForUpdate(internalReference)
    .orElseThrow(()->new ResourceNotFoundException("Payment intent not found"));
  if("COMPLETED".equals(intent.getStatus()))return;
  intent.setStatus("FAILED");intent.setFailureReason(reason);intents.save(intent);
 }
}