package co.za.ukhonapay.service;

import co.za.ukhonapay.model.LedgerAccount;
import co.za.ukhonapay.model.LedgerEntry;
import co.za.ukhonapay.model.LedgerTransaction;
import co.za.ukhonapay.repository.LedgerAccountRepository;
import co.za.ukhonapay.repository.LedgerEntryRepository;
import co.za.ukhonapay.repository.LedgerTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
public class LedgerService {
 private final LedgerAccountRepository accounts; private final LedgerTransactionRepository transactions; private final LedgerEntryRepository entries;
 public LedgerService(LedgerAccountRepository a,LedgerTransactionRepository t,LedgerEntryRepository e){accounts=a;transactions=t;entries=e;}
 public record Entry(String accountCode,String direction,BigDecimal amount){}
 @Transactional
 public LedgerTransaction post(String reference,String type,String externalReference,String description,List<Entry> lines){
  if(reference==null||reference.isBlank()||type==null||type.isBlank()||lines==null||lines.size()<2)throw new IllegalArgumentException("A ledger transaction requires at least two entries");
  if(transactions.findByReference(reference).isPresent())return transactions.findByReference(reference).orElseThrow();
  BigDecimal debits=BigDecimal.ZERO,credits=BigDecimal.ZERO;
  for(Entry line:lines){
   if(line==null||line.accountCode()==null||line.direction()==null||line.amount()==null||line.amount().signum()<=0)throw new IllegalArgumentException("Ledger entries must have positive amounts and valid accounts");
   if("DEBIT".equals(line.direction()))debits=debits.add(line.amount());
   else if("CREDIT".equals(line.direction()))credits=credits.add(line.amount());
   else throw new IllegalArgumentException("Ledger direction must be DEBIT or CREDIT");
  }
  if(debits.compareTo(credits)!=0)throw new IllegalArgumentException("Ledger transaction is not balanced");
  LedgerTransaction tx=new LedgerTransaction(); tx.setReference(reference);tx.setTransactionType(type);tx.setExternalReference(externalReference);tx.setDescription(description);transactions.save(tx);
  for(Entry line:lines){
   LedgerAccount account=accounts.findWithLockByAccountCode(line.accountCode()).orElseThrow(()->new IllegalStateException("Ledger account not found: "+line.accountCode()));
   if(!account.isActive())throw new IllegalStateException("Ledger account is inactive: "+line.accountCode());
   LedgerEntry entry=new LedgerEntry();entry.setTransaction(tx);entry.setAccount(account);entry.setDirection(line.direction());entry.setAmount(line.amount());entries.save(entry);
  }
  return tx;
 }
}