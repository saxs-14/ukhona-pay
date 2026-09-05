package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.AssociationWalletResponse;
import co.za.ukhonapay.dto.WalletResponse;
import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.model.TaxiAssociation;
import co.za.ukhonapay.model.Wallet;
import co.za.ukhonapay.repository.TaxiAssociationRepository;
import co.za.ukhonapay.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final TaxiAssociationRepository taxiAssociationRepository;

    public WalletService(WalletRepository walletRepository, TaxiAssociationRepository taxiAssociationRepository) {
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
                        .currency("ZAR")
                        .build()));
    }

    public static WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(wallet.getUserId(), wallet.getBalance(), wallet.getCashbackBalance(),
                wallet.getSavingsBalance(), wallet.getMaintenanceBalance(), wallet.getCurrency());
    }

    // Splits an incoming fare payment into the available balance (90%) and
    // two earmarked pots (5% savings, 5% maintenance) - the two percentage
    // pots are rounded first and the available share takes the remainder, so
    // the three always sum exactly to the original amount regardless of
    // rounding. Mutates the wallet in place; caller still saves it.
    public static final BigDecimal SAVINGS_RATE = new BigDecimal("0.05");
    public static final BigDecimal MAINTENANCE_RATE = new BigDecimal("0.05");

    public static void creditWithAutoAllocation(Wallet wallet, BigDecimal amount) {
        BigDecimal savingsShare = amount.multiply(SAVINGS_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal maintenanceShare = amount.multiply(MAINTENANCE_RATE).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal availableShare = amount.subtract(savingsShare).subtract(maintenanceShare);

        wallet.setBalance(wallet.getBalance().add(availableShare));
        wallet.setSavingsBalance(wallet.getSavingsBalance().add(savingsShare));
        wallet.setMaintenanceBalance(wallet.getMaintenanceBalance().add(maintenanceShare));
    }
}
