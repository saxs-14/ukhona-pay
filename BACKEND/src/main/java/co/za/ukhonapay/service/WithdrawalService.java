package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.VendorBankWithdrawalRequest;
import co.za.ukhonapay.dto.VendorBankWithdrawalResponse;
import co.za.ukhonapay.exception.InsufficientFundsException;
import co.za.ukhonapay.exception.InvalidCredentialsException;
import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.model.Transaction;
import co.za.ukhonapay.model.User;
import co.za.ukhonapay.model.Wallet;
import co.za.ukhonapay.model.enums.TransactionStatus;
import co.za.ukhonapay.repository.TransactionRepository;
import co.za.ukhonapay.repository.UserRepository;
import co.za.ukhonapay.repository.WalletRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class WithdrawalService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    public WithdrawalService(WalletRepository walletRepository,
                             UserRepository userRepository,
                             TransactionRepository transactionRepository,
                             PasswordEncoder passwordEncoder) {
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public VendorBankWithdrawalResponse withdrawToBank(Long userId, VendorBankWithdrawalRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(req.pin(), user.getPinHash())) {
            throw new InvalidCredentialsException("Incorrect account security PIN");
        }

        Wallet wallet = walletRepository.findWithLockByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        if (wallet.getBalance().compareTo(req.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient wallet balance (Available: R" + wallet.getBalance() + ")");
        }
        
        wallet.setBalance(wallet.getBalance().subtract(req.amount()));
        walletRepository.save(wallet);

        String ref = "PAYOUT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String maskedAccount = req.accountNumber().length() > 4 
                ? "****" + req.accountNumber().substring(req.accountNumber().length() - 4)
                : req.accountNumber();

        Transaction transaction = Transaction.builder()
                .reference(ref)
                .senderId(userId)
                .receiverId(userId)
                .amount(req.amount())
                .cashbackAmount(BigDecimal.ZERO)
                .cashbackRate(BigDecimal.ZERO)
                .status(TransactionStatus.COMPLETED)
                .description("Bank Cashout to " + req.bankName() + " (" + maskedAccount + ")")
                .build();
        transactionRepository.save(transaction);

        return new VendorBankWithdrawalResponse(
                ref, req.amount(), req.bankName(), maskedAccount, req.accountHolderName(), "COMPLETED", LocalDateTime.now());
    }

    @Transactional
    public co.za.ukhonapay.dto.CashSendResponse cashSend(Long userId, co.za.ukhonapay.dto.CashSendRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(req.accountPin(), user.getPinHash())) {
            throw new InvalidCredentialsException("Incorrect account security PIN");
        }

        Wallet wallet = walletRepository.findWithLockByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        if (wallet.getBalance().compareTo(req.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient wallet balance for CashSend voucher (Available: R" + String.format("%.2f", wallet.getBalance()) + ")");
        }

        wallet.setBalance(wallet.getBalance().subtract(req.amount()));
        walletRepository.save(wallet);

        String ref = "CS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long randomVoucherNum = 9000000000L + (long) (Math.random() * 1000000000L);
        String voucherNumber = String.valueOf(randomVoucherNum);

        String phone = req.recipientPhone().replaceAll("\\s+", "");
        String maskedPhone = phone.length() >= 10
                ? phone.substring(0, 3) + "***" + phone.substring(phone.length() - 4)
                : phone;

        Transaction transaction = Transaction.builder()
                .reference(ref)
                .senderId(userId)
                .receiverId(userId)
                .amount(req.amount())
                .cashbackAmount(BigDecimal.ZERO)
                .cashbackRate(BigDecimal.ZERO)
                .status(TransactionStatus.COMPLETED)
                .description("CashSend Voucher to " + maskedPhone + " (Voucher: " + voucherNumber + ")")
                .build();
        transactionRepository.save(transaction);

        java.util.List<String> validOutlets = java.util.List.of(
                "ABSA Bank ATMs",
                "Shoprite Money Market",
                "Checkers Money Market",
                "Boxer Superstores",
                "Usave",
                "Pick n Pay Store Counters",
                "PEP & PEP Cell Stores",
                "Spar Money Counters"
        );

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(30);

        return new co.za.ukhonapay.dto.CashSendResponse(
                ref,
                voucherNumber,
                req.cashSendPin(),
                req.amount(),
                phone,
                maskedPhone,
                "COMPLETED",
                validOutlets,
                now,
                expiresAt
        );
    }
}
