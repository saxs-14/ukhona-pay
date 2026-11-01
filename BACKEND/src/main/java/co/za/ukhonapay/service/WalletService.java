package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.AssociationWalletResponse;
import co.za.ukhonapay.dto.WalletResponse;
import co.za.ukhonapay.exception.InsufficientFundsException;
import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.model.TaxiAssociation;
import co.za.ukhonapay.model.Wallet;
import co.za.ukhonapay.model.enums.WalletPocket;
import co.za.ukhonapay.repository.TaxiAssociationRepository;
import co.za.ukhonapay.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletService {

    /**
     * Flat fee charged on every user-initiated transaction.
     * The fee is accounted for in the dedicated PLATFORM_FEE_REVENUE_ZAR
     * ledger account; it is never credited to a human administrator wallet.
     */
    public static final BigDecimal PLATFORM_FEE = new BigDecimal("1.00");

    private final WalletRepository walletRepository;
    private final TaxiAssociationRepository taxiAssociationRepository;

    public WalletService(
            WalletRepository walletRepository,
            TaxiAssociationRepository taxiAssociationRepository) {
        this.walletRepository = walletRepository;
        this.taxiAssociationRepository = taxiAssociationRepository;
    }

    public WalletResponse getWallet(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user " + userId));
        return toResponse(wallet);
    }

    public AssociationWalletResponse getAssociationWallet(Long associationId) {
        TaxiAssociation association = taxiAssociationRepository.findById(associationId)
                .orElseThrow(() -> new ResourceNotFoundException("Taxi association not found"));
        BigDecimal balance = walletRepository.findByAssociationId(associationId)
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
        return new AssociationWalletResponse(association.getId(), association.getName(), balance, "ZAR");
    }

    /**
     * Lazily creates the association's wallet the first time money is sent to
     * it - taxi associations have no login of their own, so nothing else would
     * ever create this row.
     */
    @Transactional
    public Wallet getOrCreateLockedAssociationWallet(Long associationId) {
        return walletRepository.findWithLockByAssociationId(associationId)
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .associationId(associationId)
                        .balance(BigDecimal.ZERO)
                        .cashbackBalance(BigDecimal.ZERO)
                        .savingsBalance(BigDecimal.ZERO)
                        .maintenanceBalance(BigDecimal.ZERO)
                        .currency("ZAR")
                        .build()));
    }

    public static WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getCashbackBalance(),
                wallet.getSavingsBalance(),
                wallet.getMaintenanceBalance(),
                wallet.getCurrency());
    }

    public static final BigDecimal SAVINGS_RATE = new BigDecimal("0.05");
    public static final BigDecimal MAINTENANCE_RATE = new BigDecimal("0.05");

    public static void creditWithAutoAllocation(Wallet wallet, BigDecimal amount) {
        BigDecimal savingsShare = amount.multiply(SAVINGS_RATE)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal maintenanceShare = amount.multiply(MAINTENANCE_RATE)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal availableShare = amount.subtract(savingsShare).subtract(maintenanceShare);

        wallet.setBalance(wallet.getBalance().add(availableShare));
        wallet.setSavingsBalance(wallet.getSavingsBalance().add(savingsShare));
        wallet.setMaintenanceBalance(wallet.getMaintenanceBalance().add(maintenanceShare));
    }

    public static void reverseAutoAllocation(Wallet wallet, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        BigDecimal savings = amount.multiply(SAVINGS_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal maintenance = amount.multiply(MAINTENANCE_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal balance = amount.subtract(savings).subtract(maintenance);

        if (wallet.getBalance().compareTo(balance) < 0
                || wallet.getSavingsBalance().compareTo(savings) < 0
                || wallet.getMaintenanceBalance().compareTo(maintenance) < 0) {
            throw new InsufficientFundsException(
                    "Vendor wallet does not have enough allocated funds to reverse this provider refund");
        }

        wallet.setBalance(wallet.getBalance().subtract(balance));
        wallet.setSavingsBalance(wallet.getSavingsBalance().subtract(savings));
        wallet.setMaintenanceBalance(wallet.getMaintenanceBalance().subtract(maintenance));
    }

    @Transactional
    public WalletResponse transferBetweenOwnPockets(
            Long userId, WalletPocket from, WalletPocket to, BigDecimal amount) {
        if (from == to) {
            throw new IllegalArgumentException("Choose two different pockets to move money between");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        Wallet wallet = walletRepository.findWithLockByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user " + userId));

        BigDecimal available = pocketBalance(wallet, from);
        if (available.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Not enough in that pocket to move R" + amount);
        }

        setPocketBalance(wallet, from, available.subtract(amount));
        setPocketBalance(wallet, to, pocketBalance(wallet, to).add(amount));

        walletRepository.save(wallet);
        return toResponse(wallet);
    }

    private static BigDecimal pocketBalance(Wallet wallet, WalletPocket pocket) {
        return switch (pocket) {
            case BALANCE -> wallet.getBalance();
            case SAVINGS -> wallet.getSavingsBalance();
            case MAINTENANCE -> wallet.getMaintenanceBalance();
        };
    }

    private static void setPocketBalance(Wallet wallet, WalletPocket pocket, BigDecimal value) {
        switch (pocket) {
            case BALANCE -> wallet.setBalance(value);
            case SAVINGS -> wallet.setSavingsBalance(value);
            case MAINTENANCE -> wallet.setMaintenanceBalance(value);
        }
    }
}
