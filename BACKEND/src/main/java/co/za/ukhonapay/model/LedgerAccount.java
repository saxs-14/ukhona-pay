package co.za.ukhonapay.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="ledger_accounts")
public class LedgerAccount {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="account_code",nullable=false,unique=true,length=80) private String accountCode;
 @Column(name="account_type",nullable=false,length=30) private String accountType;
 @Column(name="user_id") private Long userId;
 @Column(name="association_id") private Long associationId;
 @Column(nullable=false,length=3) private String currency="ZAR";
 @Column(nullable=false) private boolean active=true;
 @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
 @PrePersist void onCreate(){if(createdAt==null)createdAt=LocalDateTime.now();if(currency==null)currency="ZAR";}
 public Long getId(){return id;} public String getAccountCode(){return accountCode;} public void setAccountCode(String v){accountCode=v;}
 public String getAccountType(){return accountType;} public void setAccountType(String v){accountType=v;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
 public Long getAssociationId(){return associationId;} public void setAssociationId(Long v){associationId=v;} public String getCurrency(){return currency;} public void setCurrency(String v){currency=v;}
 public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
}