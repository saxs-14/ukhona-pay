package co.za.ukhonapay.repository;

import co.za.ukhonapay.model.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {
    List<Withdrawal> findByUserIdOrderByRequestedAtDesc(Long userId);
}
