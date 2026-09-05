package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.UserResponse;
import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.model.User;
import co.za.ukhonapay.repository.TaxiAssociationRepository;
import co.za.ukhonapay.repository.TaxiRankRepository;
import co.za.ukhonapay.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TaxiAssociationRepository taxiAssociationRepository;
    private final TaxiRankRepository taxiRankRepository;

    public UserService(UserRepository userRepository,
                        TaxiAssociationRepository taxiAssociationRepository,
                        TaxiRankRepository taxiRankRepository) {
        this.userRepository = userRepository;
        this.taxiAssociationRepository = taxiAssociationRepository;
        this.taxiRankRepository = taxiRankRepository;
    }

    public UserResponse getById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String associationName = user.getAssociationId() != null
                ? taxiAssociationRepository.findById(user.getAssociationId()).map(a -> a.getName()).orElse(null)
                : null;
        String rankName = user.getRankId() != null
                ? taxiRankRepository.findById(user.getRankId()).map(r -> r.getName()).orElse(null)
                : null;

        return new UserResponse(
                user.getId(), user.getPhoneNumber(), user.getUserType().name(),
                user.getName(), user.getSurname(), user.getEmail(),
                associationName, rankName);
    }

    public Long getAssociationIdForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getAssociationId();
    }
}
