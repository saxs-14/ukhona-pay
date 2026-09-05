package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.ServicePurchaseRequest;
import co.za.ukhonapay.dto.ServicePurchaseResponse;
import co.za.ukhonapay.exception.InsufficientFundsException;
import co.za.ukhonapay.exception.InvalidCredentialsException;
import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.model.ServicePurchase;
import co.za.ukhonapay.model.User;
import co.za.ukhonapay.model.Wallet;
import co.za.ukhonapay.model.enums.ServicePurchaseType;
import co.za.ukhonapay.repository.ServicePurchaseRepository;
import co.za.ukhonapay.repository.UserRepository;
import co.za.ukhonapay.repository.WalletRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;

// Real, server-recorded prepaid voucher / bill payment purchases (airtime,
// electricity, Pay@ bills). No real telecom/utility API exists to call from
// a hackathon project, so the voucher token is simulated - but it's
// generated and persisted here, server-side, exactly once per purchase,
// rather than fabricated fresh in the browser on every render (which is
// what this replaced - see git history). The wallet debit and PIN check are
// entirely real, and the money leaves the wallet the same way a bank
// withdrawal does: it doesn't land in any other UKHONA PAY wallet, because
// it's paying a real-world third party outside this platform.
@Service
public class ServicePurchaseService {

    private final ServicePurchaseRepository servicePurchaseRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public ServicePurchaseService(ServicePurchaseRepository servicePurchaseRepository,
                                   WalletRepository walletRepository,
                                   UserRepository userRepository,
                                   PasswordEncoder passwordEncoder) {
        this.servicePurchaseRepository = servicePurchaseRepository;
        this.walletRepository = walletRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ServicePurchaseResponse purchase(Long userId, ServicePurchaseRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(req.pin(), user.getPinHash())) {
            throw new InvalidCredentialsException("Incorrect PIN");
        }

        ServicePurchaseType type = ServicePurchaseType.valueOf(req.type());
        BigDecimal amount = req.amount();

        Wallet wallet = walletRepository.findWithLockByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient wallet balance for this purchase");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);

        String reference = generateReference();
        String voucherToken = switch (type) {
            case AIRTIME -> generateAirtimePin();
            case ELECTRICITY -> generateStsToken();
            case PAYAT_BILL -> generatePayAtCode(req.payAtReference());
        };

        ServicePurchase.Builder builder = ServicePurchase.builder()
                .userId(userId)
                .type(type)
                .reference(reference)
                .amount(amount)
                .voucherToken(voucherToken);

        String title, subtitle, tokenLabel, dialInstruction, extraLabel, extraValue;

        if (type == ServicePurchaseType.AIRTIME) {
            String network = req.network() == null ? "" : req.network().toUpperCase();
            builder.network(network).recipientPhone(req.recipientPhone());
            title = network + " Airtime Recharge";
            subtitle = "Top-up sent to " + req.recipientPhone();
            tokenLabel = "Recharge Voucher PIN";
            dialInstruction = "Dial *130*7467*" + voucherToken.replace(" ", "") + "# or load directly on your SIM";
            extraLabel = "Network Provider";
            extraValue = network;
        } else if (type == ServicePurchaseType.ELECTRICITY) {
            String meter = req.meterNumber() == null ? "" : req.meterNumber().replaceAll("\\s+", "");
            builder.meterNumber(meter).municipality(req.municipality());
            BigDecimal estimatedKwh = amount.divide(new BigDecimal("2.85"), 1, java.math.RoundingMode.HALF_UP);
            title = "Prepaid Electricity Token";
            subtitle = "Meter: " + meter + " • " + req.municipality();
            tokenLabel = "20-Digit STS Keypad Token";
            dialInstruction = "Enter these 20 digits into your in-home CIU keypad, then press # or Enter";
            extraLabel = "Units Purchased";
            extraValue = "~" + estimatedKwh + " kWh";
        } else {
            String ref = req.payAtReference() == null ? "" : req.payAtReference().replaceAll("\\s+", "");
            builder.billerName(req.billerName()).billerCategory(req.billerCategory())
                    .payAtReference(ref).accountName(req.accountName());
            title = "Pay@ Bill Payment Clearance";
            subtitle = req.billerName() + " • " + req.billerCategory();
            tokenLabel = "Pay@ Clearance Code";
            dialInstruction = "Retain this clearance code as official proof of municipal/retail settlement";
            extraLabel = "Biller Account / Reference";
            extraValue = ref;
        }

        ServicePurchase saved = servicePurchaseRepository.save(builder.build());

        return new ServicePurchaseResponse(
                reference, type.name(), amount, voucherToken, tokenLabel, dialInstruction,
                extraLabel, extraValue, title, subtitle,
                type == ServicePurchaseType.AIRTIME ? req.network() : null,
                wallet.getBalance(), saved.getCreatedAt());
    }

    public List<ServicePurchase> historyForUser(Long userId) {
        return servicePurchaseRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private String generateReference() {
        return "TXN-" + System.currentTimeMillis() % 1_000_000_000L + random.nextInt(1000);
    }

    private String generateStsToken() {
        return String.format("%04d %04d %04d %04d %04d",
                random.nextInt(10000), random.nextInt(10000), random.nextInt(10000),
                random.nextInt(10000), random.nextInt(10000));
    }

    private String generateAirtimePin() {
        return String.format("%04d %04d %04d", random.nextInt(10000), random.nextInt(10000), random.nextInt(10000));
    }

    private String generatePayAtCode(String payAtRef) {
        String tail = (payAtRef == null || payAtRef.length() < 6)
                ? String.valueOf(100000 + random.nextInt(900000))
                : payAtRef.replaceAll("\\s+", "").substring(payAtRef.replaceAll("\\s+", "").length() - 6);
        return "PA-" + tail + "-" + (1000 + random.nextInt(9000));
    }
}
