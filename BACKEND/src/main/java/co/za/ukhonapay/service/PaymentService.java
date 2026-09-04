package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.PaymentRequest;
import co.za.ukhonapay.dto.PaymentResponse;
import co.za.ukhonapay.exception.InsufficientFundsException;
import co.za.ukhonapay.exception.InvalidCredentialsException;
import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.exception.VendorNotFoundException;
import co.za.ukhonapay.model.Cashback;
import co.za.ukhonapay.model.Transaction;
import co.za.ukhonapay.model.User;
import co.za.ukhonapay.model.Vendor;
import co.za.ukhonapay.model.Wallet;
import co.za.ukhonapay.model.enums.CashbackStatus;
import co.za.ukhonapay.model.enums.TransactionStatus;
import co.za.ukhonapay.model.enums.UserType;
import co.za.ukhonapay.repository.CashbackRepository;
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
    private final PasswordEncoder passwordEncoder;
    private final BigDecimal defaultCashbackRate;
    private final SecureRandom random = new SecureRandom();

    public PaymentService(UserRepository userRepository,
                           VendorRepository vendorRepository,
                           WalletRepository walletRepository,
                           TransactionRepository transactionRepository,
                           CashbackRepository cashbackRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${ukhonapay.cashback.default-rate}") BigDecimal defaultCashbackRate) {
        this.userRepository = userRepository;
        this.vendorRepository = vendorRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.cashbackRepository = cashbackRepository;
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

        BigDecimal cashbackRate = sender.getUserType() == UserType.VENDOR ? BigDecimal.ZERO : defaultCashbackRate;
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

    private String generateReference() {
        return "TXN-" + System.currentTimeMillis() % 1_000_000_000L + random.nextInt(1000);
    }
}
