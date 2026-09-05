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

        if (!passwordEncoder.matches(req.pin(), sender.getPinHash())) {
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

        // Platform wallet is always locked last, after every other wallet in
        // this transaction - a fixed lock order across pay/transferToAssociation
        // /receiveExternalPayment so concurrent transactions can't deadlock on it.
        BigDecimal fee = WalletService.PLATFORM_FEE;
        BigDecimal netAmount = amount.subtract(fee);
        Wallet platformWallet = walletService.getLockedPlatformFeeWallet();

        senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
        WalletService.creditWithAutoAllocation(vendorWallet, netAmount);
        platformWallet.setBalance(platformWallet.getBalance().add(fee));
        walletRepository.save(senderWallet);
        walletRepository.save(vendorWallet);
        walletRepository.save(platformWallet);

        Transaction transaction = Transaction.builder()
                .reference(generateReference())
                .senderId(senderId)
                .receiverId(vendor.getUserId())
                .vendorId(vendor.getId())
                .amount(amount)
                .platformFee(fee)
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
                fee,
                BigDecimal.ZERO,
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
        BigDecimal fee = WalletService.PLATFORM_FEE;
        BigDecimal netAmount = amount.subtract(fee);
        Wallet platformWallet = walletService.getLockedPlatformFeeWallet();

        senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
        associationWallet.setBalance(associationWallet.getBalance().add(netAmount));
        platformWallet.setBalance(platformWallet.getBalance().add(fee));
        walletRepository.save(senderWallet);
        walletRepository.save(associationWallet);
        walletRepository.save(platformWallet);

        Transaction transaction = Transaction.builder()
                .reference(generateReference())
                .senderId(driverId)
                .receiverAssociationId(associationId)
                .amount(amount)
                .platformFee(fee)
                .cashbackAmount(BigDecimal.ZERO)
                .cashbackRate(BigDecimal.ZERO)
                .status(TransactionStatus.COMPLETED)
                .description(req.description())
                .build();
        transaction = transactionRepository.save(transaction);

        return new AssociationTransferResponse(
                transaction.getReference(), associationId, association.getName(),
                amount, fee, senderWallet.getBalance(), transaction.getCreatedAt());
    }

    // An association admin fining a driver in their own association - unlike
    // transferToAssociation, this is admin-initiated (no PIN, the driver isn't
    // the one authorizing it) and capped at the driver's available balance
    // rather than allowed to go negative: wallets.balance has a DB-level
    // CHECK (balance >= 0), and tracking money genuinely owed beyond that is
    // a real debt-ledger feature this doesn't attempt to be. No platform fee -
    // it's a punitive admin action, not a voluntary transaction.
    @Transactional
    public AssociationTransferResponse issueFine(Long adminAssociationId, Long vendorId, BigDecimal amount, String reason) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));
        if (vendor.getAssociationId() == null || !vendor.getAssociationId().equals(adminAssociationId)) {
            throw new ResourceNotFoundException("Driver not found");
        }
        TaxiAssociation association = taxiAssociationRepository.findById(adminAssociationId)
                .orElseThrow(() -> new ResourceNotFoundException("Taxi association not found"));

        Wallet driverWallet = walletRepository.findWithLockByUserId(vendor.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver wallet not found"));
        if (driverWallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Driver's available balance is lower than the fine amount");
        }
        Wallet associationWallet = walletService.getOrCreateLockedAssociationWallet(adminAssociationId);

        driverWallet.setBalance(driverWallet.getBalance().subtract(amount));
        associationWallet.setBalance(associationWallet.getBalance().add(amount));
        walletRepository.save(driverWallet);
        walletRepository.save(associationWallet);

        Transaction transaction = Transaction.builder()
                .reference(generateReference())
                .senderId(vendor.getUserId())
                .receiverAssociationId(adminAssociationId)
                .amount(amount)
                .cashbackAmount(BigDecimal.ZERO)
                .cashbackRate(BigDecimal.ZERO)
                .status(TransactionStatus.COMPLETED)
                .description("Fine: " + reason)
                .build();
        transaction = transactionRepository.save(transaction);

        return new AssociationTransferResponse(
                transaction.getReference(), adminAssociationId, association.getName(),
                amount, BigDecimal.ZERO, driverWallet.getBalance(), transaction.getCreatedAt());
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
        BigDecimal fee = WalletService.PLATFORM_FEE;
        BigDecimal netAmount = amount.subtract(fee);
        Wallet platformWallet = walletService.getLockedPlatformFeeWallet();

        WalletService.creditWithAutoAllocation(vendorWallet, netAmount);
        platformWallet.setBalance(platformWallet.getBalance().add(fee));
        walletRepository.save(vendorWallet);
        walletRepository.save(platformWallet);

        Transaction transaction = Transaction.builder()
                .reference(generateReference())
                .receiverId(vendor.getUserId())
                .vendorId(vendor.getId())
                .amount(amount)
                .platformFee(fee)
                .cashbackAmount(BigDecimal.ZERO)
                .cashbackRate(BigDecimal.ZERO)
                .status(TransactionStatus.COMPLETED)
                .description(req.description())
                .build();
        transaction = transactionRepository.save(transaction);

        return new IncomingPaymentResponse(
                transaction.getReference(), vendor.getId(), vendor.getBusinessName(),
                amount, fee, vendorWallet.getBalance(), transaction.getCreatedAt());
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
