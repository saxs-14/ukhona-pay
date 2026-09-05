package co.za.ukhonapay.controller;

import co.za.ukhonapay.dto.CreateTaxiAssociationRequest;
import co.za.ukhonapay.dto.CreateTaxiRankRequest;
import co.za.ukhonapay.dto.TaxiAssociationResponse;
import co.za.ukhonapay.dto.TaxiRankResponse;
import co.za.ukhonapay.model.TaxiAssociation;
import co.za.ukhonapay.model.TaxiRank;
import co.za.ukhonapay.model.Wallet;
import co.za.ukhonapay.repository.TaxiAssociationRepository;
import co.za.ukhonapay.repository.TaxiRankRepository;
import co.za.ukhonapay.repository.WalletRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

// Lookup + create endpoints for the signup dropdowns (taxi association, taxi
// rank). Deliberately unauthenticated - a new user needs these before they have
// an account, let alone a JWT. There is no pre-loaded reference data: the first
// Association Administrator to register creates their association, the first
// vendor/admin to need a rank creates it, and everyone after picks the existing
// entry rather than creating a duplicate.
@RestController
public class ReferenceDataController {

    private final TaxiAssociationRepository taxiAssociationRepository;
    private final TaxiRankRepository taxiRankRepository;
    private final WalletRepository walletRepository;

    public ReferenceDataController(TaxiAssociationRepository taxiAssociationRepository,
                                    TaxiRankRepository taxiRankRepository,
                                    WalletRepository walletRepository) {
        this.taxiAssociationRepository = taxiAssociationRepository;
        this.taxiRankRepository = taxiRankRepository;
        this.walletRepository = walletRepository;
    }

    @GetMapping("/api/taxi-associations")
    public ResponseEntity<List<TaxiAssociationResponse>> listAssociations(@RequestParam(required = false) String search) {
        List<TaxiAssociation> associations = (search != null && !search.isBlank())
                ? taxiAssociationRepository.findByNameContainingIgnoreCaseOrderByName(search)
                : taxiAssociationRepository.findAllByOrderByName();
        return ResponseEntity.ok(associations.stream().map(a -> new TaxiAssociationResponse(a.getId(), a.getName())).toList());
    }

    // Idempotent by name (case-insensitive): returns the existing association if
    // one already matches, otherwise creates it - and, only on that first
    // creation, its wallet too, so a transfer can never race the wallet into
    // existence (see WalletService.getOrCreateLockedAssociationWallet, which
    // stays as a safety net for any row that predates this). insertIfAbsent
    // uses INSERT ... ON CONFLICT DO NOTHING, so two concurrent requests for
    // the same new name never throw - one wins the insert, the other just
    // reads the row the winner created.
    @Transactional
    @PostMapping("/api/taxi-associations")
    public ResponseEntity<TaxiAssociationResponse> createAssociation(@Valid @RequestBody CreateTaxiAssociationRequest req) {
        String name = req.name().trim();
        boolean createdNow = taxiAssociationRepository.insertIfAbsent(name) == 1;
        TaxiAssociation association = taxiAssociationRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new IllegalStateException("Association insert raced but the row is missing"));

        if (createdNow) {
            walletRepository.save(Wallet.builder()
                    .associationId(association.getId())
                    .balance(BigDecimal.ZERO)
                    .cashbackBalance(BigDecimal.ZERO)
                    .currency("ZAR")
                    .build());
        }
        return ResponseEntity.ok(new TaxiAssociationResponse(association.getId(), association.getName()));
    }

    @GetMapping("/api/taxi-ranks")
    public ResponseEntity<List<TaxiRankResponse>> listRanks() {
        List<TaxiRank> ranks = taxiRankRepository.findAllByOrderByName();
        return ResponseEntity.ok(ranks.stream().map(r -> new TaxiRankResponse(r.getId(), r.getName(), r.getLocationName())).toList());
    }

    // Same idempotent-by-name + race-safe pattern as associations, for a vendor
    // or admin registering a rank that doesn't exist on the platform yet.
    @Transactional
    @PostMapping("/api/taxi-ranks")
    public ResponseEntity<TaxiRankResponse> createRank(@Valid @RequestBody CreateTaxiRankRequest req) {
        String name = req.name().trim();
        taxiRankRepository.insertIfAbsent(name, req.locationName());
        TaxiRank rank = taxiRankRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new IllegalStateException("Rank insert raced but the row is missing"));
        return ResponseEntity.ok(new TaxiRankResponse(rank.getId(), rank.getName(), rank.getLocationName()));
    }
}
