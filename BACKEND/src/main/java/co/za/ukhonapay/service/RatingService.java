package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.RatingRequest;
import co.za.ukhonapay.dto.RatingResponse;
import co.za.ukhonapay.exception.VendorNotFoundException;
import co.za.ukhonapay.model.Rating;
import co.za.ukhonapay.model.User;
import co.za.ukhonapay.model.Vendor;
import co.za.ukhonapay.repository.RatingRepository;
import co.za.ukhonapay.repository.UserRepository;
import co.za.ukhonapay.repository.VendorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class RatingService {

    private final RatingRepository ratingRepository;
    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;

    public RatingService(RatingRepository ratingRepository, VendorRepository vendorRepository, UserRepository userRepository) {
        this.ratingRepository = ratingRepository;
        this.vendorRepository = vendorRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void rate(Long reviewerId, RatingRequest req) {
        Vendor vendor = vendorRepository.findById(req.vendorId())
                .orElseThrow(() -> new VendorNotFoundException("Vendor not found"));

        if (req.transactionId() != null && ratingRepository.existsByVendorIdAndTransactionId(vendor.getId(), req.transactionId())) {
            throw new IllegalArgumentException("You've already rated this transaction");
        }

        Rating rating = Rating.builder()
                .vendorId(vendor.getId())
                .reviewerId(reviewerId)
                .transactionId(req.transactionId())
                .stars(req.stars())
                .review(req.review())
                .build();
        ratingRepository.save(rating);

        BigDecimal totalStars = vendor.getRatingAvg().multiply(BigDecimal.valueOf(vendor.getRatingCount()))
                .add(BigDecimal.valueOf(req.stars()));
        int newCount = vendor.getRatingCount() + 1;
        vendor.setRatingCount(newCount);
        vendor.setRatingAvg(totalStars.divide(BigDecimal.valueOf(newCount), 1, RoundingMode.HALF_UP));
        vendorRepository.save(vendor);
    }

    public List<RatingResponse> listForVendor(Long vendorId) {
        return ratingRepository.findByVendorIdOrderByCreatedAtDesc(vendorId).stream()
                .map(r -> new RatingResponse(
                        userRepository.findById(r.getReviewerId()).map(User::getName).orElse("Anonymous"),
                        r.getStars(),
                        r.getReview(),
                        r.getCreatedAt()))
                .toList();
    }
}
