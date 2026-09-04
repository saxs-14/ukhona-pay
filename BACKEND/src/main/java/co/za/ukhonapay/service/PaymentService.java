package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.AssociationTransferRequest;
import co.za.ukhonapay.dto.AssociationTransferResponse;
import co.za.ukhonapay.dto.IncomingPaymentRequest;
import co.za.ukhonapay.dto.IncomingPaymentResponse;
import co.za.ukhonapay.dto.PaymentRequest;
import co.za.ukhonapay.dto.PaymentResponse;
import co.za.ukhonapay.exception.InsufficientFundsException;
import co.za.ukhonapay.exception.InvalidCredentialsException;
import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.exception.VendorNotFoundException;
import co.za.ukhonapay.model.Cashback;
import co.za.ukhonapay.model.TaxiAssociation;
import co.za.ukhonapay.model.Transaction;
import co.za.ukhonapay.model.User;
import co.za.ukhonapay.model.Vendor;
import co.za.ukhonapay.model.Wallet;
import co.za.ukhonapay.model.enums.CashbackStatus;
import co.za.ukhonapay.model.enums.TransactionStatus;
import co.za.ukhonapay.model.enums.UserType;
import co.za.ukhonapay.repository.CashbackRepository;
import co.za.ukhonapay.repository.TaxiAssociationRepository;
import co.za.ukhonapay.repository.TransactionRepository;
import co.za.ukhonapay.repository.UserRepository;
import co.za.ukhonapay.repository.VendorRepository;
import co.za.ukhonapay.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;

@Service
public class PaymentService {

    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final CashbackRepository cashbackRepository;
    private final TaxiAssociationRepository taxiAssociationRepository;
    private final WalletService walletService;
    private final PasswordEncoder passwordEncoder;
    private final BigDecimal defaultCashbackRate;
    private final SecureRandom random = new SecureRandom();

    public PaymentService(UserRepository userRepository,
                           VendorRepository vendorRepository,
                           WalletRepository walletRepository,
                           TransactionRepository transactionRepository,
                           CashbackRepository cashbackRepository,
                           TaxiAssociationRepository taxiAssociationRepository,
                           WalletService walletService,
                           PasswordEncoder passwordEncoder,
                           @Value("${ukhonapay.cashback.default-rate}") BigDecimal defaultCashbackRate) {
        this.userRepository = userRepository;
        this.vendorRepository = vendorRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.cashbackRepository = cashbackRepository;
        this.taxiAssociationRepository = taxiAssociationRepository;
        this.walletService = walletService;
        this.passwordEncoder = passwordEncoder;
        this.defaultCashbackRate = defaultCashbackRate;
    }

    @Transactional
    public PaymentResponse pay(Long senderId, PaymentRequest req) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        if (!passwordEncoder.matches(req.pin(), sender.getPinHash())) {
            throw new InvalidCredentialsException("Incorrect PIN");
        }

        Vendor vendor = vendorRepository.findByQrCode(req.vendorQrCode())
                .orElseThrow(() -> new VendorNotFoundException("No vendor found for this QR code"));

        if (vendor.getUserId().equals(senderId)) {
            throw new IllegalArgumentException("You cannot pay yourself");
        }

        Wallet senderWallet = walletRepository.findWithLockByUserId(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender wallet not found"));
        Wallet vendorWallet = walletRepository.findWithLockByUserId(vendor.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor wallet not found"));

        BigDecimal amount = req.amount();
        if (senderWallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient wallet balance for this payment");
        }

        boolean isBusinessOrDriver = sender.getUserType() == UserType.VENDOR 
                || sender.getUserType() == UserType.TAXI_DRIVER 
                || sender.getUserType() == UserType.TAXI_ASSOCIATION_ADMIN;
        BigDecimal cashbackRate = isBusinessOrDriver ? BigDecimal.ZERO : defaultCashbackRate;
        BigDecimal cashbackAmount = amount.multiply(cashbackRate).setScale(2, RoundingMode.HALF_UP);

        senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
        senderWallet.setCashbackBalance(senderWallet.getCashbackBalance().add(cashbackAmount));
        vendorWallet.setBalance(vendorWallet.getBalance().add(amount));
        walletRepository.save(senderWallet);
        walletRepository.save(vendorWallet);

        Transaction transaction = Transaction.builder()
                .reference(generateReference())
                .senderId(senderId)
                .receiverId(vendor.getUserId())
                .vendorId(vendor.getId())
                .amount(amount)
                .cashbackAmount(cashbackAmount)
                .cashbackRate(cashbackRate)
                .status(TransactionStatus.COMPLETED)
                .description(req.description())
                .build();
        transaction = transactionRepository.save(transaction);

        if (cashbackAmount.compareTo(BigDecimal.ZERO) > 0) {
            Cashback cashback = Cashback.builder()
                    .userId(senderId)
                    .transactionId(transaction.getId())
                    .amount(cashbackAmount)
                    .status(CashbackStatus.EARNED)
                    .build();
            cashbackRepository.save(cashback);
        }

        return new PaymentResponse(
                transaction.getReference(),
                transaction.getId(),
                vendor.getId(),
                vendor.getBusinessName(),
                amount,
                cashbackAmount,
                senderWallet.getBalance(),
                senderWallet.getCashbackBalance(),
                transaction.getCreatedAt());
    }

    @Transactional
    public AssociationTransferResponse transferToAssociation(Long driverId, AssociationTransferRequest req) {
        User sender = userRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        if (!passwordEncoder.matches(req.pin(), sender.getPinHash())) {
            throw new InvalidCredentialsException("Incorrect PIN");
        }

        Vendor driverProfile = vendorRepository.findByUserId(driverId)
                .orElseThrow(() -> new VendorNotFoundException("No driver profile for this user"));
        Long associationId = driverProfile.getAssociationId();
        if (associationId == null) {
            throw new ResourceNotFoundException("No taxi association linked to your driver profile");
        }
        TaxiAssociation association = taxiAssociationRepository.findById(associationId)
                .orElseThrow(() -> new ResourceNotFoundException("Taxi association not found"));

        Wallet senderWallet = walletRepository.findWithLockByUserId(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender wallet not found"));

        BigDecimal amount = req.amount();
        if (senderWallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient wallet balance for this transfer");
        }

        Wallet associationWallet = walletService.getOrCreateLockedAssociationWallet(associationId);

        senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
        associationWallet.setBalance(associationWallet.getBalance().add(amount));
        walletRepository.save(senderWallet);
        walletRepository.save(associationWallet);

        Transaction transaction = Transaction.builder()
                .reference(generateReference())
                .senderId(driverId)
                .receiverAssociationId(associationId)
                .amount(amount)
                .cashbackAmount(BigDecimal.ZERO)
                .cashbackRate(BigDecimal.ZERO)
                .status(TransactionStatus.COMPLETED)
                .description(req.description())
                .build();
        transaction = transactionRepository.save(transaction);

        return new AssociationTransferResponse(
                transaction.getReference(), associationId, association.getName(),
                amount, senderWallet.getBalance(), transaction.getCreatedAt());
    }

    // A commuter paying via their own banking app - no sender wallet to debit,
    // no PIN to check, since the payer never holds a UKHONA PAY account. This
    // stands in for what a real bank's payment-confirmation webhook would call.
    @Transactional
    public IncomingPaymentResponse receiveExternalPayment(IncomingPaymentRequest req) {
        Vendor vendor = vendorRepository.findByQrCode(req.vendorQrCode())
                .orElseThrow(() -> new VendorNotFoundException("No vendor found for this QR code"));

        Wallet vendorWallet = walletRepository.findWithLockByUserId(vendor.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendor wallet not found"));

        BigDecimal amount = req.amount();
        vendorWallet.setBalance(vendorWallet.getBalance().add(amount));
        walletRepository.save(vendorWallet);

        Transaction transaction = Transaction.builder()
                .reference(generateReference())
                .receiverId(vendor.getUserId())
                .vendorId(vendor.getId())
                .amount(amount)
                .cashbackAmount(BigDecimal.ZERO)
                .cashbackRate(BigDecimal.ZERO)
                .status(TransactionStatus.COMPLETED)
                .description(req.description())
                .build();
        transaction = transactionRepository.save(transaction);

        return new IncomingPaymentResponse(
                transaction.getReference(), vendor.getId(), vendor.getBusinessName(),
                amount, vendorWallet.getBalance(), transaction.getCreatedAt());
    }

    private String generateReference() {
        return "TXN-" + System.currentTimeMillis() % 1_000_000_000L + random.nextInt(1000);
    }
}
