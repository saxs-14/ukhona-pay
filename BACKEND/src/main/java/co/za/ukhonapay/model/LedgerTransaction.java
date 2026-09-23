package co.za.ukhonapay.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="ledger_transactions")
public class LedgerTransaction {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=40) private String reference;
 @Column(name="transaction_type",nullable=false,length=40) private String transactionType;
 @Column(name="external_reference",length=160) private String externalReference;
 @Column(nullable=false,length=20) private String status="POSTED";
 @Column(length=255) private String description;
 @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
 @PrePersist void onCreate(){if(createdAt==null)createdAt=LocalDateTime.now();if(status==null)status="POSTED";}
 public Long getId(){return id;} public String getReference(){return reference;} public void setReference(String v){reference=v;}
 public String getTransactionType(){return transactionType;} public void setTransactionType(String v){transactionType=v;}
 public String getExternalReference(){return externalReference;} public void setExternalReference(String v){externalReference=v;}
 public String getStatus(){return status;} public void setStatus(String v){status=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;}
}