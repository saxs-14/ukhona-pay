package co.za.ukhonapay.service;
import co.za.ukhonapay.model.LedgerAccount;
import co.za.ukhonapay.repository.LedgerAccountRepository;
import co.za.ukhonapay.repository.LedgerEntryRepository;
import co.za.ukhonapay.repository.LedgerTransactionRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LedgerServiceTest {
 @Test void rejectsUnbalancedTransaction(){
  LedgerService service=new LedgerService(mock(LedgerAccountRepository.class),mock(LedgerTransactionRepository.class),mock(LedgerEntryRepository.class));
  assertThrows(IllegalArgumentException.class,()->service.post("LED-1","TEST",null,null,List.of(
   new LedgerService.Entry("A","DEBIT",new BigDecimal("10.00")),
   new LedgerService.Entry("B","CREDIT",new BigDecimal("9.00")))));
 }
 @Test void rejectsNonPositiveEntry(){
  LedgerService service=new LedgerService(mock(LedgerAccountRepository.class),mock(LedgerTransactionRepository.class),mock(LedgerEntryRepository.class));
  assertThrows(IllegalArgumentException.class,()->service.post("LED-2","TEST",null,null,List.of(
   new LedgerService.Entry("A","DEBIT",BigDecimal.ZERO),
   new LedgerService.Entry("B","CREDIT",new BigDecimal("1.00")))));
 }
 @Test void postsBalancedTransaction(){
  LedgerAccount a=new LedgerAccount();a.setAccountCode("A");a.setActive(true);
  LedgerAccount b=new LedgerAccount();b.setAccountCode("B");b.setActive(true);
  LedgerAccountRepository ar=mock(LedgerAccountRepository.class);
  LedgerTransactionRepository tr=mock(LedgerTransactionRepository.class);
  LedgerEntryRepository er=mock(LedgerEntryRepository.class);
  when(tr.findByReference("LED-3")).thenReturn(Optional.empty());
  when(ar.findWithLockByAccountCode("A")).thenReturn(Optional.of(a));
  when(ar.findWithLockByAccountCode("B")).thenReturn(Optional.of(b));
  LedgerService service=new LedgerService(ar,tr,er);
  assertDoesNotThrow(()->service.post("LED-3","TEST",null,"ok",List.of(
   new LedgerService.Entry("A","DEBIT",new BigDecimal("10.00")),
   new LedgerService.Entry("B","CREDIT",new BigDecimal("10.00")))));
  verify(tr).save(any()); verify(er,times(2)).save(any());
 }
}