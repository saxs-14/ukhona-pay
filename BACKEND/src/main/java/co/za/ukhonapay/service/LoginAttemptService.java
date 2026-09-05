package co.za.ukhonapay.service;

import co.za.ukhonapay.model.User;
import co.za.ukhonapay.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// Split out from AuthService because it must commit independently: on a
// wrong PIN, AuthService.login immediately throws, which rolls back its own
// @Transactional method - including any attempt-counter update made inside
// it. REQUIRES_NEW gives these their own transaction that commits
// regardless of what the caller does next, so the lockout actually holds.
@Service
public class LoginAttemptService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_MINUTES = 15;

    private final UserRepository userRepository;

    public LoginAttemptService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES));
            }
            userRepository.save(user);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void clearFailedAttempts(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            }
        });
    }
}
