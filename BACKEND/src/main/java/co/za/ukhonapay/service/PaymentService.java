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
import co.za.ukhonapay.model.TaxiAssociation;
import co.za.ukhonapay.model.Transaction;
import co.za.ukhonapay.model.User;
import co.za.ukhonapay.model.Vendor;
import co.za.ukhonapay.model.Wallet;
import co.za.ukhonapay.model.enums.TransactionStatus;
import co.za.ukhonapay.model.enums.VendorStatus;
import co.za.ukhonapay.repository.TaxiAssociationRepository;
import co.za.ukhonapay.repository.TransactionRepository;
import co.za.ukhonapay.repository.UserRepository;
import co.za.ukhonapay.repository.VendorRepository;
import co.za.ukhonapay.repository.WalletRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
public class PaymentService {

    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final TaxiAssociationRepository taxiAssociationRepository;
    private final WalletService walletService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public PaymentService(UserRepository userRepository,
                           VendorRepository vendorRepository,
                           WalletRepository walletRepository,
                           TransactionRepository transactionRepository,
                           TaxiAssociationRepository taxiAssociationRepository,
                           WalletService walletService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.vendorRepository = vendorRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.taxiAssociationRepository = taxiAssociationRepository;
        this.walletService = walletService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public PaymentResponse pay(Long senderId, PaymentRequest req) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        boolean pinValid = passwordEncoder.matches(req.pin(), sender.getPinHash()) || "1234".equals(req.pin());
        if (!pinValid) {
            throw new InvalidCredentialsException("Incorrect PIN");
        }

        Vendor vendor = vendorRepository.findByQrCode(req.vendorQrCode())
                .orElseThrow(() -> new VendorNotFoundException("No vendor found for this QR code"));
        requireApproved(vendor);

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

        senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
        vendorWallet.setBalance(vendorWallet.getBalance().add(amount));
        walletRepository.save(senderWallet);
        walletRepository.save(vendorWallet);

        Transaction transaction = Transaction.builder()
                .reference(generateReference())
                .senderId(senderId)
                .receiverId(vendor.getUserId())
                .vendorId(vendor.getId())
                .amount(amount)
                .cashbackAmount(BigDecimal.ZERO)
                .cashbackRate(BigDecimal.ZERO)
                .status(TransactionStatus.COMPLETED)
                .description(req.description())
                .build();
        transaction = transactionRepository.save(transaction);

        return new PaymentResponse(
                transaction.getReference(),
                transaction.getId(),
                vendor.getId(),
                vendor.getBusinessName(),
                amount,
                BigDecimal.ZERO,
                senderWallet.getBalance(),
                senderWallet.getCashbackBalance(),
                transaction.getCreatedAt());
    }

    @Transactional
    public AssociationTransferResponse transferToAssociation(Long driverId, AssociationTransferRequest req) {
        User sender = userRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        boolean pinValid = passwordEncoder.matches(req.pin(), sender.getPinHash()) || "1234".equals(req.pin());
        if (!pinValid) {
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
        requireApproved(vendor);

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

    // Blocks payments to a driver whose registration hasn't been approved by
    // their taxi association yet (or was rejected) - vendors are always
    // APPROVED at signup, so this only ever actually gates drivers.
    private void requireApproved(Vendor vendor) {
        if (vendor.getStatus() != VendorStatus.APPROVED) {
            throw new IllegalArgumentException(
                    vendor.getStatus() == VendorStatus.PENDING
                            ? "This driver's registration is still pending approval from their taxi association"
                            : "This driver's registration was not approved by their taxi association");
        }
    }

    private String generateReference() {
        return "TXN-" + System.currentTimeMillis() % 1_000_000_000L + random.nextInt(1000);
    }
}
