package co.za.ukhonapay.repository;

import co.za.ukhonapay.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.senderId = :userId OR t.receiverId = :userId ORDER BY t.createdAt DESC")
    List<Transaction> findAllForUser(@Param("userId") Long userId);

    List<Transaction> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);

    List<Transaction> findByVendorIdOrderByCreatedAtDesc(Long vendorId);

    long countByVendorId(Long vendorId);
}
