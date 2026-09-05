package co.za.ukhonapay.controller;

import co.za.ukhonapay.dto.RatingRequest;
import co.za.ukhonapay.dto.RatingResponse;
import co.za.ukhonapay.security.CurrentUser;
import co.za.ukhonapay.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping
    public ResponseEntity<Void> rate(@Valid @RequestBody RatingRequest request) {
        ratingService.rate(CurrentUser.id(), request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/vendor/{vendorId}")
    public ResponseEntity<List<RatingResponse>> forVendor(@PathVariable Long vendorId) {
        return ResponseEntity.ok(ratingService.listForVendor(vendorId));
    }
}
