package co.za.ukhonapay.repository;

import co.za.ukhonapay.model.User;
import co.za.ukhonapay.model.enums.UserType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByIdNumber(String idNumber);
    long countByUserType(UserType userType);
    List<User> findAllByOrderByCreatedAtDesc();
}
