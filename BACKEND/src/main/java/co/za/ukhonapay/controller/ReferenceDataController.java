package co.za.ukhonapay.controller;

import co.za.ukhonapay.dto.TaxiAssociationResponse;
import co.za.ukhonapay.dto.TaxiRankResponse;
import co.za.ukhonapay.model.TaxiAssociation;
import co.za.ukhonapay.model.TaxiRank;
import co.za.ukhonapay.repository.TaxiAssociationRepository;
import co.za.ukhonapay.repository.TaxiRankRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Read-only lookup data for the signup dropdowns/search fields (taxi association,
// taxi rank). Deliberately unauthenticated - a new user needs these before they
// have an account, let alone a JWT.
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

    @GetMapping("/api/taxi-ranks")
    public ResponseEntity<List<TaxiRankResponse>> listRanks() {
        List<TaxiRank> ranks = taxiRankRepository.findAllByOrderByName();
        return ResponseEntity.ok(ranks.stream().map(r -> new TaxiRankResponse(r.getId(), r.getName(), r.getLocationName())).toList());
    }
}
