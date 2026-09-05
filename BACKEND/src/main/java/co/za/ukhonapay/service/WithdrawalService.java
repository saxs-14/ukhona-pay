package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.AtmLocationResponse;
import co.za.ukhonapay.dto.WithdrawalRequest;
import co.za.ukhonapay.dto.WithdrawalResponse;
import co.za.ukhonapay.exception.InsufficientFundsException;
import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.model.AtmLocation;
import co.za.ukhonapay.model.Wallet;
import co.za.ukhonapay.model.Withdrawal;
import co.za.ukhonapay.model.enums.WithdrawalStatus;
import co.za.ukhonapay.repository.AtmLocationRepository;
import co.za.ukhonapay.repository.CashbackRepository;
import co.za.ukhonapay.repository.WalletRepository;
import co.za.ukhonapay.dto.VendorBankWithdrawalRequest;
import co.za.ukhonapay.dto.VendorBankWithdrawalResponse;
import co.za.ukhonapay.exception.InvalidCredentialsException;
import co.za.ukhonapay.model.Transaction;
import co.za.ukhonapay.model.User;
import co.za.ukhonapay.model.enums.TransactionStatus;
import co.za.ukhonapay.repository.TransactionRepository;
import co.za.ukhonapay.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class WithdrawalService {

    private final WithdrawalRepository withdrawalRepository;
    private final WalletRepository walletRepository;
    private final AtmLocationRepository atmLocationRepository;
    private final CashbackRepository cashbackRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final long pinValidHours;
    private final SecureRandom random = new SecureRandom();

    public WithdrawalService(WithdrawalRepository withdrawalRepository,
                              WalletRepository walletRepository,
                              AtmLocationRepository atmLocationRepository,
                              CashbackRepository cashbackRepository,
                              UserRepository userRepository,
                              TransactionRepository transactionRepository,
                              PasswordEncoder passwordEncoder,
                              @Value("${ukhonapay.withdrawal.pin-valid-hours}") long pinValidHours) {
        this.withdrawalRepository = withdrawalRepository;
        this.walletRepository = walletRepository;
        this.atmLocationRepository = atmLocationRepository;
        this.cashbackRepository = cashbackRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
        this.pinValidHours = pinValidHours;
    }

    public List<AtmLocationResponse> nearbyAtms() {
        List<AtmLocation> atms = atmLocationRepository.findAll();
        return atms.stream()
                .map(a -> new AtmLocationResponse(a.getId(), a.getName(), a.getAddress(), a.getCity(),
                        a.getLatitude(), a.getLongitude(), a.getBank(), null))
                .toList();
    }

    @Transactional
    public WithdrawalResponse requestWithdrawal(Long userId, WithdrawalRequest req) {
        Wallet wallet = walletRepository.findWithLockByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        if (wallet.getCashbackBalance().compareTo(req.amount()) < 0) {
            throw new InsufficientFundsException("Withdrawal amount exceeds available cashback balance");
        }

        AtmLocation atm = atmLocationRepository.findById(req.atmLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("ATM location not found"));

        wallet.setCashbackBalance(wallet.getCashbackBalance().subtract(req.amount()));
        walletRepository.save(wallet);

        Withdrawal withdrawal = Withdrawal.builder()
                .userId(userId)
                .atmLocationId(atm.getId())
                .amount(req.amount())
                .withdrawalPin(generatePin())
                .status(WithdrawalStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusHours(pinValidHours))
                .build();
        withdrawal = withdrawalRepository.save(withdrawal);

        markCashbackWithdrawnUpTo(userId, req.amount());

        return toResponse(withdrawal, atm.getName());
    }

    @Transactional
    public WithdrawalResponse completeWithdrawal(Long userId, Long withdrawalId) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("Withdrawal not found"));

        if (!withdrawal.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Withdrawal not found");
        }
        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new IllegalArgumentException("Withdrawal is not pending");
        }
        if (LocalDateTime.now().isAfter(withdrawal.getExpiresAt())) {
            withdrawal.setStatus(WithdrawalStatus.EXPIRED);
            withdrawalRepository.save(withdrawal);
            throw new IllegalArgumentException("Withdrawal PIN has expired, request a new withdrawal");
        }

        withdrawal.setStatus(WithdrawalStatus.COMPLETED);
        withdrawal.setCompletedAt(LocalDateTime.now());
        withdrawal = withdrawalRepository.save(withdrawal);

        AtmLocation atm = atmLocationRepository.findById(withdrawal.getAtmLocationId()).orElse(null);
        return toResponse(withdrawal, atm != null ? atm.getName() : "ATM");
    }

    public List<WithdrawalResponse> historyForUser(Long userId) {
        return withdrawalRepository.findByUserIdOrderByRequestedAtDesc(userId).stream()
                .map(w -> {
                    AtmLocation atm = atmLocationRepository.findById(w.getAtmLocationId()).orElse(null);
                    return toResponse(w, atm != null ? atm.getName() : "ATM");
                })
                .toList();
    }

    private void markCashbackWithdrawnUpTo(Long userId, BigDecimal amount) {
        // Simplified for the hackathon MVP: mark all EARNED cashback as WITHDRAWN once
        // the corresponding wallet balance has been debited above (avoids partial-row splitting).
        cashbackRepository.markAllWithdrawn(userId, CashbackStatus.WITHDRAWN);
    }

    private String generatePin() {
        return String.format("%04d", random.nextInt(10000));
    }

    @Transactional
    public VendorBankWithdrawalResponse withdrawToBank(Long userId, VendorBankWithdrawalRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(req.pin(), user.getPinHash())) {
            throw new InvalidCredentialsException("Incorrect PIN");
        }

        Wallet wallet = walletRepository.findWithLockByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));

        if (wallet.getBalance().compareTo(req.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient wallet balance for this bank withdrawal");
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

    private WithdrawalResponse toResponse(Withdrawal w, String atmName) {
        return new WithdrawalResponse(w.getId(), w.getAmount(), w.getWithdrawalPin(), atmName,
                w.getStatus().name(), w.getRequestedAt(), w.getExpiresAt(), w.getCompletedAt());
    }
}
