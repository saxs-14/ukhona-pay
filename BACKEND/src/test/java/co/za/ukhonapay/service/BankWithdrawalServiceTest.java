package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.BankWithdrawalRequest;
import co.za.ukhonapay.repository.BankAccountRepository;
import co.za.ukhonapay.repository.BankWithdrawalRepository;
import co.za.ukhonapay.repository.UserRepository;
import co.za.ukhonapay.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class BankWithdrawalServiceTest {

    @Mock BankWithdrawalRepository bankWithdrawalRepository;
    @Mock BankAccountRepository bankAccountRepository;
    @Mock WalletRepository walletRepository;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    @Test
    void liveModeRejectsWithdrawalBeforeAnyWalletMutation() {
        BankWithdrawalService service = new BankWithdrawalService(
                bankWithdrawalRepository,
                bankAccountRepository,
                walletRepository,
                userRepository,
                passwordEncoder,
                "live"
        );

        assertThrows(IllegalStateException.class, () ->
                service.withdraw(1L, new BankWithdrawalRequest(new BigDecimal("100.00"), "1234"))
        );
    }
}
