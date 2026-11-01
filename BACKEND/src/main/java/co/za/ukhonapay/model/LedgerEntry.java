package co.za.ukhonapay.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name="ledger_entries")
public class LedgerEntry {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="ledger_transaction_id",nullable=false) private LedgerTransaction transaction;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="ledger_account_id",nullable=false) private LedgerAccount account;
 @Column(nullable=false,length=6) private String direction;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal amount;
 public Long getId(){return id;} public LedgerTransaction getTransaction(){return transaction;} public void setTransaction(LedgerTransaction v){transaction=v;}
 public LedgerAccount getAccount(){return account;} public void setAccount(LedgerAccount v){account=v;} public String getDirection(){return direction;} public void setDirection(String v){direction=v;}
 public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
}