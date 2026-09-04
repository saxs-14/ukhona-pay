package co.za.ukhonapay.controller;

import co.za.ukhonapay.dto.CreateTaxiAssociationRequest;
import co.za.ukhonapay.dto.CreateTaxiRankRequest;
import co.za.ukhonapay.dto.TaxiAssociationResponse;
import co.za.ukhonapay.dto.TaxiRankResponse;
import co.za.ukhonapay.model.TaxiAssociation;
import co.za.ukhonapay.model.TaxiRank;
import co.za.ukhonapay.repository.TaxiAssociationRepository;
import co.za.ukhonapay.repository.TaxiRankRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    public ReferenceDataController(TaxiAssociationRepository taxiAssociationRepository,
                                    TaxiRankRepository taxiRankRepository) {
        this.taxiAssociationRepository = taxiAssociationRepository;
        this.taxiRankRepository = taxiRankRepository;
    }

    @GetMapping("/api/taxi-associations")
    public ResponseEntity<List<TaxiAssociationResponse>> listAssociations(@RequestParam(required = false) String search) {
        List<TaxiAssociation> associations = (search != null && !search.isBlank())
                ? taxiAssociationRepository.findByNameContainingIgnoreCaseOrderByName(search)
                : taxiAssociationRepository.findAllByOrderByName();
        return ResponseEntity.ok(associations.stream().map(a -> new TaxiAssociationResponse(a.getId(), a.getName())).toList());
    }

    // Idempotent by name (case-insensitive): returns the existing association if
    // one already matches, otherwise creates it. Lets an Association
    // Administrator register their (real) association during signup instead of
    // picking from a pre-loaded list.
    @PostMapping("/api/taxi-associations")
    public ResponseEntity<TaxiAssociationResponse> createAssociation(@Valid @RequestBody CreateTaxiAssociationRequest req) {
        TaxiAssociation association = taxiAssociationRepository.findByNameIgnoreCase(req.name().trim())
                .orElseGet(() -> {
                    TaxiAssociation a = new TaxiAssociation();
                    a.setName(req.name().trim());
                    return taxiAssociationRepository.save(a);
                });
        return ResponseEntity.ok(new TaxiAssociationResponse(association.getId(), association.getName()));
    }

    @GetMapping("/api/taxi-ranks")
    public ResponseEntity<List<TaxiRankResponse>> listRanks() {
        List<TaxiRank> ranks = taxiRankRepository.findAllByOrderByName();
        return ResponseEntity.ok(ranks.stream().map(r -> new TaxiRankResponse(r.getId(), r.getName(), r.getLocationName())).toList());
    }

    // Same idempotent-by-name pattern as associations, for a vendor or admin
    // registering a rank that doesn't exist on the platform yet.
    @PostMapping("/api/taxi-ranks")
    public ResponseEntity<TaxiRankResponse> createRank(@Valid @RequestBody CreateTaxiRankRequest req) {
        TaxiRank rank = taxiRankRepository.findByNameIgnoreCase(req.name().trim())
                .orElseGet(() -> {
                    TaxiRank r = new TaxiRank();
                    r.setName(req.name().trim());
                    r.setLocationName(req.locationName());
                    return taxiRankRepository.save(r);
                });
        return ResponseEntity.ok(new TaxiRankResponse(rank.getId(), rank.getName(), rank.getLocationName()));
    }
}
