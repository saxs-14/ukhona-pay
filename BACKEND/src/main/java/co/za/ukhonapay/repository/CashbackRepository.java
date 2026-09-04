package co.za.ukhonapay.repository;

import co.za.ukhonapay.model.Cashback;
import co.za.ukhonapay.model.enums.CashbackStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CashbackRepository extends JpaRepository<Cashback, Long> {
    List<Cashback> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Cashback> findByUserIdAndStatus(Long userId, CashbackStatus status);

    @Modifying
    @Query("UPDATE Cashback c SET c.status = :status WHERE c.userId = :userId AND c.status = 'EARNED'")
    int markAllWithdrawn(@Param("userId") Long userId, @Param("status") CashbackStatus status);
}
