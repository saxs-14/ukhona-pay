package co.za.ukhonapay.repository;

import co.za.ukhonapay.model.BankWithdrawal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankWithdrawalRepository extends JpaRepository<BankWithdrawal, Long> {
    List<BankWithdrawal> findByUserIdOrderByCreatedAtDesc(Long userId);
}
