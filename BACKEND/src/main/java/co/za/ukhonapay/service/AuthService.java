package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.AuthResponse;
import co.za.ukhonapay.dto.LoginRequest;
import co.za.ukhonapay.dto.SignupRequest;
import co.za.ukhonapay.exception.InvalidCredentialsException;
import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.model.TaxiAssociation;
import co.za.ukhonapay.model.TaxiRank;
import co.za.ukhonapay.model.User;
import co.za.ukhonapay.model.Vendor;
import co.za.ukhonapay.model.Wallet;
import co.za.ukhonapay.model.enums.UserType;
import co.za.ukhonapay.model.enums.VendorCategory;
import co.za.ukhonapay.repository.TaxiAssociationRepository;
import co.za.ukhonapay.repository.TaxiRankRepository;
import co.za.ukhonapay.repository.UserRepository;
import co.za.ukhonapay.repository.VendorRepository;
import co.za.ukhonapay.repository.WalletRepository;
import co.za.ukhonapay.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final VendorRepository vendorRepository;
    private final TaxiAssociationRepository taxiAssociationRepository;
    private final TaxiRankRepository taxiRankRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository userRepository,
                        WalletRepository walletRepository,
                        VendorRepository vendorRepository,
                        TaxiAssociationRepository taxiAssociationRepository,
                        TaxiRankRepository taxiRankRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.vendorRepository = vendorRepository;
        this.taxiAssociationRepository = taxiAssociationRepository;
        this.taxiRankRepository = taxiRankRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse signup(SignupRequest req) {
        if (userRepository.existsByPhoneNumber(req.phoneNumber())) {
            throw new IllegalArgumentException("Phone number already registered");
        }
        if (userRepository.existsByIdNumber(req.idNumber())) {
            throw new IllegalArgumentException("ID number already registered");
        }

        Long associationId = null;
        Long rankId = null;

        switch (req.userType()) {
            case TAXI_DRIVER -> associationId = requireAssociation(req.associationId());
            case VENDOR -> rankId = requireRank(req.rankId());
            case TAXI_ASSOCIATION_ADMIN -> {
                associationId = requireAssociation(req.associationId());
                rankId = requireRank(req.rankId());
            }
        }

        User.Builder userBuilder = User.builder()
                .phoneNumber(req.phoneNumber())
                .pinHash(passwordEncoder.encode(req.pin()))
                .userType(req.userType())
                .name(req.name())
                .surname(req.surname())
                .idNumber(req.idNumber())
                .email(req.email())
                // Mock ABSA SMS OTP: auto-verified for hackathon demo purposes.
                .phoneVerified(true);

        if (req.userType() == UserType.TAXI_ASSOCIATION_ADMIN) {
            userBuilder.associationId(associationId).rankId(rankId);
        }

        User user = userRepository.save(userBuilder.build());

        Wallet wallet = Wallet.builder()
                .userId(user.getId())
                .balance(BigDecimal.ZERO)
                .cashbackBalance(BigDecimal.ZERO)
                .currency("ZAR")
                .build();
        walletRepository.save(wallet);

        if (req.userType() == UserType.TAXI_DRIVER || req.userType() == UserType.VENDOR) {
            boolean isDriver = req.userType() == UserType.TAXI_DRIVER;
            Vendor.Builder vendorBuilder = Vendor.builder()
                    .userId(user.getId())
                    .businessName(req.name() + " " + req.surname())
                    .category(isDriver ? VendorCategory.TAXI : VendorCategory.OTHER)
                    .qrCode(generateQrCode(user.getId()))
                    .verified(false)
                    .ratingAvg(BigDecimal.ZERO)
                    .ratingCount(0);

            if (isDriver) {
                TaxiAssociation association = taxiAssociationRepository.findById(associationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Taxi association not found"));
                vendorBuilder.locationName(association.getName())
                        .vehicleRegistration(req.vehicleRegistration())
                        .associationId(associationId);
            } else {
                TaxiRank rank = taxiRankRepository.findById(rankId)
                        .orElseThrow(() -> new ResourceNotFoundException("Taxi rank not found"));
                vendorBuilder.locationName(rank.getName())
                        .rankId(rankId);
            }

            vendorRepository.save(vendorBuilder.build());
        }

        String token = jwtService.generateToken(user.getId(), user.getUserType().name(), user.getPhoneNumber());
        return new AuthResponse(token, user.getId(), user.getName(), user.getUserType().name(), user.getPhoneNumber());
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByPhoneNumber(req.phoneNumber())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid phone number or PIN"));

        if (!passwordEncoder.matches(req.pin(), user.getPinHash())) {
            throw new InvalidCredentialsException("Invalid phone number or PIN");
        }

        String token = jwtService.generateToken(user.getId(), user.getUserType().name(), user.getPhoneNumber());
        return new AuthResponse(token, user.getId(), user.getName(), user.getUserType().name(), user.getPhoneNumber());
    }

    private Long requireAssociation(Long associationId) {
        if (associationId == null) {
            throw new IllegalArgumentException("Taxi association is required");
        }
        return associationId;
    }

    private Long requireRank(Long rankId) {
        if (rankId == null) {
            throw new IllegalArgumentException("Taxi rank is required");
        }
        return rankId;
    }

    private String generateQrCode(Long userId) {
        return "UKP-VENDOR-" + userId + "-" + (100000 + random.nextInt(900000));
    }
}
