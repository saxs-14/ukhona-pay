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
        return new WalletResponse(wallet.getUserId(), wallet.getBalance(), wallet.getCashbackBalance(), wallet.getCurrency());
    }
}
