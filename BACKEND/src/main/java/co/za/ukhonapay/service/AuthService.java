package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.AuthResponse;
import co.za.ukhonapay.dto.LoginRequest;
import co.za.ukhonapay.dto.SignupRequest;
import co.za.ukhonapay.exception.InvalidCredentialsException;
import co.za.ukhonapay.model.User;
import co.za.ukhonapay.model.Vendor;
import co.za.ukhonapay.model.Wallet;
import co.za.ukhonapay.model.enums.UserType;
import co.za.ukhonapay.model.enums.VendorCategory;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository userRepository,
                        WalletRepository walletRepository,
                        VendorRepository vendorRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.vendorRepository = vendorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse signup(SignupRequest req) {
        if (userRepository.existsByPhoneNumber(req.phoneNumber())) {
            throw new IllegalArgumentException("Phone number already registered");
        }

        User user = User.builder()
                .phoneNumber(req.phoneNumber())
                .pinHash(passwordEncoder.encode(req.pin()))
                .userType(req.userType())
                .name(req.name())
                .email(req.email())
                // Mock ABSA SMS OTP: auto-verified for hackathon demo purposes.
                .phoneVerified(true)
                .build();
        user = userRepository.save(user);

        Wallet wallet = Wallet.builder()
                .userId(user.getId())
                .balance(req.userType() == UserType.VENDOR ? BigDecimal.ZERO : new BigDecimal("1000.00"))
                .cashbackBalance(BigDecimal.ZERO)
                .currency("ZAR")
                .build();
        walletRepository.save(wallet);

        if (req.userType() == UserType.VENDOR) {
            Vendor vendor = Vendor.builder()
                    .userId(user.getId())
                    .businessName(req.businessName() != null ? req.businessName() : req.name())
                    .category(req.category() != null ? VendorCategory.valueOf(req.category()) : VendorCategory.OTHER)
                    .locationName(req.locationName() != null ? req.locationName() : "South Africa")
                    .qrCode(generateQrCode(user.getId()))
                    .verified(false)
                    .ratingAvg(BigDecimal.ZERO)
                    .ratingCount(0)
                    .build();
            vendorRepository.save(vendor);
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

    private String generateQrCode(Long userId) {
        return "UKP-VENDOR-" + userId + "-" + (100000 + random.nextInt(900000));
    }
}
