package co.za.ukhonapay.repository;

import co.za.ukhonapay.model.User;
<<<<<<< HEAD
import org.springframework.data.jpa.repository.JpaRepository;

=======
import co.za.ukhonapay.model.enums.UserType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
>>>>>>> origin/main
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByIdNumber(String idNumber);
<<<<<<< HEAD
=======
    long countByUserType(UserType userType);
    List<User> findAllByOrderByCreatedAtDesc();
>>>>>>> origin/main
}
