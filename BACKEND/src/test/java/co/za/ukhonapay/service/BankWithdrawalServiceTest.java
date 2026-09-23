package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.BankWithdrawalRequest;
import co.za.ukhonapay.model.User;
import co.za.ukhonapay.repository.BankAccountRepository;
import co.za.ukhonapay.repository.BankWithdrawalRepository;
import co.za.ukhonapay.repository.UserRepository;
import co.za.ukhonapay.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankWithdrawalServiceTest {

    @Mock BankWithdrawalRepository bankWithdrawalRepository;
    @Mock BankAccountRepository bankAccountRepository;
    @Mock WalletRepository walletRepository;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock BankWithdrawalReservationService reservationService;
    @Mock BankWithdrawalSettlementService settlementService;
    @Mock co.za.ukhonapay.payment.PayoutProvider payoutProvider;
    @Mock PayoutSecretCryptoService secretCrypto;

    @Test
    void liveModeRejectsWithdrawalBeforeAnyWalletMutation() {
        BankWithdrawalService service = new BankWithdrawalService(
                bankWithdrawalRepository,
                bankAccountRepository,
                reservationService,
                settlementService,
                userRepository,
                walletRepository,
                passwordEncoder,
                payoutProvider,
                secretCrypto,
                "live"
        );

        User user = new User();
        user.setPinHash("encoded-pin");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("1234", "encoded-pin")).thenReturn(true);
        when(payoutProvider.isConfigured()).thenReturn(false);

        assertThrows(IllegalStateException.class, () ->
                service.withdraw(1L, new BankWithdrawalRequest(new BigDecimal("100.00"), "1234"))
        );
    }
}
